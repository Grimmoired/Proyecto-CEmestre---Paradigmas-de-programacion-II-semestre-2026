package cemestre.sorting;

import cemestre.model.Combination;

import java.util.Comparator;

public class ShortestLongestDayCriterion extends MetricCriterion {
    public ShortestLongestDayCriterion() {
        super("Jornada mas larga mas corta");
    }

    @Override
    protected Comparator<Combination> primary() {
        return Comparator.comparingInt(c -> c.getMetrics().getLongestDayMinutes());
    }
}
