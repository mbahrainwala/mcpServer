package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptToolTest {

    private PromptTool tool;

    @BeforeEach
    void setUp() {
        tool = new PromptTool();
    }

    @Test
    void template_replacesPlaceholders_kvForm() {
        String result = tool.promptTemplate("Hello {{name}}, you are {{role}}.",
                "name=Alice\nrole=engineer");
        assertThat(result).contains("Hello Alice, you are engineer.");
        assertThat(result).contains("Filled    : name, role");
        assertThat(result).contains("Missing   : (none)");
    }

    @Test
    void template_replacesPlaceholders_jsonForm() {
        String result = tool.promptTemplate("Hello {{name}}.",
                "{\"name\":\"Bob\"}");
        assertThat(result).contains("Hello Bob.");
    }

    @Test
    void template_reportsMissing() {
        String result = tool.promptTemplate("Hi {{first}} {{last}}", "first=Carol");
        assertThat(result).contains("Hi Carol {{last}}");
        assertThat(result).contains("Missing   : last");
    }

    @Test
    void jsonToSchema_objectWithPrimitives() {
        String result = tool.jsonToSchema("{\"name\":\"Alice\",\"age\":30,\"active\":true}", null);
        assertThat(result).contains("\"type\" : \"object\"")
                .contains("\"name\"")
                .contains("\"type\" : \"string\"")
                .contains("\"age\"")
                .contains("\"type\" : \"integer\"")
                .contains("\"active\"")
                .contains("\"type\" : \"boolean\"");
    }

    @Test
    void jsonToSchema_arrayOfObjects() {
        String result = tool.jsonToSchema("[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]", "Items");
        assertThat(result).contains("\"type\" : \"array\"");
        assertThat(result).contains("\"items\"");
        assertThat(result).contains("\"title\" : \"Items\"");
    }

    @Test
    void jsonToSchema_nestedObject() {
        String result = tool.jsonToSchema("{\"user\":{\"name\":\"x\",\"age\":1}}", null);
        assertThat(result).contains("\"user\"");
        assertThat(result).contains("\"properties\"");
    }

    @Test
    void jsonToSchema_invalidJson() {
        String result = tool.jsonToSchema("{not valid", null);
        assertThat(result).startsWith("Error parsing");
    }

    @Test
    void buildFewShotPrompt_assemblesExamples() {
        String examples = "[{\"input\":\"hello\",\"output\":\"world\"},{\"input\":\"foo\",\"output\":\"bar\"}]";
        String result = tool.buildFewShotPrompt("Translate the input.", examples, "ping", null, null);
        assertThat(result).contains("Translate the input.")
                .contains("Example 1")
                .contains("Input: hello")
                .contains("Output: world")
                .contains("Example 2")
                .contains("Input: foo")
                .contains("Output: bar")
                .contains("Input: ping");
        assertThat(result).endsWith("Output:");
    }

    @Test
    void buildFewShotPrompt_customLabels() {
        String result = tool.buildFewShotPrompt("Task.", "[]", "x", "Q", "A");
        assertThat(result).contains("Q: x");
        assertThat(result).contains("A:");
    }

    @Test
    void fitToContext_underBudgetFits() {
        String messages = "[{\"role\":\"system\",\"content\":\"short\"},{\"role\":\"user\",\"content\":\"hi\"}]";
        String result = tool.fitToContext(messages, 1000, 2);
        assertThat(result).contains("\"status\" : \"fits\"");
    }

    @Test
    void fitToContext_overBudgetTrims() {
        // Build many messages that exceed budget
        StringBuilder b = new StringBuilder("[");
        b.append("{\"role\":\"system\",\"content\":\"sys\"}");
        for (int i = 0; i < 50; i++) {
            b.append(",{\"role\":\"user\",\"content\":\"").repeat("a", 100)
                    .append("\"}");
        }
        b.append("]");
        String result = tool.fitToContext(b.toString(), 500, 2);
        assertThat(result).contains("\"status\" : \"trimmed\"");
        assertThat(result).contains("\"dropped_messages\"");
    }

    @Test
    void fitToContext_invalidJson() {
        String result = tool.fitToContext("not json", 100, 1);
        assertThat(result).startsWith("Error parsing");
    }
}
