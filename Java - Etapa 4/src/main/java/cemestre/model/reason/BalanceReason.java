package cemestre.model.reason;

/** Regla 1: una carrera supero su porcentaje maximo de los creditos de la combinacion. */
public final class BalanceReason extends RejectionReason {
    private final String career;
    private final int careerCredits;
    private final int totalCredits;
    private final double maxPercentage;

    public BalanceReason(String message, String career, int careerCredits, int totalCredits, double maxPercentage) {
        super("balance", message);
        this.career = career;
        this.careerCredits = careerCredits;
        this.totalCredits = totalCredits;
        this.maxPercentage = maxPercentage;
    }

    public String getCareer() {
        return career;
    }

    public int getCareerCredits() {
        return careerCredits;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public double getMaxPercentage() {
        return maxPercentage;
    }

    @Override
    public String describe() {
        return "Balance: " + career + " tiene " + careerCredits + " de " + totalCredits
                + " creditos (maximo " + trim(maxPercentage) + " %)";
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
