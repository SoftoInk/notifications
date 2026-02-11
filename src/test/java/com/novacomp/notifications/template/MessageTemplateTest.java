package com.novacomp.notifications.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MessageTemplate")
class MessageTemplateTest {

    @Test
    @DisplayName("renders single variable")
    void rendersSingleVariable() {
        MessageTemplate tpl = MessageTemplate.of("Hello {{name}}!");
        String result = tpl.render(Map.of("name", "Alice"));
        assertThat(result).isEqualTo("Hello Alice!");
    }

    @Test
    @DisplayName("renders multiple variables")
    void rendersMultipleVariables() {
        MessageTemplate tpl = MessageTemplate.of("Hi {{name}}, your code is {{code}}.");
        String result = tpl.render(Map.of("name", "Bob", "code", "9876"));
        assertThat(result).isEqualTo("Hi Bob, your code is 9876.");
    }

    @Test
    @DisplayName("leaves unmatched placeholders intact")
    void leavesUnmatchedPlaceholders() {
        MessageTemplate tpl = MessageTemplate.of("Hello {{name}}, balance: {{balance}}");
        String result = tpl.render(Map.of("name", "Carol"));
        assertThat(result).isEqualTo("Hello Carol, balance: {{balance}}");
    }

    @Test
    @DisplayName("renders template with no placeholders unchanged")
    void noPlaceholdersUnchanged() {
        MessageTemplate tpl = MessageTemplate.of("Plain text, no variables.");
        String result = tpl.render(Map.of("name", "irrelevant"));
        assertThat(result).isEqualTo("Plain text, no variables.");
    }

    @Test
    @DisplayName("handles same variable used multiple times")
    void sameVariableMultipleTimes() {
        MessageTemplate tpl = MessageTemplate.of("{{x}} + {{x}} = 2*{{x}}");
        String result = tpl.render(Map.of("x", "5"));
        assertThat(result).isEqualTo("5 + 5 = 2*5");
    }

    @Test
    @DisplayName("handles special regex characters in value")
    void specialCharactersInValue() {
        MessageTemplate tpl = MessageTemplate.of("Price: {{price}}");
        String result = tpl.render(Map.of("price", "$10.00"));
        assertThat(result).isEqualTo("Price: $10.00");
    }

    @Test
    @DisplayName("static factory of() creates instance")
    void staticFactory() {
        MessageTemplate tpl = MessageTemplate.of("test");
        assertThat(tpl.getTemplate()).isEqualTo("test");
    }

    @Test
    @DisplayName("rejects null template")
    void rejectsNullTemplate() {
        assertThatThrownBy(() -> MessageTemplate.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rejects null variables map")
    void rejectsNullVariables() {
        MessageTemplate tpl = MessageTemplate.of("Hello {{name}}");
        assertThatThrownBy(() -> tpl.render(null))
                .isInstanceOf(NullPointerException.class);
    }
}
