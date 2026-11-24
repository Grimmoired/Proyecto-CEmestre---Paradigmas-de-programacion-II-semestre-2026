package cemestre.model;

import java.util.List;

public final class CombinationSet {
    private final PolicyInfo policy;
    private final List<Combination> combinations;
    private final List<String> warnings;

    public CombinationSet(PolicyInfo policy, List<Combination> combinations, List<String> warnings) {
        this.policy = policy;
        this.combinations = List.copyOf(combinations);
        this.warnings = List.copyOf(warnings);
    }

    public PolicyInfo getPolicy() {
        return policy;
    }
    public List<Combination> getCombinations() {
        return combinations;
    }
    public List<String> getWarnings() {
        return warnings;
    }
    public int size() {
        return combinations.size();
    }
    public int getValidCount() {
        int count = 0;
        for (Combination combination : combinations) {
            if (combination.isValid()) {
                count++;
            }
        }
        return count;
    }

    public int getRejectedCount() {
        return combinations.size() - getValidCount();
    }
}
