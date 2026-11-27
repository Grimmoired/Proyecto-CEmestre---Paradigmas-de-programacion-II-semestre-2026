package cemestre.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cemestre.io.PrologJsonAdapter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RealDataMetricsTest {
    private static CombinationSet load(String name) throws Exception {
        Path file = Path.of(RealDataMetricsTest.class.getResource("/" + name).toURI());
        return new PrologJsonAdapter().load(file);
    }

    @Test
    void firstMixedCombinationCheckedByHand() throws Exception {
        Combination first = load("outputPrologMixto.json").getCombinations().get(0);
        assertEquals(1, first.getId());
        ScheduleMetrics m = first.getMetrics();
        assertEquals(3, m.getDayCount());
        assertEquals(630, m.getGapMinutes());
        assertEquals((450 + 780 + 450) / 3.0, m.getAverageStartMinutes());
        assertEquals(800, m.getLongestDayMinutes());
    }

    @Test
    void totalsOverTheWholeMixedFileMatchTheIndependentCalculation() throws Exception {
        assertTotals(load("outputPrologMixto.json"), 347, 1363, 185810, 220090, 230178.5);
    }

    @Test
    void totalsOverCeAndFiFilesMatch() throws Exception {
        assertTotals(load("outputPrologCE.json"), 7, 17, 3110, 3560, 4340.0);
        assertTotals(load("outputPrologFI.json"), 24, 72, 16000, 14640, 15440.0);
    }

    private static void assertTotals(CombinationSet set, int count, int days, int gaps, int longest, double avg) {
        int sumDays = 0;
        int sumGaps = 0;
        int sumLongest = 0;
        double sumAverage = 0;
        for (Combination combination : set.getCombinations()) {
            ScheduleMetrics m = combination.getMetrics();
            sumDays += m.getDayCount();
            sumGaps += m.getGapMinutes();
            sumLongest += m.getLongestDayMinutes();
            sumAverage += m.getAverageStartMinutes();
        }
        assertEquals(count, set.size());
        assertEquals(days, sumDays);
        assertEquals(gaps, sumGaps);
        assertEquals(longest, sumLongest);
        assertEquals(avg, sumAverage, 1e-6);
    }
}
