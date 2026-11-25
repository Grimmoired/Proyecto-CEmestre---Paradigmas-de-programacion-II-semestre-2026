package cemestre.model.reason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RejectionReasonFactoryTest {
    private final RejectionReasonFactory factory = new RejectionReasonFactory();

    private static ReasonData data(Map<String, Object> fields) {
        return new ReasonData() {
            private Object get(String field) {
                if (!fields.containsKey(field)) {
                    throw new IllegalArgumentException("falta el campo '" + field + "'");
                }
                return fields.get(field);
            }

            @Override
            public String text(String field) {
                return (String) get(field);
            }

            @Override
            public int integer(String field) {
                return (Integer) get(field);
            }

            @Override
            public double number(String field) {
                return ((Number) get(field)).doubleValue();
            }
        };
    }

    @Test
    void createsBalanceReason() {
        RejectionReason reason = factory.create("balance", "msg", data(Map.of(
                "career", "Ingenieria Fisica", "careerCredits", 8, "totalCredits", 11, "maxPercentage", 70)));
        BalanceReason balance = assertInstanceOf(BalanceReason.class, reason);
        assertEquals("balance", balance.getRule());
        assertEquals("Ingenieria Fisica", balance.getCareer());
        assertEquals(8, balance.getCareerCredits());
        assertEquals(11, balance.getTotalCredits());
        assertEquals(70.0, balance.getMaxPercentage());
    }

    @Test
    void createsAreaReason() {
        RejectionReason reason = factory.create("area", "msg", data(Map.of(
                "area", "Matematicas", "count", 3, "maxCourses", 2)));
        AreaReason area = assertInstanceOf(AreaReason.class, reason);
        assertEquals("Matematicas", area.getArea());
        assertEquals(3, area.getCount());
        assertEquals(2, area.getMaxCourses());
    }

    @Test
    void createsIncompatibilityReason() {
        RejectionReason reason = factory.create("incompatibility", "msg", data(Map.of(
                "courseA", "CI3203", "courseB", "PI2609")));
        IncompatibilityReason inc = assertInstanceOf(IncompatibilityReason.class, reason);
        assertEquals("CI3203", inc.getCourseA());
        assertEquals("PI2609", inc.getCourseB());
    }

    @Test
    void unknownRuleGivesClearError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> factory.create("foo", "msg", data(Map.of())));
        assertTrue(e.getMessage().contains("regla desconocida") && e.getMessage().contains("foo"), e.getMessage());
    }

    @Test
    void missingFieldGivesClearError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> factory.create("area", "msg", data(Map.of("area", "Matematicas", "count", 3))));
        assertTrue(e.getMessage().contains("maxCourses"), e.getMessage());
    }

    @Test
    void knowsTheSameRulesAsProlog() {
        assertEquals(List.of("balance", "area", "incompatibility"), factory.getKnownRules());
        assertTrue(factory.isKnownRule("area"));
        assertTrue(!factory.isKnownRule("Area"));
    }

    @Test
    void describeIsPolymorphicThroughTheBaseType() {
        List<RejectionReason> reasons = List.of(
                new BalanceReason("m", "Ingenieria Fisica", 8, 11, 70.0),
                new AreaReason("m", "Matematicas", 3, 2),
                new IncompatibilityReason("m", "CI3203", "PI2609"));
        assertEquals("Balance: Ingenieria Fisica tiene 8 de 11 creditos (maximo 70 %)", reasons.get(0).describe());
        assertEquals("Area: 3 cursos de Matematicas (maximo 2)", reasons.get(1).describe());
        assertEquals("Incompatibilidad: CI3203 y PI2609 no pueden llevarse juntos", reasons.get(2).describe());
    }

    @Test
    void decimalPercentageIsShownWithDecimals() {
        assertEquals("Balance: X tiene 1 de 2 creditos (maximo 62.5 %)",
                new BalanceReason("m", "X", 1, 2, 62.5).describe());
    }

    @Test
    void reasonsAreEqualByValue() {
        assertEquals(new AreaReason("m", "Matematicas", 3, 2), new AreaReason("m", "Matematicas", 3, 2));
        assertEquals(new AreaReason("m", "Matematicas", 3, 2).hashCode(),
                new AreaReason("m", "Matematicas", 3, 2).hashCode());
        assertNotEquals(new AreaReason("m", "Matematicas", 3, 2), new AreaReason("m", "Matematicas", 4, 2));
        assertNotEquals(new IncompatibilityReason("m", "A", "B"), new IncompatibilityReason("m", "B", "A"));
    }
}
