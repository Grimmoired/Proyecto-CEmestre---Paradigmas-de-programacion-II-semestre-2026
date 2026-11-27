package cemestre.view;

import cemestre.model.Combination;
import cemestre.model.CourseRef;
import cemestre.model.CourseSelection;
import cemestre.model.PolicyInfo;
import cemestre.observer.StateEvent;
import cemestre.observer.StateObserver;
import cemestre.state.AppState;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CreditSummaryView extends JPanel implements StateObserver {
    private static final String VALID_COLOR = "#1a7f37";
    private static final String REJECTED_COLOR = "#b3261e";

    private final AppState state;
    private final JEditorPane content = new JEditorPane();

    public CreditSummaryView(AppState state) {
        super(new BorderLayout());
        this.state = Objects.requireNonNull(state, "state");

        content.setContentType("text/html");                                  // interpreta el texto como HTML
        content.setEditable(false);
        content.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true); // usa la misma fuente que el resto
        content.setFont(new JLabel().getFont());
        content.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(new JScrollPane(content), BorderLayout.CENTER);

        state.addObserver(this);
        refresh();
    }

    @Override
    public void onStateChanged(StateEvent event) {
        if (event == StateEvent.SELECTION_CHANGED || event == StateEvent.DATA_LOADED) {
            refresh();
        }
    }

    private void refresh() {
        content.setText(state.getSelected()
                .map(this::describe)
                .orElse("<html><i>Seleccione una combinación de la lista para ver su resumen.</i></html>"));
        content.setCaretPosition(0);   // vuelve al inicio del texto
    }

    private String describe(Combination combination) {
        StringBuilder html = new StringBuilder("<html>");

        String color = combination.isValid() ? VALID_COLOR : REJECTED_COLOR;
        html.append("<b>Combinación ").append(combination.getId()).append("</b> &nbsp; ")
                .append("<b style='color:").append(color).append("'>")
                .append(escape(combination.getStatusLabel().toUpperCase(Locale.ROOT))).append("</b><br>")
                .append(escape(combination.getStatusDetail())).append("<br><br>");

        appendCredits(html, combination);
        appendCourses(html, combination);
        appendSuggestion(html, combination);

        return html.append("</html>").toString();
    }

    private void appendCredits(StringBuilder html, Combination combination) {
        int total = combination.getTotalCredits();
        html.append("<b>Créditos totales:</b> ").append(total).append("<br>")
                .append("<b>Créditos por carrera:</b><ul>");
        for (Map.Entry<String, Integer> entry : combination.getCreditsByCareer().entrySet()) {
            String career = entry.getKey();
            int credits = entry.getValue();
            html.append("<li>").append(escape(career)).append(": ").append(credits);
            if (total > 0) {
                html.append(String.format(Locale.ROOT, " (%.1f %%", credits * 100.0 / total));
                Double max = state.getPolicy().map(PolicyInfo::getMaxPercentages).map(m -> m.get(career)).orElse(null);
                if (max != null) {
                    html.append(String.format(Locale.ROOT, ", máximo %.0f %%", max));
                }
                html.append(")");
            }
            html.append("</li>");
        }
        html.append("</ul>");
    }

    private static void appendCourses(StringBuilder html, Combination combination) {
        html.append("<b>Cursos:</b><ul>");
        for (CourseSelection course : combination.getSelections()) {
            html.append("<li>").append(escape(course.getCourseCode())).append(" ")
                    .append(escape(course.getCourseName()))
                    .append(" - grupo ").append(escape(course.getGroupId()))
                    .append(" - ").append(course.getCredits()).append(" cr. - ")
                    .append(escape(course.getArea())).append("</li>");
        }
        html.append("</ul>");
    }

    private static void appendSuggestion(StringBuilder html, Combination combination) {
        if (combination.isValid()) {
            return;
        }
        html.append("<b>Sugerencia de Prolog:</b> ");
        if (combination.getRemovable().isEmpty()) {
            html.append("ningún curso, quitado por sí solo, deja la combinación válida.");
            return;
        }
        html.append("quitar cualquiera de estos cursos deja la combinación válida:<ul>");
        for (CourseRef ref : combination.getRemovable()) {
            html.append("<li>").append(escape(ref.toString())).append("</li>");
        }
        html.append("</ul>");
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}