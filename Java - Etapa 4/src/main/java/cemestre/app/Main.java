package cemestre.app;

import cemestre.io.PrologJsonAdapter;
import cemestre.sorting.SortCriteria;
import cemestre.sorting.SortCriterion;
import cemestre.state.AppState;
import cemestre.view.MainFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path initialFile = args.length > 0 ? Path.of(args[0]) : null;

        SwingUtilities.invokeLater(() -> {
            List<SortCriterion> criteria = SortCriteria.available();
            AppState state = new AppState(criteria.get(0));
            MainFrame frame = new MainFrame(state, criteria, new PrologJsonAdapter());
            frame.setVisible(true);
            if (initialFile != null) {
                frame.openFile(initialFile);
            }
        });
    }
}