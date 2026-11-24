package cemestre.model;

import java.util.List;
import java.util.Objects;

public final class CourseSelection {
    private final String courseCode;
    private final String courseName;
    private final String groupId;
    private final int credits;
    private final String career;
    private final String area;
    private final List<TimeBlock> blocks;

    public CourseSelection(String courseCode, String courseName, String groupId, int credits, String career, String area, List<TimeBlock> blocks) {
        this.courseCode = Objects.requireNonNull(courseCode, "courseCode");
        this.courseName = Objects.requireNonNull(courseName, "courseName");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.credits = credits;
        this.career = Objects.requireNonNull(career, "career");
        this.area = Objects.requireNonNull(area, "area");
        this.blocks = List.copyOf(blocks); // copia inmutable: nadie de afuera puede modificarla
    }

    public String getCourseCode() {
        return courseCode;
    }
    public String getCourseName() {
        return courseName;
    }
    public String getGroupId() {
        return groupId;
    }
    public int getCredits() {
        return credits;
    }
    public String getCareer() {
        return career;
    }
    public String getArea() {
        return area;
    }
    public List<TimeBlock> getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return courseCode + " grupo " + groupId;
    }
}
