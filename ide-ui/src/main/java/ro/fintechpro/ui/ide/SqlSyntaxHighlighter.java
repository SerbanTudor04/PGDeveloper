package ro.fintechpro.ui.ide;

import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlSyntaxHighlighter {

    private static final String[] KEYWORDS = new String[] {
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "AND", "OR", "NOT", "NULL",
            "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "AS", "CREATE", "TABLE",
            "DROP", "ALTER", "ADD", "CONSTRAINT", "PRIMARY", "KEY", "FOREIGN", "REFERENCES",
            "select", "from", "where", "insert", "into", "values", "update", "set", "delete" // cover lowercase too
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "--[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    public static void enable(CodeArea codeArea) {
        // 1. Listen for future changes
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

        // 2. Load CSS
        codeArea.getStylesheets().add(SqlSyntaxHighlighter.class.getResource("/sql-keywords.css").toExternalForm());

        // 3. FIX: Apply highlighting immediately to existing text
        Platform.runLater(() -> {
            try {
                if (!codeArea.getText().isEmpty()) {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                }
            } catch (Exception e) {
                // Context might not be ready yet, safe to ignore for split second
            }
        });
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                            matcher.group("STRING") != null ? "string" :
                                                    matcher.group("COMMENT") != null ? "comment" :
                                                            null; /* never happens */

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}