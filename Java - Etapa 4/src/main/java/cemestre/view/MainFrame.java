package cemestre.view;

import cemestre.io.CombinationSource;
import cemestre.model.CombinationSet;
import cemestre.sorting.SortCriterion;
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
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public final class MainFrame extends JFrame {
    private final AppState state;
    private final CombinationSource source;
    private final JLabel statusBar = new JLabel(" ");
    private final JMenuItem openItem = new JMenuItem("Abrir...");
    private static final int MAX_WARNINGS_SHOWN = 10;

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
        openItem.addActionListener(e -> chooseFile());
        JMenuItem exit = new JMenuItem("Salir");
        exit.addActionListener(e -> dispose());

        JMenu fileMenu = new JMenu("Archivo");
        fileMenu.add(openItem);
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
        right.setResizeWeight(0.6);       // al agrandar la ventana, el horario se lleva el 60 % del espacio extra

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
        setLoading(true, file);
        new SwingWorker<CombinationSet, Void>() {
            @Override
            protected CombinationSet doInBackground() throws Exception {
                return source.load(file);
            }

            @Override
            protected void done() {
                setLoading(false, file);
                try {
                    CombinationSet data = get();
                    state.load(data);
                    setTitle("CEmestre - " + file.getFileName());
                    showWarnings(data.getWarnings());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    JOptionPane.showMessageDialog(MainFrame.this, message,
                            "No se pudo abrir " + file.getFileName(), JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setLoading(boolean loading, Path file) {
        openItem.setEnabled(!loading);
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (loading) {
            statusBar.setText("Cargando " + file.getFileName() + "...");
        } else {
            updateStatusBar();
        }
    }

    private void showWarnings(List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder("El archivo se cargo, pero tiene " + warnings.size() + " advertencia(s):\n\n");
        for (String warning : warnings.subList(0, Math.min(MAX_WARNINGS_SHOWN, warnings.size()))) {
            text.append("- ").append(warning).append('\n');
        }
        if (warnings.size() > MAX_WARNINGS_SHOWN) {
            text.append("... y ").append(warnings.size() - MAX_WARNINGS_SHOWN).append(" mas");
        }
        JOptionPane.showMessageDialog(this, text.toString(), "Advertencias", JOptionPane.WARNING_MESSAGE);
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