package core.diagnostics;

import java.util.Map;
import java.util.TreeMap;

/** Formats stable single-line diagnostic events without recording secret values. */
public final class StructuredLog {
    private StructuredLog() { }

    public static String event(String name, Map<String, ?> fields) {
        StringBuilder output = new StringBuilder("event=").append(value(name));
        for (Map.Entry<String, ?> field : new TreeMap<String, Object>(fields).entrySet()) {
            output.append(' ').append(field.getKey()).append('=').append(value(field.getValue()));
        }
        return output.toString();
    }

    private static String value(Object input) {
        String text = String.valueOf(input == null ? "" : input);
        if (text.matches("[A-Za-z0-9._:/@-]*")) return text;
        return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + '"';
    }
}
