package cemestre.model;

public enum Day {
    MON("LUN", "Lunes"),
    TUE("MAR", "Martes"),
    WED("MIE", "Miercoles"),
    THU("JUE", "Jueves"),
    FRI("VIE", "Viernes"),
    SAT("SAB", "Sabado");

    private final String code;
    private final String spanishName;

    Day(String code, String spanishName) {
        this.code = code;
        this.spanishName = spanishName;
    }

    public String getCode() {
        return code;
    }

    public String getSpanishName() {
        return spanishName;
    }

    /** tira IllegalArgumentException si el codigo no es LUN, MAR, MIE, JUE, VIE o SAB */
    public static Day fromCode(String code) {
        for (Day day : values()) {
            if (day.code.equals(code)) {
                return day;
            }
        }
        throw new IllegalArgumentException("dia desconocido: '" + code + "' (se esperaba LUN, MAR, MIE, JUE, VIE o SAB)");
    }
}
