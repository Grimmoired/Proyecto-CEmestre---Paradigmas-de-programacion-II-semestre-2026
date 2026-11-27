package cemestre.view;

import cemestre.model.Combination;
import cemestre.model.Day;
import cemestre.model.PlacedBlock;
import cemestre.model.TimeBlock;
import cemestre.model.WeeklySchedule;
import cemestre.observer.StateEvent;
import cemestre.observer.StateObserver;
import cemestre.state.AppState;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScheduleView extends JPanel implements StateObserver {
    private static final int PADDING = 8;
    private static final int HOUR_LABEL_WIDTH = 46;
    private static final int TITLE_HEIGHT = 24;
    private static final int DAY_HEADER_HEIGHT = 22;
    private static final int DEFAULT_START = 7 * 60;
    private static final int DEFAULT_END = 21 * 60;

    private static final Color GRID = new Color(225, 225, 225);
    private static final Color TEXT = new Color(40, 40, 40);
    private static final Color VALID = new Color(26, 127, 55);
    private static final Color REJECTED = new Color(179, 38, 30);
    private static final Color[] PALETTE = {
            new Color(173, 206, 240), new Color(250, 205, 160), new Color(182, 225, 180), new Color(230, 190, 225),
            new Color(250, 235, 160), new Color(190, 225, 225), new Color(240, 185, 185), new Color(210, 200, 240)
    };

    private final AppState state;

    public ScheduleView(AppState state) {
        this.state = Objects.requireNonNull(state, "state");
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 420));
        ToolTipManager.sharedInstance().registerComponent(this);
        state.addObserver(this);
    }

    @Override
    public void onStateChanged(StateEvent event) {
        if (event == StateEvent.SELECTION_CHANGED || event == StateEvent.DATA_LOADED) {
            repaint();
        }
    }

    private record Grid(List<Day> days, int startMinutes, int endMinutes, int left, int top, int width, int height) {

        double columnWidth() {
            return (double) width / days.size();
        }

        int x(Day day) {
            return left + (int) Math.round(days.indexOf(day) * columnWidth());
        }

        int y(int minutes) {
            return top + (int) Math.round((minutes - startMinutes) * (double) height / (endMinutes - startMinutes));
        }
    }

    private Grid gridFor(WeeklySchedule schedule) {
        List<Day> days = new ArrayList<>(List.of(Day.MON, Day.TUE, Day.WED, Day.THU, Day.FRI));
        if (!schedule.getBlocks(Day.SAT).isEmpty()) {
            days.add(Day.SAT);
        }
        int start = schedule.isEmpty() ? DEFAULT_START : (schedule.getEarliestStartMinutes() / 60) * 60;
        int end = schedule.isEmpty() ? DEFAULT_END : ((schedule.getLatestEndMinutes() + 59) / 60) * 60;

        int left = PADDING + HOUR_LABEL_WIDTH;
        int top = PADDING + TITLE_HEIGHT + DAY_HEADER_HEIGHT;
        int width = Math.max(1, getWidth() - left - PADDING);
        int height = Math.max(1, getHeight() - top - PADDING);
        return new Grid(days, start, end, left, top, width, height);
    }

    private static Rectangle boundsOf(Grid grid, PlacedBlock block) {
        int x = grid.x(block.getDay()) + 2;
        int y = grid.y(block.getStartMinutes()) + 1;
        int width = (int) grid.columnWidth() - 4;
        int height = grid.y(block.getEndMinutes()) - y - 1;
        return new Rectangle(x, y, width, height);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Optional<Combination> selected = state.getSelected();
            if (selected.isPresent()) {
                drawCombination(g2, selected.get());
            } else {
                drawCentered(g2, "Seleccione una combinacion para ver su horario");
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawCombination(Graphics2D g2, Combination combination) {
        WeeklySchedule schedule = combination.getSchedule();
        Grid grid = gridFor(schedule);
        drawTitle(g2, combination);
        drawGrid(g2, grid);
        for (Day day : grid.days()) {
            for (PlacedBlock block : schedule.getBlocks(day)) {
                drawBlock(g2, grid, block, colorOf(combination, block));
            }
        }
        if (schedule.isEmpty()) {
            drawCentered(g2, "Esta combinacion no tiene bloques de horario");
        }
    }

    private void drawTitle(Graphics2D g2, Combination combination) {
        g2.setFont(getFont().deriveFont(Font.BOLD));
        g2.setColor(combination.isValid() ? VALID : REJECTED);
        String title = "Combinacion " + combination.getId() + " - " + combination.getStatusLabel()
                + " (" + combination.getTotalCredits() + " creditos)";
        g2.drawString(title, PADDING, PADDING + g2.getFontMetrics().getAscent());
    }

    private void drawGrid(Graphics2D g2, Grid grid) {
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();

        for (int minutes = grid.startMinutes(); minutes <= grid.endMinutes(); minutes += 60) {
            int y = grid.y(minutes);
            g2.setColor(GRID);
            g2.drawLine(grid.left(), y, grid.left() + grid.width(), y);
            g2.setColor(TEXT);
            g2.drawString(TimeBlock.format(minutes), PADDING, y + fm.getAscent() / 2);
        }

        int headerBaseline = PADDING + TITLE_HEIGHT + fm.getAscent();
        for (Day day : grid.days()) {
            int x = grid.x(day);
            g2.setColor(GRID);
            g2.drawLine(x, grid.top(), x, grid.top() + grid.height());
            g2.setColor(TEXT);
            String name = day.getSpanishName();
            int textX = x + ((int) grid.columnWidth() - fm.stringWidth(name)) / 2;
            g2.drawString(name, textX, headerBaseline);
        }
        g2.setColor(GRID);
        int right = grid.left() + grid.width();
        g2.drawLine(right, grid.top(), right, grid.top() + grid.height());
    }

    private void drawBlock(Graphics2D g2, Grid grid, PlacedBlock block, Color color) {
        Rectangle r = boundsOf(grid, block);
        g2.setColor(color);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);

        String[] lines = {
                block.getCourseCode(),
                block.getCourseName(),
                "Grupo " + block.getGroupId(),
                TimeBlock.format(block.getStartMinutes()) + " - " + TimeBlock.format(block.getEndMinutes())
        };
        Graphics2D text = (Graphics2D) g2.create();
        try {
            text.clipRect(r.x, r.y, r.width, r.height);
            text.setColor(TEXT);
            int y = r.y + 3;
            for (int i = 0; i < lines.length; i++) {
                text.setFont(i == 0 ? getFont().deriveFont(Font.BOLD) : getFont());
                FontMetrics fm = text.getFontMetrics();
                if (y + fm.getHeight() > r.y + r.height) {
                    break;
                }
                text.drawString(fit(lines[i], fm, r.width - 8), r.x + 4, y + fm.getAscent());
                y += fm.getHeight();
            }
        } finally {
            text.dispose();
        }
    }

    private static String fit(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private static Color colorOf(Combination combination, PlacedBlock block) {
        int index = combination.getSelections().indexOf(block.getCourse());
        return PALETTE[Math.max(0, index) % PALETTE.length];
    }

    private void drawCentered(Graphics2D g2, String message) {
        g2.setFont(getFont().deriveFont(Font.ITALIC));
        g2.setColor(Color.GRAY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(message, (getWidth() - fm.stringWidth(message)) / 2, getHeight() / 2);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Optional<Combination> selected = state.getSelected();
        if (selected.isEmpty()) {
            return null;
        }
        WeeklySchedule schedule = selected.get().getSchedule();
        Grid grid = gridFor(schedule);
        for (Day day : grid.days()) {
            for (PlacedBlock block : schedule.getBlocks(day)) {
                if (boundsOf(grid, block).contains(event.getPoint())) {
                    return "<html><b>" + block.getCourseCode() + "</b> " + block.getCourseName()
                            + "<br>Grupo " + block.getGroupId() + " - " + day.getSpanishName() + " "
                            + TimeBlock.format(block.getStartMinutes()) + " a " + TimeBlock.format(block.getEndMinutes())
                            + "<br>" + block.getCareer() + " - " + block.getCourse().getArea() + "</html>";
                }
            }
        }
        return null;
    }
}