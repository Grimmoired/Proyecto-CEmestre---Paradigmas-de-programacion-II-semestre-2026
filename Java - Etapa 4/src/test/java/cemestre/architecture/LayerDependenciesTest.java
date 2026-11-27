package cemestre.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LayerDependenciesTest {
    private static final Path SOURCES = Path.of("src", "main", "java", "cemestre");

    private static List<String> problems(String packageName, List<String> allowed) throws IOException {
        Path folder = SOURCES.resolve(packageName);
        assertTrue(Files.isDirectory(folder), "no se encontro " + folder.toAbsolutePath());
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.contains("getInstance")) {
                        problems.add(file.getFileName() + ": " + trimmed);
                    }
                    if (!trimmed.startsWith("import ")) {
                        continue;
                    }
                    if (trimmed.contains("javax.swing") || trimmed.contains("java.awt")) {
                        problems.add(file.getFileName() + ": " + trimmed);
                    } else if (trimmed.contains("cemestre.") && !isAllowed(trimmed, allowed)) {
                        problems.add(file.getFileName() + ": " + trimmed);
                    }
                }
            }
        }
        return problems;
    }

    private static boolean isAllowed(String importLine, List<String> allowed) {
        for (String prefix : allowed) {
            if (importLine.contains(prefix + ".")) {
                return true;
            }
        }
        return false;
    }

    @Test
    void sortingOnlyUsesTheModel() throws IOException {
        List<String> found = problems("sorting", List.of("cemestre.model", "cemestre.sorting"));
        assertTrue(found.isEmpty(), "dependencias no permitidas en sorting: " + found);
    }

    @Test
    void observerDependsOnNothingElse() throws IOException {
        List<String> found = problems("observer", List.of("cemestre.observer"));
        assertTrue(found.isEmpty(), "dependencias no permitidas en observer: " + found);
    }

    @Test
    void stateDoesNotKnowTheViews() throws IOException {
        List<String> found = problems("state",
                List.of("cemestre.model", "cemestre.sorting", "cemestre.observer", "cemestre.state"));
        assertTrue(found.isEmpty(), "dependencias no permitidas en state: " + found);
    }
}
