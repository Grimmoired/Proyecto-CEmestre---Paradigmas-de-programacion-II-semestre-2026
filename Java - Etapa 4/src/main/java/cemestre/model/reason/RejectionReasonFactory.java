package cemestre.model.reason;

import java.util.List;

public final class RejectionReasonFactory {
    private static final List<String> KNOWN_RULES = List.of("balance", "area", "incompatibility");

    public List<String> getKnownRules() {
        return KNOWN_RULES;
    }

    public boolean isKnownRule(String rule) {
        return KNOWN_RULES.contains(rule);
    }

    public RejectionReason create(String rule, String message, ReasonData data) {
        if (rule == null) {
            throw new IllegalArgumentException("la razon no trae regla");
        }
        switch (rule) {
            case "balance":
                return new BalanceReason(message,
                        data.text("career"),
                        data.integer("careerCredits"),
                        data.integer("totalCredits"),
                        data.number("maxPercentage"));
            case "area":
                return new AreaReason(message,
                        data.text("area"),
                        data.integer("count"),
                        data.integer("maxCourses"));
            case "incompatibility":
                return new IncompatibilityReason(message,
                        data.text("courseA"),
                        data.text("courseB"));
            default:
                throw new IllegalArgumentException("regla desconocida '" + rule
                        + "' (reglas conocidas: " + String.join(", ", KNOWN_RULES) + ")");
        }
    }
}
