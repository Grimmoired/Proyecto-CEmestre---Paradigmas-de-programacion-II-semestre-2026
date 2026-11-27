package cemestre.model;

import java.util.ArrayList;
import java.util.List;

final class TestCourses {
    static final String CE = "Ingenieria en Computadores";
    static final String FI = "Ingenieria Fisica";

    private TestCourses() {
    }

    static TimeBlock block(String day, String start, String end) {
        return new TimeBlock(Day.fromCode(day), TimeBlock.minutesOf(start), TimeBlock.minutesOf(end));
    }

    static CourseSelection course(String code, int credits, TimeBlock... blocks) {
        return course(code, credits, CE, blocks);
    }

    static CourseSelection course(String code, int credits, String career, TimeBlock... blocks) {
        return new CourseSelection(code, "Curso " + code, "1", credits, career, "Matematicas", List.of(blocks));
    }

    static Combination combination(CourseSelection... courses) {
        CombinationBuilder builder = new CombinationBuilder(1);
        for (CourseSelection course : courses) {
            builder.addCourse(course);
        }
        return builder.build();
    }

    static ScheduleMetrics metricsOf(CourseSelection... courses) {
        return combination(courses).getMetrics();
    }

    static List<CourseSelection> listOf(CourseSelection... courses) {
        return new ArrayList<>(List.of(courses));
    }
}
