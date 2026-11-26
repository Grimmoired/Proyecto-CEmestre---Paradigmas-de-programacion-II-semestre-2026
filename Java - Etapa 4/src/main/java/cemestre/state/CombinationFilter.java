package cemestre.state;

import cemestre.model.Combination;

import java.util.function.Predicate;

public enum CombinationFilter {
    ALL("Todas", combination -> true),
    VALID("Validas", Combination::isValid),
    REJECTED("Rechazadas", combination -> !combination.isValid());

    private final String label;
    private final Predicate<Combination> condition;

    CombinationFilter(String label, Predicate<Combination> condition) {
        this.label = label;
        this.condition = condition;
    }

    public boolean accepts(Combination combination) {
        return condition.test(combination);
    }

    @Override
    public String toString() {
        return label;
    }
}
