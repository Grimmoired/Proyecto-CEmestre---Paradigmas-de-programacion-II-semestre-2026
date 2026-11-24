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
}
