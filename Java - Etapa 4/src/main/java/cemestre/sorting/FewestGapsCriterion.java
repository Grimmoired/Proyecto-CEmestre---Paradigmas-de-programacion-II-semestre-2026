package cemestre.sorting;

import cemestre.model.Combination;

import java.util.Comparator;

public class FewestGapsCriterion extends MetricCriterion {
    public FewestGapsCriterion() {
        super("Menos huecos");
    }

    @Override
    protected Comparator<Combination> primary() {
        return Comparator.comparingInt(c -> c.getMetrics().getGapMinutes());
    }
}
