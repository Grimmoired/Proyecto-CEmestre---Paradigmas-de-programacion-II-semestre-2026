package cemestre.model;

import java.util.List;
import java.util.Objects;

public final class ScheduleMetrics {
    public static final int DEFAULT_MIN_GAP_MINUTES = 10;

    private final int dayCount;
    private final int gapMinutes;
    private final double averageStartMinutes;
    private final int longestDayMinutes;

    private ScheduleMetrics(int dayCount, int gapMinutes, double averageStartMinutes, int longestDayMinutes) {
        this.dayCount = dayCount;
        this.gapMinutes = gapMinutes;
        this.averageStartMinutes = averageStartMinutes;
        this.longestDayMinutes = longestDayMinutes;
    }

    public static ScheduleMetrics from(WeeklySchedule schedule) {
        return from(schedule, DEFAULT_MIN_GAP_MINUTES);
    }

    public static ScheduleMetrics from(WeeklySchedule schedule, int minGapMinutes) {
        Objects.requireNonNull(schedule, "schedule");
        if (minGapMinutes < 0) {
            throw new IllegalArgumentException("el hueco minimo no puede ser negativo: " + minGapMinutes);
        }

        List<Day> days = schedule.getDaysWithClasses();
        int totalGaps = 0;
        int sumOfFirstStarts = 0;
        int longestDay = 0;

        for (Day day : days) {
            List<PlacedBlock> blocks = schedule.getBlocks(day);

            int firstStart = blocks.get(0).getStartMinutes();
            sumOfFirstStarts += firstStart;

            int latestEnd = blocks.get(0).getEndMinutes();
            for (int i = 1; i < blocks.size(); i++) {
                PlacedBlock current = blocks.get(i);
                int gap = Math.max(0, current.getStartMinutes() - latestEnd);
                if (gap > minGapMinutes) {
                    totalGaps += gap;
                }
                latestEnd = Math.max(latestEnd, current.getEndMinutes());
            }

            longestDay = Math.max(longestDay, latestEnd - firstStart);
        }

        double averageStart = days.isEmpty() ? 0.0 : (double) sumOfFirstStarts / days.size();
        return new ScheduleMetrics(days.size(), totalGaps, averageStart, longestDay);
    }

    public int getDayCount() {
        return dayCount;
    }

    public int getGapMinutes() {
        return gapMinutes;
    }

    public double getAverageStartMinutes() {
        return averageStartMinutes;
    }

    public int getLongestDayMinutes() {
        return longestDayMinutes;
    }

    public boolean hasClasses() {
        return dayCount > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScheduleMetrics)) {
            return false;
        }
        ScheduleMetrics that = (ScheduleMetrics) other;
        return dayCount == that.dayCount && gapMinutes == that.gapMinutes
                && Double.compare(averageStartMinutes, that.averageStartMinutes) == 0
                && longestDayMinutes == that.longestDayMinutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayCount, gapMinutes, averageStartMinutes, longestDayMinutes);
    }

    @Override
    public String toString() {
        return dayCount + " dias, " + gapMinutes + " min de huecos, inicio promedio "
                + TimeBlock.format((int) Math.round(averageStartMinutes))
                + ", jornada mas larga " + longestDayMinutes + " min";
    }
}
