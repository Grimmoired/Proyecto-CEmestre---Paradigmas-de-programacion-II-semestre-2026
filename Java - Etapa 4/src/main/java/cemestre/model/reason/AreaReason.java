package cemestre.model.reason;

import java.util.Objects;

/** Regla 2: hay mas cursos de una misma area que el maximo permitido. */
public final class AreaReason extends RejectionReason {
    private final String area;
    private final int count;
    private final int maxCourses;

    public AreaReason(String message, String area, int count, int maxCourses) {
        super("area", message);
        this.area = Objects.requireNonNull(area, "area");
        this.count = count;
        this.maxCourses = maxCourses;
    }

    public String getArea() {
        return area;
    }

    public int getCount() {
        return count;
    }

    public int getMaxCourses() {
        return maxCourses;
    }

    @Override
    public String describe() {
        return "Area: " + count + " cursos de " + area + " (maximo " + maxCourses + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AreaReason)) {
            return false;
        }
        AreaReason that = (AreaReason) other;
        return getMessage().equals(that.getMessage())
                && area.equals(that.area)
                && count == that.count
                && maxCourses == that.maxCourses;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMessage(), area, count, maxCourses);
    }
}
