package cemestre.app;

import cemestre.io.DataLoadException;
import cemestre.io.PrologJsonAdapter;
import cemestre.model.Combination;
import cemestre.model.CombinationSet;
import cemestre.observer.StateEvent;
import cemestre.sorting.SortCriteria;
import cemestre.sorting.SortCriterion;
import cemestre.state.AppState;
import cemestre.state.CombinationFilter;

import java.nio.file.Path;
import java.util.List;

public final class SortingDemo {
    private static final int SHOWN = 5;

    private SortingDemo() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -cp target/classes cemestre.app.SortingDemo <resultados.json>");
            System.exit(1);
        }
        CombinationSet data;
        try {
            data = new PrologJsonAdapter().load(Path.of(args[0]));
        } catch (DataLoadException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
            return;
        }

        List<SortCriterion> criteria = SortCriteria.available();
        AppState state = new AppState(criteria.get(0));
        state.addObserver(event -> print(state, event));   // el observador: solo imprime

        state.load(data);
        for (SortCriterion criterion : criteria.subList(1, criteria.size())) {
            state.setCriterion(criterion);
        }
        state.setFilter(CombinationFilter.VALID);
        if (!state.getVisibleCombinations().isEmpty()) {
            state.select(state.getVisibleCombinations().get(0));
        }
    }

    private static void print(AppState state, StateEvent event) {
        System.out.println("[" + event + "] criterio: " + state.getCriterion().getName()
                + " | filtro: " + state.getFilter()
                + " | visibles: " + state.getVisibleCombinations().size());

        if (event == StateEvent.SELECTION_CHANGED) {
            state.getSelected().ifPresent(selected -> System.out.println(
                    "    seleccionada: " + selected + " -> " + selected.getStatusDetail()));
            return;
        }
        List<Combination> visible = state.getVisibleCombinations();
        for (Combination combination : visible.subList(0, Math.min(SHOWN, visible.size()))) {
            System.out.println("    " + combination.getId() + ": " + combination.getMetrics());
        }
    }
}
