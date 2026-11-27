package cemestre.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cemestre.io.PrologJsonAdapter;
import cemestre.model.Combination;
import cemestre.model.CombinationSet;
import cemestre.model.ValidCombination;
import cemestre.observer.StateEvent;
import cemestre.observer.StateObserver;
import cemestre.sorting.FewestDaysCriterion;
import cemestre.sorting.FewestGapsCriterion;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppStateTest {
    private static CombinationSet realData;

    private AppState state;
    private List<StateEvent> events;

    @BeforeAll
    static void loadRealData() throws Exception {
        realData = new PrologJsonAdapter().load(Path.of("src", "test", "resources", "outputPrologMixto.json"));
    }

    @BeforeEach
    void setUp() {
        state = new AppState(new FewestDaysCriterion());
        events = new ArrayList<>();
        state.addObserver(events::add);   // observador de prueba: anota cada evento que recibe
    }

    private static List<Integer> firstIds(List<Combination> combinations, int n) {
        List<Integer> ids = new ArrayList<>();
        for (Combination combination : combinations.subList(0, n)) {
            ids.add(combination.getId());
        }
        return ids;
    }

    private Combination firstRejectedVisible() {
        for (Combination combination : state.getVisibleCombinations()) {
            if (!combination.isValid()) {
                return combination;
            }
        }
        throw new AssertionError("no hay rechazadas visibles");
    }

    @Test
    void startsWithoutData() {
        assertFalse(state.hasData());
        assertTrue(state.getVisibleCombinations().isEmpty());
        assertTrue(state.getSelected().isEmpty());
    }

    @Test
    void loadShowsEverythingSortedAndNotifies() {
        state.load(realData);
        assertEquals(List.of(StateEvent.DATA_LOADED), events);
        assertEquals(347, state.getVisibleCombinations().size());
        assertEquals(List.of(88, 92, 95), firstIds(state.getVisibleCombinations(), 3));
    }

    @Test
    void changingTheCriterionReordersWithoutReloading() {
        state.load(realData);
        events.clear();
        state.setCriterion(new FewestGapsCriterion());
        assertEquals(List.of(StateEvent.CRITERION_CHANGED), events);
        assertEquals(347, state.getVisibleCombinations().size());
        assertEquals(List.of(80, 81, 86), firstIds(state.getVisibleCombinations(), 3));
    }

    @Test
    void settingTheSameCriterionDoesNotNotify() {
        state.load(realData);
        events.clear();
        state.setCriterion(state.getCriterion());
        assertTrue(events.isEmpty());
    }

    @Test
    void filterShowsValidOrRejected() {
        state.load(realData);

        state.setFilter(CombinationFilter.VALID);
        assertEquals(42, state.getVisibleCombinations().size());
        assertTrue(state.getVisibleCombinations().stream().allMatch(Combination::isValid));

        state.setFilter(CombinationFilter.REJECTED);
        assertEquals(305, state.getVisibleCombinations().size());
        assertTrue(state.getVisibleCombinations().stream().noneMatch(Combination::isValid));

        state.setFilter(CombinationFilter.ALL);
        assertEquals(347, state.getVisibleCombinations().size());
    }

    @Test
    void filteredListKeepsTheCriterionOrder() {
        state.load(realData);
        state.setFilter(CombinationFilter.VALID);
        List<Combination> visible = state.getVisibleCombinations();
        for (int i = 1; i < visible.size(); i++) {
            assertTrue(state.getCriterion().comparator().compare(visible.get(i - 1), visible.get(i)) <= 0,
                    "fuera de orden en la posicion " + i);
        }
    }

    @Test
    void selectingNotifiesOnlyWhenTheSelectionChanges() {
        state.load(realData);
        events.clear();
        Combination first = state.getVisibleCombinations().get(0);

        state.select(first);
        assertEquals(List.of(StateEvent.SELECTION_CHANGED), events);
        assertEquals(first, state.getSelected().orElseThrow());

        state.select(first);
        assertEquals(1, events.size());
    }

    @Test
    void aFilterThatHidesTheSelectionClearsIt() {
        state.load(realData);
        state.select(firstRejectedVisible());
        events.clear();

        state.setFilter(CombinationFilter.VALID);
        assertEquals(List.of(StateEvent.FILTER_CHANGED, StateEvent.SELECTION_CHANGED), events);
        assertTrue(state.getSelected().isEmpty());
    }

    @Test
    void loadingNewDataClearsTheSelection() {
        state.load(realData);
        state.select(state.getVisibleCombinations().get(0));
        state.load(realData);
        assertTrue(state.getSelected().isEmpty());
    }

    @Test
    void cannotSelectACombinationThatIsNotLoaded() {
        state.load(realData);
        Combination outsider = new ValidCombination(9999, List.of(), 0);
        assertThrows(IllegalArgumentException.class, () -> state.select(outsider));
    }

    @Test
    void everyObserverReceivesTheSameEvents() {
        List<StateEvent> otherView = new ArrayList<>();
        state.addObserver(otherView::add);

        state.load(realData);
        state.setCriterion(new FewestGapsCriterion());
        state.select(state.getVisibleCombinations().get(0));

        assertEquals(events, otherView);
        assertEquals(3, otherView.size());
    }

    @Test
    void aRemovedObserverStopsReceivingEvents() {
        List<StateEvent> otherEvents = new ArrayList<>();
        StateObserver otherView = otherEvents::add;   // se guarda para poder quitar el mismo objeto
        state.addObserver(otherView);
        state.removeObserver(otherView);

        state.load(realData);
        assertTrue(otherEvents.isEmpty());
        assertEquals(1, events.size());
    }
}