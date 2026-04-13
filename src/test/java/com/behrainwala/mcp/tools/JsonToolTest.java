package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonToolTest {

    private JsonTool tool;

    @BeforeEach
    void setUp() {
        tool = new JsonTool();
    }

    // ── validateJson ─────────────────────────────────────────────────────────

    @Test
    void validateJson_validObject() {
        String result = tool.validateJson("{\"name\":\"Alice\",\"age\":30}");
        assertThat(result)
                .contains("VALID")
                .contains("object")
                .contains("Top-level keys: 2")
                .contains("Keys:");
    }

    @Test
    void validateJson_validArray() {
        String result = tool.validateJson("[1,2,3]");
        assertThat(result)
                .contains("VALID")
                .contains("array")
                .contains("Array length: 3");
    }

    @Test
    void validateJson_nestedObject_depth() {
        String result = tool.validateJson("{\"a\":{\"b\":{\"c\":1}}}");
        assertThat(result).contains("Max nesting depth: 3");
    }

    @Test
    void validateJson_scalarValue() {
        String result = tool.validateJson("42");
        assertThat(result).contains("VALID").contains("number");
    }

    @Test
    void validateJson_invalid_trailingComma() {
        String result = tool.validateJson("{\"a\":1,}");
        assertThat(result)
                .contains("INVALID")
                .contains("Common fixes:");
    }

    @Test
    void validateJson_invalid_singleQuotes() {
        String result = tool.validateJson("{'a':'b'}");
        assertThat(result).contains("INVALID");
    }

    @Test
    void validateJson_longJson_truncated() {
        // Create JSON longer than 5000 chars
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 300; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"key").append(i).append("\":\"").append("x".repeat(20)).append("\"");
        }
        sb.append("}");
        String result = tool.validateJson(sb.toString());
        assertThat(result).contains("VALID");
    }

    @Test
    void validateJson_totalNodes_counted() {
        String result = tool.validateJson("{\"a\":1,\"b\":[2,3]}");
        assertThat(result).contains("Total nodes:");
    }

    // ── queryJson ────────────────────────────────────────────────────────────

    @Test
    void queryJson_topLevel() {
        String result = tool.queryJson("{\"name\":\"Alice\"}", "name");
        assertThat(result).contains("Alice").contains("Results: 1");
    }

    @Test
    void queryJson_nested() {
        String result = tool.queryJson("{\"address\":{\"city\":\"London\"}}", "address.city");
        assertThat(result).contains("London");
    }

    @Test
    void queryJson_arrayIndex() {
        String result = tool.queryJson("{\"items\":[\"a\",\"b\",\"c\"]}", "items[1]");
        assertThat(result).contains("b");
    }

    @Test
    void queryJson_arrayStar() {
        String result = tool.queryJson("{\"users\":[{\"name\":\"A\"},{\"name\":\"B\"}]}", "users[*].name");
        assertThat(result).contains("A").contains("B").contains("[0]").contains("[1]");
    }

    @Test
    void queryJson_missingPath_showsTopLevelKeys() {
        String result = tool.queryJson("{\"name\":\"Alice\"}", "nonexistent");
        assertThat(result).contains("No value found").contains("name");
    }

    @Test
    void queryJson_invalidJson_returnsError() {
        String result = tool.queryJson("not json", "key");
        assertThat(result).contains("Invalid JSON:");
    }

    @Test
    void queryJson_emptyKey_traversesRoot() {
        String result = tool.queryJson("[1,2,3]", "[0]");
        assertThat(result).contains("1");
    }

    @Test
    void queryJson_multipleResults_moreThan100() {
        // Create JSON with many array elements
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 105; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"val\":").append(i).append("}");
        }
        sb.append("]");
        String result = tool.queryJson(sb.toString(), "[*].val");
        assertThat(result).contains("more");
    }

    // ── diffJson ─────────────────────────────────────────────────────────────

    @Test
    void diffJson_identical() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":1}");
        assertThat(result).contains("identical");
    }

    @Test
    void diffJson_changedValue() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":2}");
        assertThat(result).contains("CHANGED").contains("1").contains("2");
    }

    @Test
    void diffJson_addedField() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":1,\"b\":2}");
        assertThat(result).contains("ADDED").contains("b");
    }

    @Test
    void diffJson_removedField() {
        String result = tool.diffJson("{\"a\":1,\"b\":2}", "{\"a\":1}");
        assertThat(result).contains("REMOVED").contains("b");
    }

    @Test
    void diffJson_typeChange() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":\"string\"}");
        assertThat(result).contains("CHANGED").contains("type");
    }

    @Test
    void diffJson_arrayAddedElement() {
        String result = tool.diffJson("[1,2]", "[1,2,3]");
        assertThat(result).contains("ADDED");
    }

    @Test
    void diffJson_arrayRemovedElement() {
        String result = tool.diffJson("[1,2,3]", "[1,2]");
        assertThat(result).contains("REMOVED");
    }

    @Test
    void diffJson_arrayChangedElement() {
        String result = tool.diffJson("[1,2,3]", "[1,99,3]");
        assertThat(result).contains("CHANGED");
    }

    @Test
    void diffJson_nestedDiff() {
        String result = tool.diffJson("{\"a\":{\"b\":1}}", "{\"a\":{\"b\":2}}");
        assertThat(result).contains("CHANGED").contains("$.a.b");
    }

    @Test
    void diffJson_invalidJson_returnsError() {
        String result = tool.diffJson("not json", "{}");
        assertThat(result).contains("Invalid JSON:");
    }

    // ── transformJson ────────────────────────────────────────────────────────

    @Test
    void transformJson_minify() {
        String result = tool.transformJson("{\"a\": 1, \"b\": 2}", "minify");
        assertThat(result).contains("{\"a\":1,\"b\":2}").contains("Minified");
    }

    @Test
    void transformJson_sortKeys() {
        String result = tool.transformJson("{\"z\":3,\"a\":1,\"m\":2}", "sort_keys");
        int aPos = result.indexOf("\"a\"");
        int mPos = result.indexOf("\"m\"");
        int zPos = result.indexOf("\"z\"");
        assertThat(aPos).isLessThan(mPos).isLessThan(zPos);
    }

    @Test
    void transformJson_sortKeys_nestedArray() {
        String result = tool.transformJson("{\"z\":[{\"b\":1,\"a\":2}]}", "sort_keys");
        int aPos = result.indexOf("\"a\"");
        int bPos = result.indexOf("\"b\"");
        assertThat(aPos).isLessThan(bPos);
    }

    @Test
    void transformJson_flatten() {
        String result = tool.transformJson("{\"a\":{\"b\":\"hello\"},\"c\":[1,2]}", "flatten");
        assertThat(result)
                .contains("a.b = hello")
                .contains("c[0] = 1")
                .contains("c[1] = 2")
                .contains("Flattened");
    }

    @Test
    void transformJson_schema_objectAndArray() {
        String result = tool.transformJson("{\"name\":\"Alice\",\"items\":[1]}", "schema");
        assertThat(result).contains("string").contains("number").contains("Schema");
    }

    @Test
    void transformJson_schema_emptyArray() {
        String result = tool.transformJson("{\"items\":[]}", "schema");
        assertThat(result).contains("Schema");
    }

    @Test
    void transformJson_unknownOp() {
        String result = tool.transformJson("{\"a\":1}", "unknown_op");
        assertThat(result).contains("Unknown operation");
    }

    @Test
    void transformJson_invalidJson_returnsError() {
        String result = tool.transformJson("not json", "minify");
        assertThat(result).contains("Invalid JSON:");
    }

    @Test
    void transformJson_minify_longResult() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 400; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"key").append(i).append("\":\"").append("x".repeat(20)).append("\"");
        }
        sb.append("}");
        String result = tool.transformJson(sb.toString(), "minify");
        assertThat(result).contains("Minified");
    }

    // ── fieldNames helper coverage ───────────────────────────────────────────

    @Test
    void queryJson_fieldNames_notObject() {
        // Query a non-existent path on a non-object root
        String result = tool.queryJson("[1,2,3]", "nonexistent");
        assertThat(result).contains("No value found");
    }
}
