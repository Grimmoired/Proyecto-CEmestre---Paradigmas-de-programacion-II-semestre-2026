package cemestre.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModelDependenciesTest {
    private static final Path MODEL_SOURCES = Path.of("src", "main", "java", "cemestre", "model");

    private static List<String> problems() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MODEL_SOURCES)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    boolean forbiddenImport = trimmed.startsWith("import ")
                            && (trimmed.contains("javax.swing") || trimmed.contains("java.awt")
                                || (trimmed.contains("cemestre.") && !trimmed.contains("cemestre.model")));
                    if (forbiddenImport || trimmed.contains("getInstance")) {
                        problems.add(file.getFileName() + ": " + trimmed);
                    }
                }
            }
        }
        return problems;
    }

    @Test
    void modelOnlyDependsOnItselfAndTheJdk() throws IOException {
        assertTrue(Files.isDirectory(MODEL_SOURCES), "no se encontro " + MODEL_SOURCES.toAbsolutePath());
        List<String> found = problems();
        assertTrue(found.isEmpty(), "dependencias no permitidas en model: " + found);
    }
}
