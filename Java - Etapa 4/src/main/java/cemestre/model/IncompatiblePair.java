package cemestre.model;

public final class IncompatiblePair {
    private final String courseA;
    private final String courseB;

    public IncompatiblePair(String courseA, String courseB) {
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
    public String toString() {
        return courseA + " - " + courseB;
    }
}
