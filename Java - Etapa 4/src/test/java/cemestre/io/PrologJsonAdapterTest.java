package cemestre.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cemestre.model.Combination;
import cemestre.model.CombinationSet;
import cemestre.model.CourseRef;
import cemestre.model.CourseSelection;
import cemestre.model.Day;
import cemestre.model.RejectedCombination;
import cemestre.model.ValidCombination;
import cemestre.model.reason.AreaReason;
import cemestre.model.reason.BalanceReason;
import cemestre.model.reason.IncompatibilityReason;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PrologJsonAdapterTest {
    private final CombinationSource source = new PrologJsonAdapter();

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(PrologJsonAdapterTest.class.getResource("/" + name).toURI());
    }

    private String loadError(String name) throws URISyntaxException {
        DataLoadException e = assertThrows(DataLoadException.class, () -> source.load(resource(name)));
        return e.getMessage();
    }

    //  carga correcta

    @Test
    void loadsAllCombinationsInFileOrder() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        assertEquals(3, set.size());
        assertEquals(1, set.getValidCount());
        assertEquals(2, set.getRejectedCount());
        assertEquals(1, set.getCombinations().get(0).getId());
        assertEquals(2, set.getCombinations().get(1).getId());
        assertEquals(3, set.getCombinations().get(2).getId());
    }

    @Test
    void validAndRejectedAreDifferentSubclasses() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        assertInstanceOf(RejectedCombination.class, set.getCombinations().get(0));
        assertInstanceOf(ValidCombination.class, set.getCombinations().get(1));
        assertFalse(set.getCombinations().get(0).isValid());
        assertTrue(set.getCombinations().get(1).isValid());
    }

    @Test
    void loadsCoursesWithTextIdsAndBlocksInMinutes() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        Combination first = set.getCombinations().get(0);
        assertEquals(4, first.getSelections().size());
        CourseSelection course = first.getSelections().get(0);
        assertEquals("CI3203", course.getCourseCode());
        assertEquals("1", course.getGroupId());
        assertEquals(2, course.getCredits());
        assertEquals("Ingenieria en Computadores", course.getCareer());
        assertEquals("Humanistica", course.getArea());
        assertEquals(1, course.getBlocks().size());
        assertEquals(Day.MON, course.getBlocks().get(0).getDay());
        assertEquals(450, course.getBlocks().get(0).getStartMinutes());
        assertEquals(680, course.getBlocks().get(0).getEndMinutes());
    }

    @Test
    void zeroCreditCourseAndNonNumericGroupLoad() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        CourseSelection course = set.getCombinations().get(1).getSelections().get(0);
        assertEquals("SE1100", course.getCourseCode());
        assertEquals("SE1101_1", course.getGroupId());
        assertEquals(0, course.getCredits());
        assertEquals(0, set.getCombinations().get(1).getTotalCredits());
    }

    @Test
    void createsTheRightReasonSubclassPerRule() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        RejectedCombination incompatible = (RejectedCombination) set.getCombinations().get(0);
        RejectedCombination balance = (RejectedCombination) set.getCombinations().get(2);

        IncompatibilityReason inc = assertInstanceOf(IncompatibilityReason.class, incompatible.getReason());
        assertTrue(inc.describe().contains("CI3203") && inc.describe().contains("PI2609"));
        assertEquals("incompatibility", inc.getRule());

        BalanceReason bal = assertInstanceOf(BalanceReason.class, balance.getReason());
        assertEquals("Ingenieria Fisica", bal.getCareer());
        assertEquals(3, bal.getCareerCredits());
        assertEquals(3, bal.getTotalCredits());
        assertEquals(70.0, bal.getMaxPercentage());
    }

    @Test
    void loadsAreaReason() throws Exception {
        CombinationSet set = source.load(resource("results_area.json"));
        RejectedCombination rejected = (RejectedCombination) set.getCombinations().get(0);
        AreaReason area = assertInstanceOf(AreaReason.class, rejected.getReason());
        assertEquals("Ciencias naturales", area.getArea());
        assertEquals(2, area.getCount());
        assertEquals(1, area.getMaxCourses());
    }

    @Test
    void polymorphicDescribeWorksThroughBaseType() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        for (Combination combination : set.getCombinations()) {
            if (combination instanceof RejectedCombination) {
                String text = ((RejectedCombination) combination).getReason().describe();
                assertFalse(text.isEmpty());
            }
        }
    }

    @Test
    void loadsRemovableCourses() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        RejectedCombination first = (RejectedCombination) set.getCombinations().get(0);
        assertEquals(2, first.getRemovable().size());
        assertTrue(first.getRemovable().contains(new CourseRef("CI3203", "1")));
        assertTrue(first.getRemovable().contains(new CourseRef("PI2609", "1")));
        RejectedCombination balance = (RejectedCombination) set.getCombinations().get(2);
        assertTrue(balance.getRemovable().isEmpty());
    }

    @Test
    void loadsPolicyAsInformation() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        assertEquals(2, set.getPolicy().getMaxCoursesPerArea());
        assertEquals(70.0, set.getPolicy().getMaxPercentages().get("Ingenieria Fisica"));
        assertEquals(4, set.getPolicy().getIncompatiblePairs().size());
        assertEquals(java.util.List.of("balance", "area", "incompatibility"), set.getPolicy().getRuleOrder());
    }

    @Test
    void emptyResultsFileLoadsWithZeroCombinations() throws Exception {
        CombinationSet set = source.load(resource("results_empty.json"));
        assertEquals(0, set.size());
    }

    @Test
    void modelCollectionsCannotBeModifiedFromOutside() throws Exception {
        CombinationSet set = source.load(resource("results_small.json"));
        assertThrows(UnsupportedOperationException.class, () -> set.getCombinations().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> set.getCombinations().get(0).getSelections().clear());
    }

    @Test
    void warnsWhenTotalCreditsDoesNotMatch() throws Exception {
        CombinationSet set = source.load(resource("results_total_mismatch.json"));
        assertEquals(1, set.getWarnings().size());
        assertTrue(set.getWarnings().get(0).contains("totalCredits=99"));
    }

    @Test
    void cleanFileHasNoWarnings() throws Exception {
        assertTrue(source.load(resource("results_small.json")).getWarnings().isEmpty());
    }

    // prueba de errores

    @Test
    void missingFileGivesClearError() {
        DataLoadException e = assertThrows(DataLoadException.class,
                () -> source.load(Path.of("no_existe_este_archivo.json")));
        assertTrue(e.getMessage().contains("no existe"));
    }

    @Test
    void truncatedJsonGivesClearError() throws Exception {
        assertTrue(loadError("results_truncated.json").contains("JSON invalido"));
    }

    @Test
    void countMismatchIsDetected() throws Exception {
        assertTrue(loadError("results_bad_count.json").contains("combinationCount"));
    }

    @Test
    void compliantCountMismatchIsDetected() throws Exception {
        assertTrue(loadError("results_bad_compliant.json").contains("compliantCount"));
    }

    @Test
    void unknownRuleIsNotIgnored() throws Exception {
        String message = loadError("results_unknown_rule.json");
        assertTrue(message.contains("foo") && message.contains("regla desconocida"), message);
    }

    @Test
    void validWithReasonIsInconsistent() throws Exception {
        assertTrue(loadError("results_valid_with_reason.json").contains("complies=true"));
    }

    @Test
    void rejectedWithoutReasonIsInconsistent() throws Exception {
        assertTrue(loadError("results_rejected_without_reason.json").contains("complies=false"));
    }

    @Test
    void missingAreaNamesTheExactPath() throws Exception {
        String message = loadError("results_missing_area.json");
        assertTrue(message.contains("area") && message.contains("results[0].selections[0]"), message);
    }

    @Test
    void badTimeAndBadDayAreReportedWithPath() throws Exception {
        String time = loadError("results_bad_time.json");
        assertTrue(time.contains("9999") && time.contains("results[0].selections[0].blocks[0]"), time);
        assertTrue(loadError("results_bad_day.json").contains("DOM"));
    }

    @Test
    void duplicateIdsAreRejected() throws Exception {
        assertTrue(loadError("results_duplicate_id.json").contains("repetido"));
    }

    @Test
    void wrongFieldTypeIsReported() throws Exception {
        String message = loadError("results_credits_text.json");
        assertTrue(message.contains("credits") && message.contains("entero"), message);
    }
}
