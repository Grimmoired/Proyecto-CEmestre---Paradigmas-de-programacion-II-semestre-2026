package cemestre.view;

import cemestre.model.Combination;
import cemestre.observer.StateEvent;
import cemestre.observer.StateObserver;
import cemestre.sorting.SortCriterion;
import cemestre.state.AppState;
import cemestre.state.CombinationFilter;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

public final class CombinationListPanel extends JPanel implements StateObserver {
    private final AppState state;
    private final CombinationTableModel tableModel;
    private final JTable table;

    private boolean syncing = false;

    public CombinationListPanel(AppState state, List<SortCriterion> criteria) {
        super(new BorderLayout());
        this.state = Objects.requireNonNull(state, "state");
        this.tableModel = new CombinationTableModel(state);
        this.table = new JTable(tableModel);

        add(createControls(criteria), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        configureTable();

        state.addObserver(this);
    }

    private JPanel createControls(List<SortCriterion> criteria) {
        JComboBox<SortCriterion> criterionBox = new JComboBox<>(criteria.toArray(new SortCriterion[0]));
        criterionBox.setSelectedItem(state.getCriterion());
        criterionBox.addActionListener(e -> state.setCriterion((SortCriterion) criterionBox.getSelectedItem()));

        JComboBox<CombinationFilter> filterBox = new JComboBox<>(CombinationFilter.values());
        filterBox.setSelectedItem(state.getFilter());
        filterBox.addActionListener(e -> state.setFilter((CombinationFilter) filterBox.getSelectedItem()));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        controls.add(new JLabel("Ordenar por:"));
        controls.add(criterionBox);
        controls.add(new JLabel("Mostrar:"));
        controls.add(filterBox);
        return controls;
    }

    private void configureTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {40, 75, 60, 45, 85, 85, 115, 420};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);   // columnas con su ancho; barra horizontal si no caben

        table.getSelectionModel().addListSelectionListener(e -> {
            if (syncing || e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            state.select(row < 0 ? null : tableModel.combinationAt(row));
        });
    }

    @Override
    public void onStateChanged(StateEvent event) {
        syncing = true;
        try {
            if (event != StateEvent.SELECTION_CHANGED) {
                tableModel.fireTableDataChanged();   // la lista cambio: la tabla vuelve a leer todas las filas
            }
            showSelection();
        } finally {
            syncing = false;
        }
    }

    private void showSelection() {
        int row = state.getSelected().map(tableModel::rowOf).orElse(-1);
        if (row < 0) {
            table.clearSelection();
            return;
        }
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }
}