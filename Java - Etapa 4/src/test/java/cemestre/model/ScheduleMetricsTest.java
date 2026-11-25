package cemestre.model;

import static cemestre.model.TestCourses.block;
import static cemestre.model.TestCourses.course;
import static cemestre.model.TestCourses.metricsOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleMetricsTest {
    @Test
    void oneDayWithSeveralCoursesCountsAsOneDay() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920")),
                course("B", 3, block("LUN", "0930", "1120")));
        assertEquals(1, m.getDayCount());
    }

    @Test
    void severalDaysAreCountedOnce() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920"), block("JUE", "0730", "0920")),
                course("B", 3, block("MAR", "0930", "1120"), block("JUE", "1300", "1450")));
        assertEquals(3, m.getDayCount());
    }

    @Test
    void tenMinuteBreakIsNotAGap() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920")),
                course("B", 3, block("LUN", "0930", "1120")));
        assertEquals(0, m.getGapMinutes());
    }

    @Test
    void elevenMinuteGapDoesCount() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920")),
                course("B", 3, block("LUN", "0931", "1120")));
        assertEquals(11, m.getGapMinutes());
    }

    @Test
    void bigGapIsCountedInFull() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920")),
                course("B", 3, block("LUN", "1300", "1450")));
        assertEquals(220, m.getGapMinutes());
    }

    @Test
    void gapsAreAddedAcrossDaysButNotBetweenDays() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920"), block("MAR", "0930", "1120")),
                course("B", 3, block("LUN", "1300", "1450"), block("MAR", "1300", "1450")));
        assertEquals(320, m.getGapMinutes());
    }

    @Test
    void blocksGivenOutOfOrderAreSortedFirst() {
        ScheduleMetrics m = metricsOf(
                course("B", 3, block("LUN", "1300", "1450")),
                course("A", 3, block("LUN", "0730", "0920")));
        assertEquals(220, m.getGapMinutes());
    }

    @Test
    void overlappingBlocksNeverGiveNegativeGaps() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "1120")),
                course("B", 3, block("LUN", "0930", "1020")),
                course("C", 3, block("LUN", "1300", "1450")));

        assertEquals(100, m.getGapMinutes());
    }

    @Test
    void minimumGapCanBeChanged() {
        Combination c = TestCourses.combination(
                course("A", 3, block("LUN", "0730", "0920")),
                course("B", 3, block("LUN", "0930", "1120")));
        assertEquals(0, ScheduleMetrics.from(c.getSchedule()).getGapMinutes());
        assertEquals(10, ScheduleMetrics.from(c.getSchedule(), 0).getGapMinutes());
        assertThrows(IllegalArgumentException.class, () -> ScheduleMetrics.from(c.getSchedule(), -1));
    }

    @Test
    void averageStartUsesOnlyTheFirstLessonOfEachDay() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920"), block("LUN", "1300", "1450")),
                course("B", 3, block("MAR", "1300", "1450")));
        assertEquals((450 + 780) / 2.0, m.getAverageStartMinutes());
    }

    @Test
    void averageStartWithOneDayIsThatDaysStart() {
        ScheduleMetrics m = metricsOf(course("A", 3, block("VIE", "0930", "1120")));
        assertEquals(570.0, m.getAverageStartMinutes());
    }

    @Test
    void averageStartCanHaveDecimals() {
        ScheduleMetrics m = metricsOf(course("A", 3,
                block("LUN", "0730", "0920"), block("MAR", "0730", "0920"), block("MIE", "0731", "0920")));
        assertEquals(1351 / 3.0, m.getAverageStartMinutes(), 1e-9);
    }

    @Test
    void longestDayGoesFromFirstStartToLastEnd() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "0920"), block("MAR", "0930", "1120")),
                course("B", 3, block("LUN", "1300", "1450")));
        assertEquals(440, m.getLongestDayMinutes());
    }

    @Test
    void longestDayUsesTheLatestEndEvenIfAnEarlierBlockIsLonger() {
        ScheduleMetrics m = metricsOf(
                course("A", 3, block("LUN", "0730", "1650")),
                course("B", 3, block("LUN", "0930", "1020")));
        assertEquals(560, m.getLongestDayMinutes());
    }

    @Test
    void combinationWithoutBlocksHasZeroMetrics() {
        ScheduleMetrics m = metricsOf(course("A", 0));
        assertFalse(m.hasClasses());
        assertEquals(0, m.getDayCount());
        assertEquals(0, m.getGapMinutes());
        assertEquals(0.0, m.getAverageStartMinutes());
        assertEquals(0, m.getLongestDayMinutes());
    }

    @Test
    void metricsAreComputedOnceAndKept() {
        Combination c = TestCourses.combination(course("A", 3, block("LUN", "0730", "0920")));
        assertTrue(c.getMetrics() == c.getMetrics());
        assertEquals(c.getMetrics(), ScheduleMetrics.from(c.getSchedule()));
    }

    @Test
    void realisticWeek() {
        List<CourseSelection> courses = List.of(
                course("MA1102", 4, block("LUN", "0730", "0920"), block("JUE", "0730", "0920")),
                course("CE1101", 3, block("LUN", "0930", "1120"), block("JUE", "1300", "1450")),
                course("FI1101", 3, block("MAR", "1300", "1450")));
        ScheduleMetrics m = TestCourses.combination(courses.toArray(new CourseSelection[0])).getMetrics();
        assertEquals(3, m.getDayCount());
        assertEquals(220, m.getGapMinutes());
        assertEquals((450 + 780 + 450) / 3.0, m.getAverageStartMinutes());
        assertEquals(440, m.getLongestDayMinutes());
    }
}
