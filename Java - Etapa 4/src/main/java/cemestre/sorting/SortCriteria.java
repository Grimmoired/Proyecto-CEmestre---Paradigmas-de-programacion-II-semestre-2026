package cemestre.sorting;

import java.util.List;

public final class SortCriteria {

    private SortCriteria() {
    }

    public static List<SortCriterion> available() {
        return List.of(
                new FewestDaysCriterion(),
                new FewestGapsCriterion(),
                new EarliestAverageStartCriterion(),
                new ShortestLongestDayCriterion());
    }
}