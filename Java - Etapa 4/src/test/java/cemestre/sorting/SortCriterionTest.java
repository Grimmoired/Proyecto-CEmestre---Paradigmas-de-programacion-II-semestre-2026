package cemestre.sorting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cemestre.io.PrologJsonAdapter;
import cemestre.model.Combination;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SortCriterionTest {
    private static List<Combination> combinations;

    @BeforeAll
    static void loadRealData() throws Exception {
        combinations = new PrologJsonAdapter()
                .load(Path.of("src", "test", "resources", "outputPrologMixto.json"))
                .getCombinations();
    }

    private static List<Combination> sortedBy(SortCriterion criterion) {
        List<Combination> sorted = new ArrayList<>(combinations);
        sorted.sort(criterion.comparator());
        return sorted;
    }

    private static List<Integer> firstIds(SortCriterion criterion, int n) {
        List<Integer> ids = new ArrayList<>();
        for (Combination c : sortedBy(criterion).subList(0, n)) {
            ids.add(c.getId());
        }
        return ids;
    }

    @Test
    void fewestDays() {
        assertEquals(List.of(88, 92, 95, 126, 130), firstIds(new FewestDaysCriterion(), 5));
    }

    @Test
    void fewestGaps() {
        assertEquals(List.of(80, 81, 86, 87, 334), firstIds(new FewestGapsCriterion(), 5));
    }

    @Test
    void earliestAverageStart() {
        assertEquals(List.of(88, 90, 92, 93, 95), firstIds(new EarliestAverageStartCriterion(), 5));
    }

    @Test
    void shortestLongestDay() {
        assertEquals(List.of(86, 87, 342, 343, 59), firstIds(new ShortestLongestDayCriterion(), 5));
    }

    @Test
    void tiesAreBrokenById() {
        List<MetricCriterion> all = List.of(new FewestDaysCriterion(), new FewestGapsCriterion(),
                new EarliestAverageStartCriterion(), new ShortestLongestDayCriterion());
        for (MetricCriterion criterion : all) {
            List<Combination> sorted = sortedBy(criterion);
            for (int i = 1; i < sorted.size(); i++) {
                Combination previous = sorted.get(i - 1);
                Combination current = sorted.get(i);
                if (criterion.primary().compare(previous, current) == 0) {
                    assertTrue(previous.getId() < current.getId(),
                            criterion.getName() + ": empate entre " + previous.getId()
                                    + " y " + current.getId() + " no se resolvio por id");
                }
            }
        }
    }
}
