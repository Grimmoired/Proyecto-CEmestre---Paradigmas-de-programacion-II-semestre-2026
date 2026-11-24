package cemestre.model;

import cemestre.model.reason.RejectionReason;

import java.util.List;
import java.util.Objects;

public final class RejectedCombination extends Combination {
    private final RejectionReason reason;
    private final List<CourseRef> removable;

    public RejectedCombination(int id, List<CourseSelection> selections, int totalCredits, RejectionReason reason, List<CourseRef> removable) {
        super(id, selections, totalCredits);
        this.reason = Objects.requireNonNull(reason, "reason");
        this.removable = List.copyOf(removable);
    }

    public RejectionReason getReason() {
        return reason;
    }
    public List<CourseRef> getRemovable() {
        return removable;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String getStatusLabel() {
        return "Rechazada";
    }
}
