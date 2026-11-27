package cemestre.sorting;

import cemestre.model.Combination;

import java.util.Comparator;
import java.util.Objects;

public abstract class MetricCriterion implements SortCriterion {
    private final String name;

    protected MetricCriterion(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    protected abstract Comparator<Combination> primary();

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final Comparator<Combination> comparator() {
        return primary().thenComparingInt(Combination::getId);
    }

    @Override
    public String toString() {
        return name;
    }
}
