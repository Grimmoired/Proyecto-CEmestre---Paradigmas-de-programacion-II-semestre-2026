package cemestre.model;

import java.util.List;
import java.util.Objects;

/**
 * La jerarquia es Combination -> ValidCombination / RejectedCombination (la rechazada es la unica que tiene razon).
 * Ahora mismo este script solo se usa para pruebas, al encargado del cuartil 2 le toca terminarlo :p
 */
public abstract class Combination {
    private final int id;
    private final List<CourseSelection> selections;
    private final int totalCredits;

    protected Combination(int id, List<CourseSelection> selections, int totalCredits) {
        this.id = id;
        this.selections = List.copyOf(Objects.requireNonNull(selections, "selections"));
        this.totalCredits = totalCredits;
    }

    public int getId() {
        return id;
    }
    public List<CourseSelection> getSelections() {
        return selections;
    }
    public int getTotalCredits() {
        return totalCredits;
    }
    public abstract boolean isValid();
    public abstract String getStatusLabel();

    @Override
    public String toString() {
        return "Combinacion " + id + " (" + getStatusLabel() + ", " + totalCredits + " creditos)";
    }
}
