package cemestre.model;

import static cemestre.model.TestCourses.CE;
import static cemestre.model.TestCourses.FI;
import static cemestre.model.TestCourses.block;
import static cemestre.model.TestCourses.course;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cemestre.model.reason.IncompatibilityReason;
import cemestre.model.reason.RejectionReason;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CombinationTest {
    private final CourseSelection ci = course("CI3203", 2, CE, block("LUN", "0730", "0920"));
    private final CourseSelection pi = course("PI2609", 2, CE, block("MAR", "0730", "0920"));
    private final CourseSelection qu = course("QU1107", 3, FI, block("MIE", "0730", "0920"));
    private final RejectionReason incompatibility =
            new IncompatibilityReason("CI3203 y PI2609 son incompatibles", "CI3203", "PI2609");

    private Combination valid() {
        return new CombinationBuilder(1).addCourse(ci).addCourse(qu).build();
    }

    private Combination rejected() {
        return new CombinationBuilder(2).addCourse(ci).addCourse(pi)
                .rejectedBecause(incompatibility)
                .addRemovable(new CourseRef("CI3203", "1"))
                .addRemovable(new CourseRef("PI2609", "1"))
                .build();
    }

    @Test
    void validAndRejectedAnswerDifferentlyThroughTheBaseType() {
        List<Combination> all = List.of(valid(), rejected());

        Combination v = all.get(0);
        assertTrue(v.isValid());
        assertEquals("Valida", v.getStatusLabel());
        assertEquals("Cumple todas las reglas de politica", v.getStatusDetail());
        assertTrue(v.getRemovable().isEmpty());

        Combination r = all.get(1);
        assertFalse(r.isValid());
        assertEquals("Rechazada", r.getStatusLabel());
        assertEquals(incompatibility.describe(), r.getStatusDetail());
        assertEquals(2, r.getRemovable().size());
    }

    @Test
    void builderCreatesTheRightSubclass() {
        assertInstanceOf(ValidCombination.class, valid());
        assertInstanceOf(RejectedCombination.class, rejected());
    }

    @Test
    void builderRejectsRemovableOnAValidCombination() {
        CombinationBuilder builder = new CombinationBuilder(3).addCourse(ci).addRemovable(new CourseRef("CI3203", "1"));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void totalCreditsDefaultsToTheSumOfCourses() {
        assertEquals(5, valid().getTotalCredits());
    }

    @Test
    void totalCreditsFromPrologIsKeptAsIs() {
        Combination c = new CombinationBuilder(4).addCourse(ci).totalCredits(99).build();
        assertEquals(99, c.getTotalCredits());
    }

    @Test
    void creditsByCareerAreSummed() {
        Combination c = new CombinationBuilder(5).addCourse(ci).addCourse(pi).addCourse(qu).build();
        assertEquals(Map.of(CE, 4, FI, 3), c.getCreditsByCareer());
    }

    @Test
    void changingTheOriginalListDoesNotChangeTheCombination() {
        List<CourseSelection> courses = new ArrayList<>(List.of(ci, pi));
        Combination c = new ValidCombination(6, courses, 4);
        courses.add(qu);
        assertEquals(2, c.getSelections().size());
    }

    @Test
    void collectionsCannotBeModifiedFromOutside() {
        Combination r = rejected();
        assertThrows(UnsupportedOperationException.class, () -> r.getSelections().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.getRemovable().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.getCreditsByCareer().clear());
        assertThrows(UnsupportedOperationException.class, () -> valid().getRemovable().add(new CourseRef("X", "1")));
    }

    @Test
    void rejectedCombinationNeedsAReason() {
        assertThrows(NullPointerException.class,
                () -> new RejectedCombination(7, List.of(ci), 2, null, List.of()));
    }

    @Test
    void courseBlocksAreCopiedDefensively() {
        List<TimeBlock> blocks = new ArrayList<>(List.of(block("LUN", "0730", "0920")));
        CourseSelection course = new CourseSelection("A", "Curso A", "1", 3, CE, "Matematicas", blocks);
        blocks.add(block("MAR", "0730", "0920"));
        assertEquals(1, course.getBlocks().size());
    }

    @Test
    void combinationsAreEqualByTypeAndId() {
        Combination a = new ValidCombination(10, List.of(ci), 2);
        Combination sameId = new ValidCombination(10, List.of(ci), 2);
        Combination otherId = new ValidCombination(11, List.of(ci), 2);
        Combination otherType = new RejectedCombination(10, List.of(ci), 2, incompatibility, List.of());
        assertEquals(a, sameId);
        assertEquals(a.hashCode(), sameId.hashCode());
        assertNotEquals(a, otherId);
        assertNotEquals(a, otherType);
    }

    @Test
    void courseSelectionsAreEqualByValue() {
        CourseSelection copy = course("CI3203", 2, CE, block("LUN", "0730", "0920"));
        assertEquals(ci, copy);
        assertEquals(ci.hashCode(), copy.hashCode());
        assertNotEquals(ci, pi);
    }

    @Test
    void toStringIsReadable() {
        assertEquals("Combinacion 1 (Valida, 5 creditos)", valid().toString());
        assertEquals("Combinacion 2 (Rechazada, 4 creditos)", rejected().toString());
    }
}
