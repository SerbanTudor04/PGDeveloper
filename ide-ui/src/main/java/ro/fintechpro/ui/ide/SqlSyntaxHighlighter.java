// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/ide/SqlSyntaxHighlighter.java
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
            // Standard SQL
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "AS", "AND", "OR", "NOT", "NULL",
            "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "CREATE", "TABLE", "DROP", "ALTER",
            "ADD", "COLUMN", "CONSTRAINT", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "DEFAULT",
            "DISTINCT", "UNION", "ALL", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "BEGIN",
            "COMMIT", "ROLLBACK", "TRANSACTION", "GRANT", "REVOKE", "VIEW", "INDEX", "TRIGGER",
            "PROCEDURE", "FUNCTION", "RETURNS", "LANGUAGE", "DECLARE", "IF", "LOOP", "WHILE",
            // Oracle/Toad Specific
            "DUAL", "VARCHAR2", "NUMBER", "DATE", "TIMESTAMP", "CLOB", "BLOB", "SYSDATE", "ROWNUM",
            "REPLACE", "TRUNC", "CONNECT", "START", "WITH", "EXEC", "EXECUTE", "ELSIF", "EXCEPTION"
    };

    private static final String[] FUNCTIONS = new String[] {
            "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "NOW", "SUBSTRING", "LOWER", "UPPER", "LENGTH",
            // Oracle Specific
            "NVL", "NVL2", "DECODE", "TO_CHAR", "TO_DATE", "TO_NUMBER", "INSTR", "LPAD", "RPAD", "TRIM"
    };

    // Regex Groups
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String FUNCTION_PATTERN = "\\b(" + String.join("|", FUNCTIONS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "--[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN = "\\b\\d+";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<FUNCTION>" + FUNCTION_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")",
            Pattern.CASE_INSENSITIVE
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("FUNCTION") != null ? "function" :
                                    matcher.group("PAREN") != null ? "paren" :
                                            matcher.group("SEMICOLON") != null ? "semicolon" :
                                                    matcher.group("STRING") != null ? "string" :
                                                            matcher.group("COMMENT") != null ? "comment" :
                                                                    matcher.group("NUMBER") != null ? "number" :
                                                                            null;

            assert styleClass != null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public static void enable(CodeArea codeArea) {
        // 1. Listen for future changes
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(100))
                .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

        // 2. Load CSS
        String css = SqlSyntaxHighlighter.class.getResource("/sql-keywords.css").toExternalForm();
        if (!codeArea.getStylesheets().contains(css)) {
            codeArea.getStylesheets().add(css);
        }
        codeArea.getStyleClass().add("styled-text-area");

        // 3. Apply highlighting immediately
        Platform.runLater(() -> {
            try {
                if (!codeArea.getText().isEmpty()) {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                }
            } catch (Exception e) { }
        });
    }
}