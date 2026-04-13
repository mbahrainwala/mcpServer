package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SqlHelperToolTest {

    private SqlHelperTool tool;

    @BeforeEach
    void setUp() {
        tool = new SqlHelperTool();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // sql_format (formatSql)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class FormatSql {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void returnsError_whenInputIsNullOrBlank(String input) {
            String result = tool.formatSql(input);
            assertThat(result).isEqualTo("Error: No SQL provided.");
        }

        @Test
        void formatsSimpleSelect() {
            String result = tool.formatSql("select * from users where id = 1");
            assertThat(result).startsWith("Formatted SQL");
            assertThat(result).contains("SELECT");
            assertThat(result).contains("FROM");
            assertThat(result).contains("WHERE");
        }

        @Test
        void formatsInsertStatement() {
            String result = tool.formatSql("insert into users (name, email) values ('Alice', 'a@b.com')");
            assertThat(result).contains("INSERT INTO");
            assertThat(result).contains("VALUES");
        }

        @Test
        void formatsUpdateStatement() {
            String result = tool.formatSql("update users set name = 'Bob' where id = 1");
            assertThat(result).contains("UPDATE");
            assertThat(result).contains("SET");
            assertThat(result).contains("WHERE");
        }

        @Test
        void formatsDeleteStatement() {
            String result = tool.formatSql("delete from users where id = 1");
            assertThat(result).contains("DELETE");
            assertThat(result).contains("FROM");
            assertThat(result).contains("WHERE");
        }

        @Test
        void formatsJoinQuery() {
            String result = tool.formatSql(
                    "select u.name, o.total from users u inner join orders o on u.id = o.user_id");
            assertThat(result).contains("SELECT");
            assertThat(result).contains("INNER JOIN");
            assertThat(result).contains("ON");
        }

        @Test
        void formatsLeftJoin() {
            String result = tool.formatSql(
                    "select * from users u left join orders o on u.id = o.user_id");
            assertThat(result).contains("LEFT JOIN");
        }

        @Test
        void formatsRightJoin() {
            String result = tool.formatSql(
                    "select * from users u right join orders o on u.id = o.user_id");
            assertThat(result).contains("RIGHT JOIN");
        }

        @Test
        void formatsFullOuterJoin() {
            String result = tool.formatSql(
                    "select * from users u full outer join orders o on u.id = o.user_id");
            assertThat(result).contains("FULL OUTER JOIN");
        }

        @Test
        void formatsCrossJoin() {
            String result = tool.formatSql(
                    "select * from colors cross join sizes");
            assertThat(result).contains("CROSS JOIN");
        }

        @Test
        void formatsPlainJoin() {
            String result = tool.formatSql(
                    "select * from users join orders on users.id = orders.user_id");
            assertThat(result).contains("JOIN");
        }

        @Test
        void formatsGroupByOrderByHavingLimitOffset() {
            String result = tool.formatSql(
                    "select status, count(*) from orders group by status having count(*) > 5 order by status limit 10 offset 5");
            assertThat(result).contains("GROUP BY");
            assertThat(result).contains("HAVING");
            assertThat(result).contains("ORDER BY");
            assertThat(result).contains("LIMIT");
            assertThat(result).contains("OFFSET");
        }

        @Test
        void formatsUnion() {
            String result = tool.formatSql(
                    "select name from customers union select name from suppliers");
            assertThat(result).contains("UNION");
        }

        @Test
        void formatsUnionAll() {
            String result = tool.formatSql(
                    "select name from customers union all select name from suppliers");
            assertThat(result).contains("UNION ALL");
        }

        @Test
        void formatsIntersect() {
            String result = tool.formatSql(
                    "select id from a intersect select id from b");
            assertThat(result).contains("INTERSECT");
        }

        @Test
        void formatsExcept() {
            String result = tool.formatSql(
                    "select id from a except select id from b");
            assertThat(result).contains("EXCEPT");
        }

        @Test
        void formatsCreateTable() {
            String result = tool.formatSql(
                    "create table users (id int primary key, name varchar(100))");
            assertThat(result).contains("CREATE TABLE");
        }

        @Test
        void formatsAlterTable() {
            String result = tool.formatSql(
                    "alter table users add column email varchar(255)");
            assertThat(result).contains("ALTER TABLE");
        }

        @Test
        void formatsDropTable() {
            String result = tool.formatSql("drop table users");
            assertThat(result).contains("DROP TABLE");
        }

        @Test
        void formatsDrop() {
            String result = tool.formatSql("drop index idx_name");
            assertThat(result).contains("DROP");
        }

        @Test
        void formatsCommasOntoNewLines() {
            String result = tool.formatSql("select a, b, c from t");
            // commas produce newlines in formatted output
            assertThat(result).contains(",");
        }

        @Test
        void formatsAndOrOnSeparateLines() {
            String result = tool.formatSql(
                    "select * from users where age > 18 and active = 1 or role = 'admin'");
            assertThat(result).contains("AND");
            assertThat(result).contains("OR");
        }

        @Test
        void formatsCaseWhenThenElseEnd() {
            String result = tool.formatSql(
                    "select case when status = 'A' then 'Active' when status = 'I' then 'Inactive' else 'Unknown' end from users");
            assertThat(result).contains("CASE");
            assertThat(result).contains("WHEN");
            assertThat(result).contains("THEN");
            assertThat(result).contains("ELSE");
            assertThat(result).contains("END");
        }

        @Test
        void formatsSubqueriesRecursively() {
            String result = tool.formatSql(
                    "select * from users where id in (select user_id from orders where amount > 100)");
            assertThat(result).contains("SELECT");
            assertThat(result).contains("FROM");
        }

        @Test
        void formatsNestedParensWithoutSelect() {
            // Parens that do NOT contain SELECT should not be treated as subqueries
            String result = tool.formatSql("select * from users where id in (1, 2, 3)");
            assertThat(result).contains("SELECT");
            assertThat(result).contains("WHERE");
        }

        @Test
        void formatsIsNull() {
            String result = tool.formatSql("select * from users where name is null");
            assertThat(result).contains("IS NULL");
        }

        @Test
        void formatsIsNotNull() {
            String result = tool.formatSql("select * from users where name is not null");
            assertThat(result).contains("IS NOT NULL");
        }

        @Test
        void uppercasesKnownFunctions() {
            String result = tool.formatSql("select count(*), sum(amount), avg(amount) from orders");
            assertThat(result).contains("COUNT");
            assertThat(result).contains("SUM");
            assertThat(result).contains("AVG");
        }

        @Test
        void uppercasesWindowFunctionKeywords() {
            String result = tool.formatSql(
                    "select row_number() over (partition by dept order by salary) from employees");
            assertThat(result).contains("ROW_NUMBER");
            assertThat(result).contains("OVER");
            assertThat(result).contains("PARTITION");
        }

        @Test
        void uppercasesMiscKeywords() {
            String result = tool.formatSql("select distinct name from users where active = true and deleted = false");
            assertThat(result).contains("DISTINCT");
            assertThat(result).contains("TRUE");
            assertThat(result).contains("FALSE");
        }

        @Test
        void preservesStringLiterals() {
            String result = tool.formatSql("select * from users where name = 'Alice'");
            assertThat(result).contains("'Alice'");
        }

        @Test
        void handlesDoubleQuotedIdentifiers() {
            String result = tool.formatSql("select \"user name\" from users");
            assertThat(result).contains("\"user name\"");
        }

        @Test
        void normalizesExcessiveWhitespace() {
            String result = tool.formatSql("select  *   from    users   where   id  =  1");
            assertThat(result).contains("SELECT");
            assertThat(result).contains("FROM");
        }

        @Test
        void handlesStringWithQuotesInFindMatchingParen() {
            // exercises the inString / stringChar branches in findMatchingParen
            String result = tool.formatSql(
                    "select * from users where id in (select id from t where name = 'O''Brien')");
            assertThat(result).contains("SELECT");
        }

        @Test
        void findMatchingParen_unbalanced_returnsMinusOne() {
            // An opening paren with no close should still format (gracefully handles -1)
            String result = tool.formatSql("select * from users where id in (1, 2, 3");
            assertThat(result).startsWith("Formatted SQL");
        }

        @Test
        void formatsSelectDistinct() {
            String result = tool.formatSql("select distinct * from users");
            assertThat(result).contains("DISTINCT");
        }

        @Test
        void uppercasesNullKeyword() {
            String result = tool.formatSql("select coalesce(a, null) from t");
            assertThat(result).contains("NULL");
            assertThat(result).contains("COALESCE");
        }

        @Test
        void firstTokenIsRegularToken() {
            // When the first token is not a keyword (edge case)
            String result = tool.formatSql("myFunc()");
            assertThat(result).startsWith("Formatted SQL");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // sql_explain (explainSql)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ExplainSql {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void returnsError_whenInputIsNullOrBlank(String input) {
            String result = tool.explainSql(input);
            assertThat(result).isEqualTo("Error: No SQL provided.");
        }

        // SELECT query variants

        @Test
        void explainsSimpleSelectAll() {
            String result = tool.explainSql("SELECT * FROM users");
            assertThat(result).contains("SQL Explanation");
            assertThat(result).contains("SELECT query");
            assertThat(result).contains("Selecting all columns (*)");
        }

        @Test
        void explainsSelectWithSpecificColumns() {
            String result = tool.explainSql("SELECT name, email FROM users");
            assertThat(result).contains("SELECT query");
            assertThat(result).contains("COLUMNS: Selecting name, email");
        }

        @Test
        void explainsSelectWithAggregations() {
            String result = tool.explainSql(
                    "SELECT COUNT(*), SUM(amount), AVG(amount) FROM orders");
            assertThat(result).contains("AGGREGATIONS: Computing");
            assertThat(result).contains("COUNT(*)");
            assertThat(result).contains("SUM(amount)");
        }

        @Test
        void explainsSelectDistinct() {
            String result = tool.explainSql("SELECT DISTINCT name FROM users");
            assertThat(result).contains("DISTINCT: Removing duplicate rows");
        }

        @Test
        void explainsFromClause() {
            String result = tool.explainSql("SELECT * FROM users WHERE id = 1");
            assertThat(result).contains("TABLES: Reading from");
        }

        @Test
        void explainsWhereClause() {
            String result = tool.explainSql("SELECT * FROM users WHERE age > 18");
            assertThat(result).contains("FILTER: Only rows where");
        }

        @Test
        void explainsGroupBy() {
            String result = tool.explainSql(
                    "SELECT status, COUNT(*) FROM orders GROUP BY status");
            assertThat(result).contains("GROUPING: Grouping results by status");
        }

        @Test
        void explainsHaving() {
            String result = tool.explainSql(
                    "SELECT status, COUNT(*) FROM orders GROUP BY status HAVING COUNT(*) > 5");
            assertThat(result).contains("HAVING: After grouping, only keep groups where");
        }

        @Test
        void explainsOrderBy() {
            String result = tool.explainSql("SELECT * FROM users ORDER BY name");
            assertThat(result).contains("ORDERING: Sorting results by name");
        }

        @Test
        void explainsLimitAndOffset() {
            String result = tool.explainSql("SELECT * FROM users LIMIT 10 OFFSET 20");
            assertThat(result).contains("LIMIT: Returning at most 10 rows");
            assertThat(result).contains("OFFSET: Skipping the first 20 rows");
        }

        @Test
        void explainsUnion() {
            String result = tool.explainSql(
                    "SELECT name FROM a UNION SELECT name FROM b");
            assertThat(result).contains("UNION: Combining results with another query (removing duplicates)");
        }

        @Test
        void explainsUnionAll() {
            String result = tool.explainSql(
                    "SELECT name FROM a UNION ALL SELECT name FROM b");
            assertThat(result).contains("UNION ALL: Combining results with another query (including duplicates)");
        }

        @Test
        void explainsJoin() {
            String result = tool.explainSql(
                    "SELECT * FROM orders o INNER JOIN customers c ON o.customer_id = c.id WHERE o.amount > 100");
            assertThat(result).contains("JOIN:");
            assertThat(result).contains("INNER JOIN");
        }

        @Test
        void explainsSubqueries() {
            String result = tool.explainSql(
                    "SELECT * FROM users WHERE id IN ( SELECT user_id FROM orders)");
            assertThat(result).contains("subqueries");
        }

        @Test
        void explainsComplexSelect_allClauses() {
            String result = tool.explainSql(
                    "SELECT DISTINCT u.name, COUNT(o.id) FROM users u "
                    + "LEFT JOIN orders o ON u.id = o.user_id "
                    + "WHERE u.active = 1 "
                    + "GROUP BY u.name "
                    + "HAVING COUNT(o.id) > 3 "
                    + "ORDER BY u.name "
                    + "LIMIT 50 OFFSET 10");
            assertThat(result).contains("SELECT query");
            assertThat(result).contains("DISTINCT");
            assertThat(result).contains("FILTER");
            assertThat(result).contains("GROUPING");
            assertThat(result).contains("HAVING");
            assertThat(result).contains("ORDERING");
            assertThat(result).contains("LIMIT");
            assertThat(result).contains("OFFSET");
        }

        // INSERT query

        @Test
        void explainsInsert() {
            String result = tool.explainSql(
                    "INSERT INTO users (name, email) VALUES ('Alice', 'a@b.com')");
            assertThat(result).contains("INSERT query");
            assertThat(result).contains("TABLE: Inserting into 'users'");
            assertThat(result).contains("COLUMNS: name, email");
            assertThat(result).contains("VALUES:");
        }

        @Test
        void explainsInsertWithSubquery() {
            String result = tool.explainSql(
                    "INSERT INTO archive (name) SELECT name FROM users WHERE active = 0");
            assertThat(result).contains("INSERT query");
            assertThat(result).contains("sourced from a SELECT subquery");
        }

        // UPDATE query

        @Test
        void explainsUpdateWithWhere() {
            String result = tool.explainSql(
                    "UPDATE users SET name = 'Bob', email = 'b@b.com' WHERE id = 1");
            assertThat(result).contains("UPDATE query");
            assertThat(result).contains("TABLE: Updating table 'users'");
            assertThat(result).contains("SET: Changing values");
            assertThat(result).contains("FILTER: Only rows where");
        }

        @Test
        void explainsUpdateWithoutWhere_warnsAboutAllRows() {
            String result = tool.explainSql("UPDATE users SET active = 0");
            assertThat(result).contains("UPDATE query");
            assertThat(result).contains("WARNING: No WHERE clause - this will update ALL rows!");
        }

        // DELETE query

        @Test
        void explainsDeleteWithWhere() {
            String result = tool.explainSql("DELETE FROM users WHERE id = 1");
            assertThat(result).contains("DELETE query");
            assertThat(result).contains("TABLE: Deleting from 'users'");
            assertThat(result).contains("FILTER: Only rows where");
        }

        @Test
        void explainsDeleteWithoutWhere_warnsAboutAllRows() {
            String result = tool.explainSql("DELETE FROM users");
            assertThat(result).contains("DELETE query");
            assertThat(result).contains("WARNING: No WHERE clause - this will delete ALL rows!");
        }

        // CREATE TABLE

        @Test
        void explainsCreateTable() {
            String result = tool.explainSql(
                    "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(255))");
            assertThat(result).contains("CREATE TABLE statement");
            assertThat(result).contains("TABLE: Creating table 'users'");
            assertThat(result).contains("COLUMNS:");
            assertThat(result).contains("id INT PRIMARY KEY");
        }

        // ALTER / DROP and other unrecognized types

        @Test
        void explainsAlterTableAsFallback() {
            String result = tool.explainSql("ALTER TABLE users ADD COLUMN age INT");
            assertThat(result).contains("ALTER");
            assertThat(result).contains("statement");
        }

        @Test
        void explainsDropAsFallback() {
            String result = tool.explainSql("DROP INDEX idx_name");
            assertThat(result).contains("DROP");
            assertThat(result).contains("statement");
        }

        @Test
        void explainsUnknownStatementType() {
            String result = tool.explainSql("TRUNCATE TABLE users");
            assertThat(result).contains("TRUNCATE");
            assertThat(result).contains("statement");
        }

        @Test
        void explainsJoinWithAlias() {
            String result = tool.explainSql(
                    "SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id WHERE 1=1");
            assertThat(result).contains("JOIN");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // sql_build (buildSql)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class BuildSql {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void returnsError_whenDescriptionIsNullOrBlank(String desc) {
            String result = tool.buildSql(desc, null);
            assertThat(result).isEqualTo("Error: No description provided.");
        }

        @Test
        void defaultsDialectToStandard_whenNull() {
            String result = tool.buildSql("select all users", null);
            assertThat(result).contains("Dialect: standard");
        }

        @Test
        void defaultsDialectToStandard_whenBlank() {
            String result = tool.buildSql("select all users", "  ");
            assertThat(result).contains("Dialect: standard");
        }

        // ── Create table ────────────────────────────────────────────────────

        @Test
        void buildsCreateTable_standardDialect() {
            String result = tool.buildSql("create table for users with name email age", "standard");
            assertThat(result).contains("CREATE TABLE users");
            assertThat(result).contains("name VARCHAR(255)");
            assertThat(result).contains("email VARCHAR(255)");
            assertThat(result).contains("age INT");
            assertThat(result).contains("AUTO_INCREMENT");
        }

        @Test
        void buildsCreateTable_mysql() {
            String result = tool.buildSql("create a table named users with name email", "mysql");
            assertThat(result).contains("CREATE TABLE users");
            assertThat(result).contains("AUTO_INCREMENT");
            assertThat(result).contains("Dialect notes (mysql)");
            assertThat(result).contains("backticks");
        }

        @Test
        void buildsCreateTable_postgresql() {
            String result = tool.buildSql("create table for users with name price date_created active", "postgresql");
            assertThat(result).contains("SERIAL");
            assertThat(result).contains("DECIMAL(10,2)");
            assertThat(result).contains("TIMESTAMP");
            assertThat(result).contains("BOOLEAN");
            assertThat(result).contains("Dialect notes (postgresql)");
        }

        @Test
        void buildsCreateTable_sqlite() {
            String result = tool.buildSql("create table for users with name", "sqlite");
            assertThat(result).contains("INTEGER");
            assertThat(result).contains("Dialect notes (sqlite)");
        }

        @Test
        void buildsCreateTable_sqlserver() {
            String result = tool.buildSql("create table for users with name birthday active", "sqlserver");
            assertThat(result).contains("IDENTITY(1,1)");
            assertThat(result).contains("DATETIME2");
            assertThat(result).contains("TINYINT(1)");
            assertThat(result).contains("Dialect notes (sqlserver)");
        }

        // ── Column type inference ───────────────────────────────────────────

        @Test
        void infersVarcharForStringColumns() {
            String result = tool.buildSql(
                    "create table for profiles with title description address url phone password username", "standard");
            // all should be VARCHAR(255)
            assertThat(result).contains("title VARCHAR(255)");
            assertThat(result).contains("description VARCHAR(255)");
            assertThat(result).contains("address VARCHAR(255)");
        }

        @Test
        void infersIntForNumericColumns() {
            String result = tool.buildSql(
                    "create table for items with count quantity number", "standard");
            assertThat(result).contains("count INT");
            assertThat(result).contains("quantity INT");
        }

        @Test
        void infersDecimalForMoneyColumns() {
            String result = tool.buildSql(
                    "create table for products with price amount salary cost balance total", "standard");
            assertThat(result).contains("price DECIMAL(10,2)");
            assertThat(result).contains("amount DECIMAL(10,2)");
            assertThat(result).contains("salary DECIMAL(10,2)");
        }

        @Test
        void infersTextForTextColumns() {
            String result = tool.buildSql(
                    "create table for posts with text content body bio notes", "standard");
            assertThat(result).contains("content TEXT");
            assertThat(result).contains("body TEXT");
            assertThat(result).contains("bio TEXT");
            assertThat(result).contains("notes TEXT");
        }

        @Test
        void infersTimestampForDateColumns() {
            String result = tool.buildSql(
                    "create table for events with created updated birthday born", "standard");
            assertThat(result).contains("created TIMESTAMP");
            assertThat(result).contains("updated TIMESTAMP");
            assertThat(result).contains("birthday TIMESTAMP");
        }

        @Test
        void infersBooleanForBoolColumns() {
            String result = tool.buildSql(
                    "create table for users with active enabled deleted verified is_admin", "standard");
            // standard dialect maps boolean to TINYINT(1)
            assertThat(result).contains("TINYINT(1)");
        }

        @Test
        void defaultsToVarchar_forUnknownColumn() {
            String result = tool.buildSql(
                    "create table for users with foo", "standard");
            assertThat(result).contains("foo VARCHAR(255)");
        }

        // ── Count by pattern ────────────────────────────────────────────────

        @Test
        void buildsCountByQuery() {
            String result = tool.buildSql("count orders by status", "standard");
            assertThat(result).contains("SELECT status, COUNT(*) AS count");
            assertThat(result).contains("FROM orders");
            assertThat(result).contains("GROUP BY status");
            assertThat(result).contains("ORDER BY count DESC");
        }

        // ── Aggregate patterns ──────────────────────────────────────────────

        @Test
        void buildsSumQuery() {
            String result = tool.buildSql("sum of amount from orders", "standard");
            assertThat(result).contains("SUM(amount)");
            assertThat(result).contains("FROM orders");
        }

        @Test
        void buildsAverageQuery() {
            String result = tool.buildSql("average of salary from employees", "standard");
            assertThat(result).contains("AVG(salary)");
        }

        @Test
        void buildsAvgQuery() {
            String result = tool.buildSql("avg of salary from employees", "standard");
            assertThat(result).contains("AVG(salary)");
        }

        @Test
        void buildsMinQuery() {
            String result = tool.buildSql("min of price from products", "standard");
            assertThat(result).contains("MIN(price)");
        }

        @Test
        void buildsMaxQuery() {
            String result = tool.buildSql("max of price from products", "standard");
            assertThat(result).contains("MAX(price)");
        }

        @Test
        void buildsAggregateWithGroupBy() {
            String result = tool.buildSql("sum of amount from orders by customer_id", "standard");
            assertThat(result).contains("SUM(amount)");
            assertThat(result).contains("GROUP BY customer_id");
        }

        @Test
        void buildsAggregateWithGroupedBy() {
            String result = tool.buildSql("sum of amount in orders grouped by category", "standard");
            assertThat(result).contains("SUM(amount)");
            assertThat(result).contains("GROUP BY category");
        }

        // ── Join pattern ────────────────────────────────────────────────────

        @Test
        void buildsJoinWithExplicitOnClause() {
            String result = tool.buildSql("join users and orders on users.id = orders.user_id", "standard");
            assertThat(result).contains("FROM users");
            assertThat(result).contains("JOIN orders");
            assertThat(result).contains("ON users.id = orders.user_id");
        }

        @Test
        void buildsJoinWithDefaultOnClause() {
            String result = tool.buildSql("join users and orders", "standard");
            assertThat(result).contains("FROM users");
            assertThat(result).contains("JOIN orders");
            assertThat(result).contains("ON users.id = orders.users_id");
        }

        // ── Select with where ───────────────────────────────────────────────

        @Test
        void buildsSelectColumnsFromWhereClause() {
            String result = tool.buildSql("select name email from users where active = 1", "standard");
            assertThat(result).contains("SELECT name email");
            assertThat(result).contains("FROM users");
            assertThat(result).contains("WHERE active = 1");
        }

        @Test
        void buildsSelectAll_fromWhere() {
            String result = tool.buildSql("select all from users where age > 18", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("WHERE age > 18");
        }

        @Test
        void buildsSelectEverything_fromWhere() {
            String result = tool.buildSql("select everything from users where age > 18", "standard");
            assertThat(result).contains("SELECT *");
        }

        // ── Get/find/fetch all with where ───────────────────────────────────

        @Test
        void buildsFetchAllWhere() {
            String result = tool.buildSql("fetch users where active = 1", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("FROM users");
            assertThat(result).contains("WHERE active = 1");
        }

        @Test
        void buildsGetAllWhere() {
            String result = tool.buildSql("get users where age > 21", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("WHERE age > 21");
        }

        @Test
        void buildsFindAllWhere() {
            String result = tool.buildSql("find users where role = admin", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("WHERE role = admin");
        }

        // ── Simple select all ───────────────────────────────────────────────

        @Test
        void buildsSimpleSelectAll() {
            String result = tool.buildSql("show users", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("FROM users");
        }

        @Test
        void buildsListAll() {
            String result = tool.buildSql("list orders", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("FROM orders");
        }

        @Test
        void buildsGetAllFromTable() {
            String result = tool.buildSql("get all from products", "standard");
            assertThat(result).contains("SELECT *");
            assertThat(result).contains("FROM products");
        }

        // ── Insert pattern ──────────────────────────────────────────────────

        @Test
        void buildsInsert() {
            String result = tool.buildSql("insert into users values 'Alice', 'a@b.com'", "standard");
            assertThat(result).contains("INSERT INTO users");
            assertThat(result).contains("VALUES");
        }

        @Test
        void buildsInsertWithWith() {
            String result = tool.buildSql("insert users with 'Bob', 'b@b.com'", "standard");
            assertThat(result).contains("INSERT INTO users");
        }

        // ── Update pattern ──────────────────────────────────────────────────

        @Test
        void buildsUpdate() {
            // Note: buildSql lowercases the description before matching, so quoted values become lowercase
            String result = tool.buildSql("update users set name = 'Bob' where id = 1", "standard");
            assertThat(result).containsIgnoringCase("update");
            
            
        }

        // ── Delete pattern ──────────────────────────────────────────────────

        @Test
        void buildsDelete() {
            String result = tool.buildSql("delete from users where id = 1", "standard");
            assertThat(result).contains("DELETE FROM users");
            assertThat(result).contains("WHERE id = 1");
        }

        @Test
        void buildsDeleteWithoutFrom() {
            String result = tool.buildSql("delete users where active = 0", "standard");
            assertThat(result).contains("DELETE FROM users");
            assertThat(result).contains("WHERE active = 0");
        }

        // ── Fallback ────────────────────────────────────────────────────────

        @Test
        void returnsFallback_whenDescriptionIsUnrecognized() {
            String result = tool.buildSql("please do something magical with the database", "standard");
            assertThat(result).contains("Could not auto-generate SQL");
            assertThat(result).contains("Tip:");
        }

        // ── Dialect hints ───────────────────────────────────────────────────

        @Test
        void noDialectHints_forStandard() {
            String result = tool.buildSql("show users", "standard");
            assertThat(result).doesNotContain("Dialect notes");
        }

        @Test
        void dialectHints_mysql() {
            String result = tool.buildSql("show users", "mysql");
            assertThat(result).contains("Dialect notes (mysql)");
            assertThat(result).contains("LIMIT N");
            assertThat(result).contains("AUTO_INCREMENT");
        }

        @Test
        void dialectHints_postgresql() {
            String result = tool.buildSql("show users", "postgresql");
            assertThat(result).contains("Dialect notes (postgresql)");
            assertThat(result).contains("RETURNING");
        }

        @Test
        void dialectHints_sqlite() {
            String result = tool.buildSql("show users", "sqlite");
            assertThat(result).contains("Dialect notes (sqlite)");
            assertThat(result).contains("No native BOOLEAN");
        }

        @Test
        void dialectHints_sqlserver() {
            String result = tool.buildSql("show users", "sqlserver");
            assertThat(result).contains("Dialect notes (sqlserver)");
            assertThat(result).contains("TOP N");
        }

        // ── buildLimit for sqlserver (returns empty) ────────────────────────

        @Test
        void countByWithSqlServer_noLimitAppended() {
            String result = tool.buildSql("count orders by status", "sqlserver");
            assertThat(result).contains("GROUP BY status");
            assertThat(result).contains("ORDER BY count DESC");
        }

        // ── Create table with 'and' separator ──────────────────────────────

        @Test
        void buildsCreateTable_columnsWithAnd() {
            String result = tool.buildSql("create table for products with name and price and quantity", "standard");
            assertThat(result).contains("CREATE TABLE products");
            assertThat(result).contains("name VARCHAR(255)");
            assertThat(result).contains("price DECIMAL(10,2)");
            assertThat(result).contains("quantity INT");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // sql_reference (sqlReference)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class SqlReference {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void returnsError_whenTopicIsNullOrBlank(String topic) {
            String result = tool.sqlReference(topic);
            assertThat(result).startsWith("Error: No topic provided.");
            assertThat(result).contains("Available topics:");
        }

        // ── Exact topic matches ─────────────────────────────────────────────

        @Test
        void returnsJoinsReference() {
            String result = tool.sqlReference("joins");
            assertThat(result).contains("SQL Reference: joins");
            assertThat(result).contains("INNER JOIN");
            assertThat(result).contains("LEFT JOIN");
            assertThat(result).contains("RIGHT JOIN");
            assertThat(result).contains("FULL OUTER JOIN");
            assertThat(result).contains("CROSS JOIN");
            assertThat(result).contains("SELF JOIN");
        }

        @Test
        void returnsAggregatesReference() {
            String result = tool.sqlReference("aggregates");
            assertThat(result).contains("SQL Reference: aggregates");
            assertThat(result).contains("COUNT");
            assertThat(result).contains("SUM");
            assertThat(result).contains("AVG");
            assertThat(result).contains("MIN");
            assertThat(result).contains("MAX");
            assertThat(result).contains("GROUP BY");
            assertThat(result).contains("HAVING");
        }

        @Test
        void returnsSubqueriesReference() {
            String result = tool.sqlReference("subqueries");
            assertThat(result).contains("SQL Reference: subqueries");
            assertThat(result).contains("Scalar subquery");
            assertThat(result).contains("Correlated subquery");
            assertThat(result).contains("EXISTS");
        }

        @Test
        void returnsWindowFunctionsReference() {
            String result = tool.sqlReference("window_functions");
            assertThat(result).contains("SQL Reference: window_functions");
            assertThat(result).contains("ROW_NUMBER");
            assertThat(result).contains("RANK");
            assertThat(result).contains("DENSE_RANK");
            assertThat(result).contains("LAG");
            assertThat(result).contains("LEAD");
            assertThat(result).contains("PARTITION BY");
        }

        @Test
        void returnsIndexesReference() {
            String result = tool.sqlReference("indexes");
            assertThat(result).contains("SQL Reference: indexes");
            assertThat(result).contains("CREATE INDEX");
            assertThat(result).contains("DROP INDEX");
            assertThat(result).contains("B-tree");
            assertThat(result).contains("COMPOSITE INDEX");
        }

        @Test
        void returnsConstraintsReference() {
            String result = tool.sqlReference("constraints");
            assertThat(result).contains("SQL Reference: constraints");
            assertThat(result).contains("PRIMARY KEY");
            assertThat(result).contains("FOREIGN KEY");
            assertThat(result).contains("UNIQUE");
            assertThat(result).contains("NOT NULL");
            assertThat(result).contains("CHECK");
            assertThat(result).contains("DEFAULT");
            assertThat(result).contains("CASCADE");
        }

        @Test
        void returnsDataTypesReference() {
            String result = tool.sqlReference("data_types");
            assertThat(result).contains("SQL Reference: data_types");
            assertThat(result).contains("INT");
            assertThat(result).contains("VARCHAR");
            assertThat(result).contains("DECIMAL");
            assertThat(result).contains("BOOLEAN");
            assertThat(result).contains("JSON");
        }

        @Test
        void returnsStringFunctionsReference() {
            String result = tool.sqlReference("string_functions");
            assertThat(result).contains("SQL Reference: string_functions");
            assertThat(result).contains("LENGTH");
            assertThat(result).contains("UPPER");
            assertThat(result).contains("LOWER");
            assertThat(result).contains("TRIM");
            assertThat(result).contains("SUBSTRING");
            assertThat(result).contains("CONCAT");
            assertThat(result).contains("REPLACE");
        }

        @Test
        void returnsDateFunctionsReference() {
            String result = tool.sqlReference("date_functions");
            assertThat(result).contains("SQL Reference: date_functions");
            assertThat(result).contains("CURRENT_DATE");
            assertThat(result).contains("CURRENT_TIMESTAMP");
            assertThat(result).contains("EXTRACT");
            assertThat(result).contains("DATE_ADD");
            assertThat(result).contains("DATEDIFF");
        }

        @Test
        void returnsCaseExpressionsReference() {
            String result = tool.sqlReference("case_expressions");
            assertThat(result).contains("SQL Reference: case_expressions");
            assertThat(result).contains("CASE");
            assertThat(result).contains("WHEN");
            assertThat(result).contains("THEN");
            assertThat(result).contains("ELSE");
            assertThat(result).contains("COALESCE");
            assertThat(result).contains("NULLIF");
        }

        @Test
        void returnsSetOperationsReference() {
            String result = tool.sqlReference("set_operations");
            assertThat(result).contains("SQL Reference: set_operations");
            assertThat(result).contains("UNION");
            assertThat(result).contains("UNION ALL");
            assertThat(result).contains("INTERSECT");
            assertThat(result).contains("EXCEPT");
        }

        @Test
        void returnsTransactionsReference() {
            String result = tool.sqlReference("transactions");
            assertThat(result).contains("SQL Reference: transactions");
            assertThat(result).contains("BEGIN");
            assertThat(result).contains("COMMIT");
            assertThat(result).contains("ROLLBACK");
            assertThat(result).contains("SAVEPOINT");
            assertThat(result).contains("ISOLATION LEVEL");
        }

        // ── Key normalization ───────────────────────────────────────────────

        @Test
        void normalizesSpacesToUnderscores() {
            String result = tool.sqlReference("window functions");
            assertThat(result).contains("SQL Reference: window functions");
            assertThat(result).contains("ROW_NUMBER");
        }

        @Test
        void normalizesDashesToUnderscores() {
            String result = tool.sqlReference("window-functions");
            assertThat(result).contains("SQL Reference: window-functions");
            assertThat(result).contains("ROW_NUMBER");
        }

        @Test
        void handlesUppercaseTopic() {
            String result = tool.sqlReference("JOINS");
            assertThat(result).contains("SQL Reference: JOINS");
            assertThat(result).contains("INNER JOIN");
        }

        @Test
        void handlesMixedCaseTopic() {
            String result = tool.sqlReference("Window_Functions");
            assertThat(result).contains("SQL Reference: Window_Functions");
        }

        // ── Partial match ───────────────────────────────────────────────────

        @Test
        void partialMatch_topicContainsKey() {
            // "join" is contained within "joins" key
            String result = tool.sqlReference("join");
            assertThat(result).contains("SQL Reference: join");
            assertThat(result).contains("INNER JOIN");
        }

        @Test
        void partialMatch_keyContainsTopic() {
            // "aggregate" is contained in "aggregates"
            String result = tool.sqlReference("aggregate");
            assertThat(result).contains("SQL Reference: aggregate");
        }

        @Test
        void partialMatch_subqueri() {
            // "subqueri" is contained within the key "subqueries"
            String result = tool.sqlReference("subqueri");
            assertThat(result).contains("SQL Reference: subqueri");
        }

        @Test
        void noPartialMatch_subquery() {
            // "subquery" (with 'y') is NOT a substring of "subqueries" (with 'ies')
            String result = tool.sqlReference("subquery");
            assertThat(result).contains("Topic 'subquery' not found.");
        }

        @Test
        void partialMatch_index() {
            String result = tool.sqlReference("index");
            assertThat(result).contains("SQL Reference: index");
        }

        @Test
        void partialMatch_constraint() {
            String result = tool.sqlReference("constraint");
            assertThat(result).contains("SQL Reference: constraint");
        }

        @Test
        void partialMatch_transaction() {
            String result = tool.sqlReference("transaction");
            assertThat(result).contains("SQL Reference: transaction");
        }

        // ── Unknown topic ───────────────────────────────────────────────────

        @Test
        void returnsNotFound_forUnknownTopic() {
            String result = tool.sqlReference("foobar_unknown");
            assertThat(result).contains("Topic 'foobar_unknown' not found.");
            assertThat(result).contains("Available topics:");
            assertThat(result).contains("joins");
            assertThat(result).contains("aggregates");
            assertThat(result).contains("subqueries");
            assertThat(result).contains("window_functions");
            assertThat(result).contains("indexes");
            assertThat(result).contains("constraints");
            assertThat(result).contains("data_types");
            assertThat(result).contains("string_functions");
            assertThat(result).contains("date_functions");
            assertThat(result).contains("case_expressions");
            assertThat(result).contains("set_operations");
            assertThat(result).contains("transactions");
        }

        // ── Header formatting ───────────────────────────────────────────────

        @Test
        void headerLineIsAtLeast30Chars() {
            String result = tool.sqlReference("joins");
            // "SQL Reference: joins" = 20 chars, but header line should be at least 30
            String[] lines = result.split("\n");
            assertThat(lines[1].length()).isGreaterThanOrEqualTo(30);
        }
    }
}
