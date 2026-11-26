package cemestre.view;

import cemestre.model.Combination;
import cemestre.model.ScheduleMetrics;
import cemestre.model.TimeBlock;
import cemestre.state.AppState;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Objects;

public final class CombinationTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Id", "Estado", "Creditos", "Dias", "Huecos (min)", "Inicio prom.", "Jornada max. (min)", "Razon"
    };
    private static final Class<?>[] TYPES = {
            Integer.class, String.class, Integer.class, Integer.class, Integer.class, String.class, Integer.class, String.class
    };

    private final AppState state;

    public CombinationTableModel(AppState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public Combination combinationAt(int row) {
        return rows().get(row);
    }

    public int rowOf(Combination combination) {
        return rows().indexOf(combination);
    }

    private List<Combination> rows() {
        return state.getVisibleCombinations();
    }

    @Override
    public int getRowCount() {
        return rows().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Combination combination = combinationAt(row);
        ScheduleMetrics metrics = combination.getMetrics();
        switch (column) {
            case 0: return combination.getId();
            case 1: return combination.getStatusLabel();
            case 2: return combination.getTotalCredits();
            case 3: return metrics.getDayCount();
            case 4: return metrics.getGapMinutes();
            case 5: return TimeBlock.format((int) Math.round(metrics.getAverageStartMinutes()));
            case 6: return metrics.getLongestDayMinutes();
            case 7: return combination.isValid() ? "" : combination.getStatusDetail();
            default: throw new IllegalArgumentException("columna inexistente: " + column);
        }
    }
}