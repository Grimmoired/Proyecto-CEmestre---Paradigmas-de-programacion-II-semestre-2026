package cemestre.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WeeklySchedule {
    private static final Comparator<PlacedBlock> BY_TIME = Comparator
            .comparingInt(PlacedBlock::getStartMinutes)
            .thenComparingInt(PlacedBlock::getEndMinutes)
            .thenComparing(PlacedBlock::getCourseCode);

    private final Map<Day, List<PlacedBlock>> blocksByDay;

    public WeeklySchedule(List<CourseSelection> selections) {
        Map<Day, List<PlacedBlock>> temp = new EnumMap<>(Day.class);
        for (CourseSelection course : selections) {
            for (TimeBlock block : course.getBlocks()) {
                temp.computeIfAbsent(block.getDay(), day -> new ArrayList<>())
                        .add(new PlacedBlock(course, block));
            }
        }

        Map<Day, List<PlacedBlock>> result = new EnumMap<>(Day.class);
        for (Map.Entry<Day, List<PlacedBlock>> entry : temp.entrySet()) {
            List<PlacedBlock> dayBlocks = entry.getValue();
            dayBlocks.sort(BY_TIME);
            result.put(entry.getKey(), List.copyOf(dayBlocks));
        }
        this.blocksByDay = Collections.unmodifiableMap(result);
    }

    public List<PlacedBlock> getBlocks(Day day) {
        return blocksByDay.getOrDefault(day, List.of());
    }

    public List<Day> getDaysWithClasses() {
        return List.copyOf(blocksByDay.keySet());
    }

    public boolean isEmpty() {
        return blocksByDay.isEmpty();
    }

    public int getEarliestStartMinutes() {
        int earliest = Integer.MAX_VALUE;
        for (List<PlacedBlock> dayBlocks : blocksByDay.values()) {
            earliest = Math.min(earliest, dayBlocks.get(0).getStartMinutes());
        }
        return isEmpty() ? 0 : earliest;
    }

    public int getLatestEndMinutes() {
        int latest = 0;
        for (List<PlacedBlock> dayBlocks : blocksByDay.values()) {
            for (PlacedBlock block : dayBlocks) {
                latest = Math.max(latest, block.getEndMinutes());
            }
        }
        return latest;
    }

    @Override
    public String toString() {
        return "Horario " + blocksByDay;
    }
}
