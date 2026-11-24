package cemestre.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TimeBlockTest {

    @Test
    void convertsTextToMinutes() {
        assertEquals(450, TimeBlock.minutesOf("0730"));
        assertEquals(0, TimeBlock.minutesOf("0000"));
        assertEquals(1439, TimeBlock.minutesOf("2359"));
    }

    @Test
    void rejectsBadTimes() {
        assertThrows(IllegalArgumentException.class, () -> TimeBlock.minutesOf("730"));
        assertThrows(IllegalArgumentException.class, () -> TimeBlock.minutesOf("07:30"));
        assertThrows(IllegalArgumentException.class, () -> TimeBlock.minutesOf("2460"));
        assertThrows(IllegalArgumentException.class, () -> TimeBlock.minutesOf("2400"));
        assertThrows(IllegalArgumentException.class, () -> TimeBlock.minutesOf(null));
    }

    @Test
    void rejectsEmptyOrInvertedRanges() {
        assertThrows(IllegalArgumentException.class, () -> new TimeBlock(Day.MON, 600, 600));
        assertThrows(IllegalArgumentException.class, () -> new TimeBlock(Day.MON, 700, 600));
    }

    @Test
    void formatsAndComputesDuration() {
        TimeBlock block = new TimeBlock(Day.TUE, 450, 560);
        assertEquals("MAR 07:30-09:20", block.toString());
        assertEquals(110, block.getDurationMinutes());
    }

    @Test
    void dayLookupByCode() {
        assertEquals(Day.SAT, Day.fromCode("SAB"));
        assertThrows(IllegalArgumentException.class, () -> Day.fromCode("DOM"));
    }
}
