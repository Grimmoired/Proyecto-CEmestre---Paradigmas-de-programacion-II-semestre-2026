package cemestre.io;

import cemestre.model.Combination;
import cemestre.model.CombinationSet;
import cemestre.model.CourseRef;
import cemestre.model.CourseSelection;
import cemestre.model.Day;
import cemestre.model.IncompatiblePair;
import cemestre.model.PolicyInfo;
import cemestre.model.RejectedCombination;
import cemestre.model.TimeBlock;
import cemestre.model.ValidCombination;
import cemestre.model.reason.AreaReason;
import cemestre.model.reason.BalanceReason;
import cemestre.model.reason.IncompatibilityReason;
import cemestre.model.reason.RejectionReason;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PrologJsonAdapter implements CombinationSource {

    @Override
    public CombinationSet load(Path file) throws DataLoadException {
        return adapt(PrologResultLoader.loadRoot(file));
    }

    CombinationSet adapt(JsonObject root) throws DataLoadException {
        List<String> warnings = new ArrayList<>();
        PolicyInfo policy = readPolicy(root.object("policy"));
        int declaredCount = root.integer("combinationCount");
        int declaredCompliant = root.integer("compliantCount");

        List<Combination> combinations = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (JsonObject result : root.objects("results")) {
            Combination combination = readCombination(result, warnings);
            if (!ids.add(combination.getId())) {
                throw new DataLoadException("el id " + combination.getId() + " esta repetido (" + result.getPath() + ")");
            }
            combinations.add(combination);
        }

        if (declaredCount != combinations.size()) {
            throw new DataLoadException("combinationCount dice " + declaredCount + " pero el archivo trae "
                    + combinations.size() + " resultados (archivo truncado o incompleto?)");
        }
        int valid = 0;
        for (Combination combination : combinations) {
            if (combination.isValid()) {
                valid++;
            }
        }
        if (declaredCompliant != valid) {
            throw new DataLoadException("compliantCount dice " + declaredCompliant + " pero hay " + valid
                    + " resultados con complies=true");
        }
        return new CombinationSet(policy, combinations, warnings);
    }

    // politica

    private PolicyInfo readPolicy(JsonObject policy) throws DataLoadException {
        Map<String, Double> maxPercentages = new LinkedHashMap<>();
        for (JsonObject entry : policy.objects("maxPercentages")) {
            maxPercentages.put(entry.string("career"), entry.number("maxPercentage"));
        }
        List<IncompatiblePair> pairs = new ArrayList<>();
        for (JsonObject pair : policy.objects("incompatiblePairs")) {
            pairs.add(new IncompatiblePair(pair.string("courseA"), pair.string("courseB")));
        }
        return new PolicyInfo(maxPercentages, policy.integer("maxCoursesPerArea"), pairs, policy.strings("ruleOrder"));
    }

    // combinaciones

    private Combination readCombination(JsonObject result, List<String> warnings) throws DataLoadException {
        int id = result.integer("id");
        boolean complies = result.bool("complies");
        int totalCredits = result.integer("totalCredits");

        List<CourseSelection> selections = new ArrayList<>();
        int creditSum = 0;
        for (JsonObject selection : result.objects("selections")) {
            CourseSelection course = readSelection(selection);
            selections.add(course);
            creditSum += course.getCredits();
        }
        if (creditSum != totalCredits) {
            warnings.add("combinacion " + id + ": totalCredits=" + totalCredits + " pero los cursos suman " + creditSum);
        }

        if (complies) {
            if (!result.isNull("reason")) {
                throw new DataLoadException("la combinacion " + id + " dice complies=true pero trae una razon de rechazo ("
                        + result.pathOf("reason") + ")");
            }
            return new ValidCombination(id, selections, totalCredits);
        }

        if (result.isNull("reason")) {
            throw new DataLoadException("la combinacion " + id + " dice complies=false pero su razon es null ("
                    + result.pathOf("reason") + ")");
        }
        RejectionReason reason = readReason(result.object("reason"));
        List<CourseRef> removable = new ArrayList<>();
        for (JsonObject ref : result.objects("removable")) {
            CourseRef courseRef = new CourseRef(ref.string("courseCode"), ref.string("groupId"));
            if (!containsCourse(selections, courseRef)) {
                warnings.add("combinacion " + id + ": el curso a quitar " + courseRef + " no esta entre sus cursos");
            }
            removable.add(courseRef);
        }
        return new RejectedCombination(id, selections, totalCredits, reason, removable);
    }

    private static boolean containsCourse(List<CourseSelection> selections, CourseRef ref) {
        for (CourseSelection selection : selections) {
            if (selection.getCourseCode().equals(ref.getCourseCode()) && selection.getGroupId().equals(ref.getGroupId())) {
                return true;
            }
        }
        return false;
    }

    private RejectionReason readReason(JsonObject reason) throws DataLoadException {
        String rule = reason.string("rule");
        String message = reason.string("message");
        switch (rule) {
            case "balance":
                return new BalanceReason(message, reason.string("career"), reason.integer("careerCredits"),
                        reason.integer("totalCredits"), reason.number("maxPercentage"));
            case "area":
                return new AreaReason(message, reason.string("area"), reason.integer("count"),
                        reason.integer("maxCourses"));
            case "incompatibility":
                return new IncompatibilityReason(message, reason.string("courseA"), reason.string("courseB"));
            default:
                throw new DataLoadException("regla desconocida '" + rule + "' en " + reason.pathOf("rule")
                        + " (reglas conocidas: balance, area, incompatibility)");
        }
    }

    // cursos

    private CourseSelection readSelection(JsonObject selection) throws DataLoadException {
        int credits = selection.integer("credits");
        if (credits < 0) {
            throw new DataLoadException("creditos negativos en " + selection.pathOf("credits"));
        }
        List<TimeBlock> blocks = new ArrayList<>();
        for (JsonObject block : selection.objects("blocks")) {
            blocks.add(readBlock(block));
        }
        return new CourseSelection(selection.string("courseCode"), selection.string("courseName"),
                selection.string("groupId"), credits, selection.string("career"), selection.string("area"), blocks);
    }

    private TimeBlock readBlock(JsonObject block) throws DataLoadException {
        try {
            Day day = Day.fromCode(block.string("day"));
            int start = TimeBlock.minutesOf(block.string("startTime"));
            int end = TimeBlock.minutesOf(block.string("endTime"));
            return new TimeBlock(day, start, end);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException("bloque invalido en " + block.getPath() + ": " + e.getMessage(), e);
        }
    }
}
