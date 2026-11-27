package cemestre.model;

import cemestre.model.reason.RejectionReason;

import java.util.ArrayList;
import java.util.List;

public final class CombinationBuilder {
    private final int id;
    private final List<CourseSelection> selections = new ArrayList<>();
    private final List<CourseRef> removable = new ArrayList<>();
    private Integer totalCredits;
    private RejectionReason reason;

    public CombinationBuilder(int id) {
        this.id = id;
    }

    public CombinationBuilder addCourse(CourseSelection course) {
        selections.add(course);
        return this;
    }

    public CombinationBuilder totalCredits(int total) {
        this.totalCredits = total;
        return this;
    }

    public CombinationBuilder rejectedBecause(RejectionReason reason) {
        this.reason = reason;
        return this;
    }

    public CombinationBuilder addRemovable(CourseRef course) {
        removable.add(course);
        return this;
    }

    public Combination build() {
        int total = (totalCredits != null) ? totalCredits : sumCredits();

        if (reason == null) {
            if (!removable.isEmpty()) {
                throw new IllegalStateException("una combinacion valida no puede tener cursos a quitar");
            }
            return new ValidCombination(id, selections, total);
        }
        return new RejectedCombination(id, selections, total, reason, removable);
    }

    private int sumCredits() {
        int sum = 0;
        for (CourseSelection course : selections) {
            sum += course.getCredits();
        }
        return sum;
    }
}
