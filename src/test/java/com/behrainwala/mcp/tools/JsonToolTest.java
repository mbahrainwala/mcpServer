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
        assertThat(result).contains("VALID").contains("name").contains("age");
    }

    @Test
    void validateJson_validArray() {
        String result = tool.validateJson("[1,2,3]");
        assertThat(result).contains("VALID").containsIgnoringCase("array").contains("3");
    }

    @Test
    void validateJson_invalid_trailingComma() {
        String result = tool.validateJson("{\"a\":1,}");
        assertThat(result).contains("INVALID");
    }

    @Test
    void validateJson_invalid_singleQuotes() {
        String result = tool.validateJson("{'a':'b'}");
        assertThat(result).contains("INVALID");
    }

    // ── queryJson ────────────────────────────────────────────────────────────

    @Test
    void queryJson_topLevel() {
        String result = tool.queryJson("{\"name\":\"Alice\"}", "name");
        assertThat(result).contains("Alice");
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
        assertThat(result).contains("A").contains("B");
    }

    @Test
    void queryJson_missingPath_showsTopLevelKeys() {
        String result = tool.queryJson("{\"name\":\"Alice\"}", "nonexistent");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("no value"), s -> assertThat(s).contains("name"));
    }

    // ── diffJson ─────────────────────────────────────────────────────────────

    @Test
    void diffJson_identical() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":1}");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("identical"), s -> assertThat(s).contains("0"));
    }

    @Test
    void diffJson_changedValue() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":2}");
        assertThat(result).containsIgnoringCase("changed").contains("1").contains("2");
    }

    @Test
    void diffJson_addedField() {
        String result = tool.diffJson("{\"a\":1}", "{\"a\":1,\"b\":2}");
        assertThat(result).containsIgnoringCase("added").contains("b");
    }

    @Test
    void diffJson_removedField() {
        String result = tool.diffJson("{\"a\":1,\"b\":2}", "{\"a\":1}");
        assertThat(result).containsIgnoringCase("removed").contains("b");
    }

    // ── transformJson ────────────────────────────────────────────────────────

    @Test
    void transformJson_minify() {
        String result = tool.transformJson("{\"a\": 1, \"b\": 2}", "minify");
        assertThat(result).contains("{\"a\":1,\"b\":2}");
    }

    @Test
    void transformJson_sortKeys() {
        String result = tool.transformJson("{\"z\":3,\"a\":1,\"m\":2}", "sort_keys");
        // Keys should be sorted: a, m, z
        int aPos = result.indexOf("\"a\"");
        int mPos = result.indexOf("\"m\"");
        int zPos = result.indexOf("\"z\"");
        assertThat(aPos).isLessThan(mPos).isLessThan(zPos);
    }

    @Test
    void transformJson_flatten() {
        String result = tool.transformJson("{\"a\":{\"b\":\"hello\"}}", "flatten");
        assertThat(result).contains("a.b").contains("hello");
    }

    @Test
    void transformJson_schema() {
        String result = tool.transformJson("{\"name\":\"Alice\",\"age\":30}", "schema");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("string"), s -> assertThat(s).containsIgnoringCase("number"));
    }

    @Test
    void transformJson_unknownOp() {
        String result = tool.transformJson("{\"a\":1}", "unknown_op");
        assertThat(result).containsIgnoringCase("unknown");
    }
}
