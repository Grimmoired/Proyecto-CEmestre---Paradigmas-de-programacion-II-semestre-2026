package cemestre.model.reason;

/** Regla 3: la combinacion incluye los dos cursos de un par declarado incompatible. */
public final class IncompatibilityReason extends RejectionReason {
    private final String courseA;
    private final String courseB;

    public IncompatibilityReason(String message, String courseA, String courseB) {
        super("incompatibility", message);
        this.courseA = courseA;
        this.courseB = courseB;
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
}
