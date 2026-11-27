package cemestre.model;

import java.util.Objects;

public final class PlacedBlock {
    private final CourseSelection course;
    private final TimeBlock block;

    public PlacedBlock(CourseSelection course, TimeBlock block) {
        this.course = Objects.requireNonNull(course, "course");
        this.block = Objects.requireNonNull(block, "block");
    }

    public CourseSelection getCourse() {
        return course;
    }
    public TimeBlock getBlock() {
        return block;
    }

    public Day getDay() {
        return block.getDay();
    }
    public int getStartMinutes() {
        return block.getStartMinutes();
    }
    public int getEndMinutes() {
        return block.getEndMinutes();
    }
    public String getCourseCode() {
        return course.getCourseCode();
    }
    public String getCourseName() {
        return course.getCourseName();
    }
    public String getGroupId() {
        return course.getGroupId();
    }
    public String getCareer() {
        return course.getCareer();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlacedBlock)) {
            return false;
        }
        PlacedBlock that = (PlacedBlock) other;
        return course.equals(that.course) && block.equals(that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(course, block);
    }

    @Override
    public String toString() {
        return block + " " + course.getCourseCode() + " grupo " + course.getGroupId();
    }
}
