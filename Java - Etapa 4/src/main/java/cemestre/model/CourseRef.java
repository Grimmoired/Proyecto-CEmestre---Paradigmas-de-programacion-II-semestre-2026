package cemestre.model;

import java.util.Objects;

public final class CourseRef {
    private final String courseCode;
    private final String groupId;

    public CourseRef(String courseCode, String groupId) {
        this.courseCode = Objects.requireNonNull(courseCode, "courseCode");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getGroupId() {
        return groupId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CourseRef)) {
            return false;
        }
        CourseRef that = (CourseRef) other;
        return courseCode.equals(that.courseCode) && groupId.equals(that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode, groupId);
    }

    @Override
    public String toString() {
        return courseCode + " grupo " + groupId;
    }
}
