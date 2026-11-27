package cemestre.model;

import java.util.List;

public final class ValidCombination extends Combination {
    public ValidCombination(int id, List<CourseSelection> selections, int totalCredits) {
        super(id, selections, totalCredits);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getStatusLabel() {
        return "Valida";
    }

    @Override
    public String getStatusDetail() {
        return "Cumple todas las reglas de politica";
    }

    @Override
    public List<CourseRef> getRemovable() {
        return List.of();
    }
}
