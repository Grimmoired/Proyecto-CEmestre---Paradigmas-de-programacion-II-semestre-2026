package cemestre.model;

import static cemestre.model.TestCourses.block;
import static cemestre.model.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyScheduleTest {
    private final WeeklySchedule schedule = new WeeklySchedule(List.of(
            course("B", 3, block("LUN", "1300", "1450"), block("JUE", "0930", "1120")),
            course("A", 4, block("LUN", "0730", "0920"), block("JUE", "0730", "0920"))));

    @Test
    void groupsBlocksByDayInWeekOrder() {
        assertEquals(List.of(Day.MON, Day.THU), schedule.getDaysWithClasses());
        assertEquals(2, schedule.getBlocks(Day.MON).size());
        assertEquals(2, schedule.getBlocks(Day.THU).size());
    }

    @Test
    void blocksOfADayAreSortedByStart() {
        List<PlacedBlock> monday = schedule.getBlocks(Day.MON);
        assertEquals("A", monday.get(0).getCourseCode());
        assertEquals("B", monday.get(1).getCourseCode());
    }

    @Test
    void placedBlockCarriesWhatTheViewNeeds() {
        PlacedBlock first = schedule.getBlocks(Day.MON).get(0);
        assertEquals("A", first.getCourseCode());
        assertEquals("Curso A", first.getCourseName());
        assertEquals("1", first.getGroupId());
        assertEquals(Day.MON, first.getDay());
        assertEquals(450, first.getStartMinutes());
        assertEquals(560, first.getEndMinutes());
        assertEquals(TestCourses.CE, first.getCareer());
    }

    @Test
    void dayWithoutClassesGivesEmptyListNotNull() {
        assertTrue(schedule.getBlocks(Day.SAT).isEmpty());
    }

    @Test
    void rangeOfTheWeekForDrawing() {
        assertEquals(450, schedule.getEarliestStartMinutes());
        assertEquals(890, schedule.getLatestEndMinutes());
    }

    @Test
    void emptyScheduleHasZeroRange() {
        WeeklySchedule empty = new WeeklySchedule(List.of(course("A", 0)));
        assertTrue(empty.isEmpty());
        assertTrue(empty.getDaysWithClasses().isEmpty());
        assertEquals(0, empty.getEarliestStartMinutes());
        assertEquals(0, empty.getLatestEndMinutes());
    }

    @Test
    void overlappingBlocksAreBothKept() {
        WeeklySchedule overlap = new WeeklySchedule(List.of(
                course("A", 3, block("LUN", "0730", "0920")),
                course("B", 3, block("LUN", "0730", "0920"))));
        assertEquals(2, overlap.getBlocks(Day.MON).size());
    }

    @Test
    void scheduleCannotBeModifiedFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> schedule.getBlocks(Day.MON).clear());
        assertThrows(UnsupportedOperationException.class, () -> schedule.getDaysWithClasses().clear());
    }
}
