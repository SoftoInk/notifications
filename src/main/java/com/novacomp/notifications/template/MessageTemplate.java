package com.novacomp.notifications.template;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight mustache-style message template.
 *
 * <p>Variables are delimited by double braces: {@code {{variable}}}.
 * Call {@link #render(Map)} to produce the final text.</p>
 *
 * <pre>{@code
 * MessageTemplate tpl = MessageTemplate.of("Hello {{name}}, your code is {{code}}.");
 * String text = tpl.render(Map.of("name", "Alice", "code", "1234"));
 * // → "Hello Alice, your code is 1234."
 * }</pre>
 */
public class MessageTemplate {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final String template;

    public MessageTemplate(String template) {
        this.template = Objects.requireNonNull(template, "Template must not be null");
    }

    public static MessageTemplate of(String template) {
        return new MessageTemplate(template);
    }

    /**
     * Replaces all {@code {{key}}} placeholders with the corresponding values.
     * Variables that have no matching entry in the map are left unchanged.
     * Null values are rendered as empty strings.
     */
    public String render(Map<String, String> variables) {
        Objects.requireNonNull(variables, "Variables map must not be null");

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** Returns the raw template string. */
    public String getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        return "MessageTemplate{" + template + "}";
    }
}
