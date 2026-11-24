package cemestre.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PolicyInfo {
    private final Map<String, Double> maxPercentages;
    private final int maxCoursesPerArea;
    private final List<IncompatiblePair> incompatiblePairs;
    private final List<String> ruleOrder;

    public PolicyInfo(Map<String, Double> maxPercentages, int maxCoursesPerArea, List<IncompatiblePair> incompatiblePairs, List<String> ruleOrder) {
        this.maxPercentages = Collections.unmodifiableMap(new LinkedHashMap<>(maxPercentages));
        this.maxCoursesPerArea = maxCoursesPerArea;
        this.incompatiblePairs = List.copyOf(incompatiblePairs);
        this.ruleOrder = List.copyOf(ruleOrder);
    }

    public Map<String, Double> getMaxPercentages() {
        return maxPercentages;
    }
    public int getMaxCoursesPerArea() {
        return maxCoursesPerArea;
    }
    public List<IncompatiblePair> getIncompatiblePairs() {
        return incompatiblePairs;
    }
    public List<String> getRuleOrder() {
        return ruleOrder;
    }
}
