package cemestre.sorting;

import cemestre.model.Combination;

import java.util.Comparator;

public final class FewestDaysCriterion extends MetricCriterion {
    public FewestDaysCriterion() {
        super("Menos días en el TEC");
    }

    @Override
    protected Comparator<Combination> primary() {
        return Comparator.comparingInt(c -> c.getMetrics().getDayCount());
    }
}