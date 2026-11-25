package cemestre.model;

import java.util.Objects;

public final class IncompatiblePair {
    private final String courseA;
    private final String courseB;

    public IncompatiblePair(String courseA, String courseB) {
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
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IncompatiblePair)) {
            return false;
        }
        IncompatiblePair that = (IncompatiblePair) other;
        return courseA.equals(that.courseA) && courseB.equals(that.courseB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseA, courseB);
    }

    @Override
    public String toString() {
        return courseA + " - " + courseB;
    }
}
