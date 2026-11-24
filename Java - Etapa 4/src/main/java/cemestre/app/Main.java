package cemestre.app;

import cemestre.io.CombinationSource;
import cemestre.io.DataLoadException;
import cemestre.io.PrologJsonAdapter;
import cemestre.model.Combination;
import cemestre.model.CombinationSet;
import cemestre.model.RejectedCombination;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Ahorita solo sirve para hacer una prueba de carga de archivos, y ver que los distintos archivos de input de prueba ubicados en test tiren los errores correspondientes */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -jar cemestre.jar <resultados.json>");
            System.exit(1);
        }
        CombinationSource source = new PrologJsonAdapter();
        try {
            CombinationSet set = source.load(Path.of(args[0]));
            printSummary(set);
        } catch (DataLoadException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printSummary(CombinationSet set) {
        System.out.println("Combinaciones cargadas: " + set.size()
                + " (validas: " + set.getValidCount() + ", rechazadas: " + set.getRejectedCount() + ")");
        System.out.println("Orden de reglas de Prolog: " + set.getPolicy().getRuleOrder());

        Map<String, Integer> byRule = new TreeMap<>();
        for (Combination combination : set.getCombinations()) {
            if (combination instanceof RejectedCombination) {
                String rule = ((RejectedCombination) combination).getReason().getRule();
                byRule.merge(rule, 1, Integer::sum);
            }
        }
        System.out.println("Rechazadas por primera regla violada: " + byRule);

        int shown = Math.min(5, set.size());
        for (int i = 0; i < shown; i++) {
            Combination combination = set.getCombinations().get(i);
            String detail = combination instanceof RejectedCombination
                    ? " -> " + ((RejectedCombination) combination).getReason().describe()
                    : "";
            System.out.println("  " + combination + detail);
        }
        for (String warning : set.getWarnings()) {
            System.err.println("ADVERTENCIA: " + warning);
        }
    }
}
