package cemestre.model.reason;

import java.util.Objects;

/** Regla 3: la combinacion incluye los dos cursos de un par declarado incompatible. */
public final class IncompatibilityReason extends RejectionReason {
    private final String courseA;
    private final String courseB;

    public IncompatibilityReason(String message, String courseA, String courseB) {
        super("incompatibility", message);
        this.courseA = Objects.requireNonNull(courseA, "courseA");
        this.courseB = Objects.requireNonNull(courseB, "courseB");
    }

    public String getCourseA() {
        return courseA;
    }

    public String getCourseB() {
        return courseB;
    }

    @Override
    public String describe() {
        return "Incompatibilidad: " + courseA + " y " + courseB + " no pueden llevarse juntos";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IncompatibilityReason)) {
            return false;
        }
        IncompatibilityReason that = (IncompatibilityReason) other;
        return getMessage().equals(that.getMessage())
                && courseA.equals(that.courseA)
                && courseB.equals(that.courseB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMessage(), courseA, courseB);
    }
}
