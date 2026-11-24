package cemestre.model;

import java.util.Objects;

public final class TimeBlock {
    private final Day day;
    private final int startMinutes;
    private final int endMinutes;

    /** Manda IllegalArgumentException si el rango no es valido (0 <= inicio < fin <= 1440) */
    public TimeBlock(Day day, int startMinutes, int endMinutes) {
        this.day = Objects.requireNonNull(day, "day");
        if (startMinutes < 0 || endMinutes > 24 * 60 || startMinutes >= endMinutes) {
            throw new IllegalArgumentException("rango de horas invalido: " + startMinutes + " a " + endMinutes + " minutos");
        }
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
    }

    /**
     * Convierte el texto de hora de las etapas anteriores a minutos, tira IllegalArgumentException si no son 4 digitos o la hora no es valida.
     */
    public static int minutesOf(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) {
            throw new IllegalArgumentException("hora invalida: '" + hhmm + "' (se esperaban 4 digitos, por ejemplo 0730)");
        }
        for (int i = 0; i < 4; i++) {
            if (!Character.isDigit(hhmm.charAt(i))) {
                throw new IllegalArgumentException("hora invalida: '" + hhmm + "' (se esperaban 4 digitos, por ejemplo 0730)");
            }
        }
        int hours = Integer.parseInt(hhmm.substring(0, 2));
        int minutes = Integer.parseInt(hhmm.substring(2, 4));
        if (hours > 23 || minutes > 59) {
            throw new IllegalArgumentException("hora inexistente: '" + hhmm + "'");
        }
        return hours * 60 + minutes;
    }

    public Day getDay() {
        return day;
    }
    public int getStartMinutes() {
        return startMinutes;
    }
    public int getEndMinutes() {
        return endMinutes;
    }
    public int getDurationMinutes() {
        return endMinutes - startMinutes;
    }

    /** Vuelve a convertir a formato de horas legible */
    public static String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimeBlock)) {
            return false;
        }
        TimeBlock that = (TimeBlock) other;
        return day == that.day && startMinutes == that.startMinutes && endMinutes == that.endMinutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, startMinutes, endMinutes);
    }

    @Override
    public String toString() {
        return day.getCode() + " " + format(startMinutes) + "-" + format(endMinutes);
    }
}
