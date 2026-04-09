package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlHelperToolTest {

    private SqlHelperTool tool;

    @BeforeEach
    void setUp() {
        tool = new SqlHelperTool();
    }

    // ── formatSql ────────────────────────────────────────────────────────────

    @Test
    void formatSql_selectStatement() {
        String result = tool.formatSql("select * from users where id = 1");
        assertThat(result).containsIgnoringCase("SELECT").containsIgnoringCase("FROM")
                .containsIgnoringCase("WHERE");
    }

    @Test
    void formatSql_joinQuery() {
        String result = tool.formatSql("select u.name, o.total from users u inner join orders o on u.id = o.user_id");
        assertThat(result).containsIgnoringCase("SELECT").containsIgnoringCase("JOIN");
    }

    @Test
    void formatSql_blank_returnsError() {
        String result = tool.formatSql("");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("no sql"));
    }

    @Test
    void formatSql_null_returnsError() {
        String result = tool.formatSql(null);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("no sql"));
    }

    @Test
    void formatSql_insertStatement() {
        String result = tool.formatSql("insert into users (name, email) values ('Alice', 'a@b.com')");
        assertThat(result).containsIgnoringCase("INSERT").containsIgnoringCase("VALUES");
    }
}
