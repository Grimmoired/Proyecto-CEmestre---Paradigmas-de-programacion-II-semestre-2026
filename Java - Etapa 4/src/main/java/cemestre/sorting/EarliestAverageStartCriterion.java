package cemestre.sorting;

import cemestre.model.Combination;

import java.util.Comparator;

public class EarliestAverageStartCriterion extends MetricCriterion {
    public EarliestAverageStartCriterion() {
        super("Hora promedio de inicio");
    }

    @Override
    protected Comparator<Combination> primary() {
        return Comparator.comparingDouble(c -> c.getMetrics().getAverageStartMinutes());
    }
}