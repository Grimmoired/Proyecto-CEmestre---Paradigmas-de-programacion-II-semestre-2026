package cemestre.model.reason;

import java.util.Objects;

/**
 * Primera regla de politica que viola una combinacion rechazada.
 * Jerarquia: BalanceReason, AreaReason, IncompatibilityReason (cada una con sus propios datos).
 * Se usa siempre a traves de este tipo base (polimorfismo): reason.describe().
 */
public abstract class RejectionReason {
    private final String rule;
    private final String message;

    protected RejectionReason(String rule, String message) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.message = Objects.requireNonNull(message, "message");
    }

    /** Nombre de la regla segun Prolog: balance, area o incompatibility. */
    public String getRule() {
        return rule;
    }

    /** Mensaje ya redactado por Prolog (sirve como respaldo). */
    public String getMessage() {
        return message;
    }

    /** Descripcion para la interfaz, armada con los datos propios de cada subclase. */
    public abstract String describe();

    @Override
    public String toString() {
        return rule + ": " + message;
    }
}
