package cemestre.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * La jerarquia es Combination -> ValidCombination / RejectedCombination (la rechazada es la unica que tiene razon)
 */
public abstract class Combination {
    private final int id;
    private final List<CourseSelection> selections;
    private final int totalCredits;
    private final WeeklySchedule schedule;
    private final ScheduleMetrics metrics;
    private final Map<String, Integer> creditsByCareer;

    protected Combination(int id, List<CourseSelection> selections, int totalCredits) {
        this.id = id;
        this.selections = List.copyOf(Objects.requireNonNull(selections, "selections"));
        this.totalCredits = totalCredits;
        this.schedule = new WeeklySchedule(this.selections);
        this.metrics = ScheduleMetrics.from(this.schedule);
        this.creditsByCareer = sumCreditsByCareer(this.selections);
    }

    private static Map<String, Integer> sumCreditsByCareer(List<CourseSelection> selections) {
        Map<String, Integer> sums = new LinkedHashMap<>();
        for (CourseSelection course : selections) {
            sums.merge(course.getCareer(), course.getCredits(), Integer::sum);
        }
        return Collections.unmodifiableMap(sums);
    }

    public int getId() {
        return id;
    }
    public List<CourseSelection> getSelections() {
        return selections;
    }

    public int getTotalCredits() {
        return totalCredits;
    }
    public WeeklySchedule getSchedule() {
        return schedule;
    }
    public ScheduleMetrics getMetrics() {
        return metrics;
    }

    public Map<String, Integer> getCreditsByCareer() {
        return creditsByCareer;
    }

    public abstract boolean isValid();

    public abstract String getStatusLabel();

    public abstract String getStatusDetail();

    public abstract List<CourseRef> getRemovable();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id == ((Combination) other).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return "Combinacion " + id + " (" + getStatusLabel() + ", " + totalCredits + " creditos)";
    }
}
