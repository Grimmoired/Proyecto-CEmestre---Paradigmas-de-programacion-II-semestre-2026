package cemestre.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewIndependenceTest {
    private static final Path VIEWS = Path.of("src", "main", "java", "cemestre", "view");
    private static final List<String> OBSERVER_VIEWS =
            List.of("CombinationListPanel", "ScheduleView", "CreditSummaryView");

    private static String source(String className) throws IOException {
        Path file = VIEWS.resolve(className + ".java");
        assertTrue(Files.exists(file), "no se encontro " + file.toAbsolutePath());
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void observerViewsDoNotKnowEachOther() throws IOException {
        List<String> problems = new ArrayList<>();
        for (String view : OBSERVER_VIEWS) {
            String code = source(view);
            for (String other : OBSERVER_VIEWS) {
                if (!other.equals(view) && code.contains(other)) {
                    problems.add(view + " menciona a " + other);
                }
            }
        }
        assertTrue(problems.isEmpty(), "las vistas se conocen entre si: " + problems);
    }

    @Test
    void everyObserverViewRegistersItselfInAppState() throws IOException {
        for (String view : OBSERVER_VIEWS) {
            String code = source(view);
            assertTrue(code.contains("implements StateObserver"), view + " no implementa StateObserver");
            assertTrue(code.contains("state.addObserver(this)"), view + " no se registra en AppState");
        }
    }
}