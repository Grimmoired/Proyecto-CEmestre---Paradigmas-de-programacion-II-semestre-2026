package cemestre.view;

import cemestre.io.CombinationSource;
import cemestre.io.DataLoadException;
import cemestre.state.AppState;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import cemestre.sorting.SortCriterion;
import java.util.List;

public final class MainFrame extends JFrame {
    private final AppState state;
    private final CombinationSource source;
    private final JLabel statusBar = new JLabel(" ");

    public MainFrame(AppState state, List<SortCriterion> criteria, CombinationSource source) {
        super("CEmestre");
        this.state = Objects.requireNonNull(state, "state");
        this.source = Objects.requireNonNull(source, "source");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(createMenuBar());
        add(createContent(criteria), BorderLayout.CENTER);

        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);

        state.addObserver(event -> updateStatusBar());   // la barra de estado tambien es un observador
        updateStatusBar();

        setSize(1200, 750);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuItem open = new JMenuItem("Abrir...");
        open.addActionListener(e -> chooseFile());
        JMenuItem exit = new JMenuItem("Salir");
        exit.addActionListener(e -> dispose());

        JMenu fileMenu = new JMenu("Archivo");
        fileMenu.add(open);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu);
        return bar;
    }

    private JComponent createContent(List<SortCriterion> criteria) {
        JComponent list = new CombinationListPanel(state, criteria);
        JComponent schedule = new ScheduleView(state);
        JComponent summary = new CreditSummaryView(state);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, schedule, summary);
        right.setResizeWeight(0.6);      // al agrandar la ventana, el horario se lleva el 75 % del espacio extra

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, right);
        main.setResizeWeight(0.4);
        return main;
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("Resultados de Prolog (*.json)", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile().toPath());
        }
    }

    public void openFile(Path file) {
        try {
            state.load(source.load(file));
            setTitle("CEmestre - " + file.getFileName());
        } catch (DataLoadException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "No se pudo abrir " + file.getFileName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatusBar() {
        if (!state.hasData()) {
            statusBar.setText("Sin datos: use Archivo > Abrir para cargar la salida de Prolog");
            return;
        }
        statusBar.setText(state.getTotalCount() + " combinaciones (" + state.getValidCount() + " validas)"
                + " | mostrando " + state.getVisibleCombinations().size()
                + " | criterio: " + state.getCriterion().getName()
                + " | filtro: " + state.getFilter());
    }
}
