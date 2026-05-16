# CLAUDE.md — Developer Guide for llmStudioMcp

## Build & Run

```bash
# Compile
mvn compile

# Run all tests
mvn test

# Run tests for a single class
mvn test -Dtest=PhysicsToolTest

# Package (skip tests)
mvn package -DskipTests

# Run (SSE/HTTP mode, port 8282)
mvn spring-boot:run

# Run (stdio mode, for Claude Code / CLI clients)
mvn spring-boot:run -Dspring-boot.run.profiles=stdio
```

## Project Structure

```
src/main/java/com/behrainwala/mcp/
├── config/
│   ├── McpServerConfig.java     ← Register ALL tools here
│   └── McpProperties.java       ← application.yml bindings
├── model/
│   └── SearchResult.java
├── service/
│   ├── WebSearchService.java    ← DuckDuckGo scraping
│   ├── WebContentService.java   ← Jsoup fetch + text extraction
│   └── OcrService.java          ← Tesseract OCR
└── tools/                       ← One class per domain, each a @Service
    ├── WebSearchTool.java
    ├── WebContentFetcherTool.java
    ├── WebResearchTool.java      ← Combined search+fetch (token-saving)
    ├── MathTool.java
    ├── AdvancedMathTool.java
    ├── CalculusTool.java
    ├── PhysicsTool.java
    ├── ChemistryTool.java
    ├── BiologyTool.java
    ├── JsonTool.java             ← Also has yaml_to_json / json_to_yaml
    ├── CsvTool.java              ← csv_to_json, json_to_csv, csv_stats
    ├── TextTool.java
    ├── RegexTool.java
    ├── EncodingTool.java
    ├── HttpClientTool.java
    ├── DateTimeTool.java
    ├── UnitConverterTool.java
    ├── WikipediaTool.java
    ├── DictionaryTool.java
    ├── CronTool.java
    ├── ExcelFormulaTool.java
    ├── ExcelDataTool.java
    ├── ExcelTool.java
    ├── PdfTool.java
    ├── DocumentTool.java
    ├── ImageTool.java
    ├── FinanceTool.java
    ├── HealthTool.java
    ├── NetworkTool.java
    ├── ColorTool.java
    ├── DataGeneratorTool.java
    ├── NumberBaseTool.java
    ├── ProbabilityTool.java
    ├── AstronomyTool.java
    └── SqlHelperTool.java
```

## Adding a New Tool

1. Create `src/main/java/com/behrainwala/mcp/tools/MyTool.java`:

```java
@Service
public class MyTool {

    @Tool(name = "my_tool_name", description = "One clear sentence. Then detail.")
    public String myMethod(
            @ToolParam(description = "What this param is") String requiredParam,
            @ToolParam(description = "Optional. Default X.", required = false) Integer optionalParam) {
        // ...
        return result;
    }
}
```

2. Register in `McpServerConfig.java`:
   - Add `MyTool myTool` to the `allToolsProvider(...)` parameter list
   - Add `myTool` to the `.toolObjects(...)` chain

3. Add a test in `src/test/java/.../tools/MyToolTest.java`.

## Token-Reduction Design Principles

Every tool you add or modify should follow these rules to keep LLM context consumption low:

### Output sizing
- **Default to concise output.** A tool returning 200 chars is better than 2000 chars if both answer the question.
- **Add `maxChars` / `limit` params to any tool that can return variable-length content** (web content, documents, search results). Let the LLM decide how much it needs.
- **Avoid decorative formatting.** ASCII art dividers (`──────`) and empty header lines cost tokens without adding information. Use them sparingly.

### Batch operations
- **Combine related computations** into a single tool call. `batch_calculate` replaces N calls to `calculate`. A future `batch_convert` could replace N `convert_units` calls.
- Any time you notice a common pattern where an LLM calls the same tool 3+ times in a row, that's a signal to add a batch variant.

### Workflow shortcuts
- **Combine multi-step chains** into one tool. `web_research` = `web_search` + `fetch_webpage` × N. Similarly consider: `search_and_summarize`, `fetch_and_extract_table`, etc.
- Document in the `@Tool` description that the tool *replaces* a multi-step chain so the LLM knows to prefer it.

### Structured output
- When the LLM will programmatically consume your tool's output (e.g., JSON, CSV), return clean machine-readable format rather than annotated prose. Save prose descriptions for tools that inform human-readable responses.

## Tool Output Format Convention

For tools where the LLM uses the result to reason (physics, math, chemistry), the current verbose format is acceptable — it helps the LLM verify steps. For tools that feed into further processing (CSV, JSON, web content), return minimal structure.

```
# Good — math tool (LLM needs context to verify)
Newton's Second Law
───────────────────
Formula: F = ma
Given: m = 5 kg, a = 3 m/s²
Result: F = 15 N

# Good — data tool (LLM just needs the data)
{"name":"Alice","age":30}

# Avoid — data tool with unnecessary prose
JSON Conversion Result
──────────────────────
Successfully converted your CSV to JSON format.
Here is the output:
{"name":"Alice","age":30}
```

## Key Configuration

`src/main/resources/application.yml`:

| Property | Default | Effect |
|---|---|---|
| `server.port` | 8282 | HTTP port for SSE mode |
| `mcp.search.max-results` | 10 | Hard cap on web search results |
| `mcp.fetch.max-content-length` | 50000 | Hard cap on fetched page size |
| `mcp.fetch.timeout` | 30 | HTTP fetch timeout (seconds) |

For `fetch_webpage`, the LLM-controllable `maxChars` param defaults to 5000 and is capped by the server's `max-content-length`. Use `maxChars=0` to request the full server-capped content.

## Dependencies

| Library | Purpose |
|---|---|
| `spring-ai-starter-mcp-server-webmvc` | MCP SSE transport |
| `spring-ai-starter-mcp-server` | MCP stdio transport |
| `jsoup` | HTML fetch + parsing |
| `pdfbox` | PDF text extraction |
| `poi-ooxml` | .docx/.xlsx read/write |
| `poi-scratchpad` | .doc (legacy Word) |
| `tesseract-platform` | OCR (self-contained, no install) |
| `commons-csv` | CSV parsing/writing |
| `snakeyaml` | YAML (transitive via spring-boot-starter) |

## Tool Inventory by Category

### Web & Research (5 tools)
| Tool | Key Params | Token Note |
|---|---|---|
| `web_search` | `query`, `maxResults` (1–10) | Use `maxResults=3` for quick lookups |
| `fetch_webpage` | `url`, `maxChars` (default 5000) | Always pass `maxChars` — default saves ~10× vs server max |
| `web_research` | `query`, `maxSources` (1–5), `maxCharsPerSource` | **Preferred** over search+fetch chain |
| `wikipedia_lookup` | `topic` | Short, curated summaries |
| `http_request` | `method`, `url`, `headers`, `body` | For API testing |

### Math & Calculation (11 tools)
`calculate`, `batch_calculate`, `solve_quadratic`, `solve_linear_system`, `compute_polynomial`, `statistics`, `number_properties`, `trigonometry`, `triangle_properties`, `triangle_area`, `circle_properties`, `shape_area_volume`

Use `batch_calculate` when you need 2+ expressions evaluated.

### Physics (7 tools)
`physics_constants`, `physics_kinematics`, `physics_projectile`, `physics_forces`, `physics_waves`, `physics_electricity`, `physics_thermodynamics`

`physics_constants` supports single-letter queries: `g`, `c`, `h`, `G`, `e`, `k`, `r`, or `all`.

### Chemistry & Biology (10 tools)
`element_lookup`, `molar_mass`, `chemistry_solutions`, `chemistry_gas_laws`, `chemistry_equilibrium`, `dna_operations`, `hardy_weinberg`, `punnett_square`, `population_growth`, `enzyme_kinetics`

### Calculus & Linear Algebra (5 tools)
`numerical_derivative`, `numerical_integral`, `numerical_limit`, `series_sum`, `matrix_operations`

### Data & Formats (9 tools)
`json_validate`, `json_query`, `json_diff`, `json_transform`, `yaml_to_json`, `json_to_yaml`, `csv_to_json`, `json_to_csv`, `csv_stats`

### Text & Coding (9 tools)
`text_diff`, `text_analyze`, `text_transform`, `regex_test`, `regex_replace`, `regex_explain`, `encode_decode`, `hash_text`, `http_request`

### Time & Conversion (4 tools)
`get_current_datetime`, `convert_timezone`, `date_difference`, `convert_units`

### Documents (6 tools)
`pdf_to_text`, `pdf_metadata`, `doc_to_text`, `doc_create`, `doc_edit`, `image_to_text`

### Excel (9 tools)
`excel_to_text`, `excel_create`, `excel_edit`, `excel_function_lookup`, `excel_formula_explain`, `excel_formula_build`, `excel_analyze_data`, `excel_chart_recommend`, `excel_conditional_format`, `excel_pivot_guide`, `excel_vba_snippet`, `excel_shortcut_reference`

### Finance, Health, Network, Misc (20+ tools)
Finance: `finance_compound_interest`, `finance_loan_calculator`, `finance_investment_analysis`, `finance_break_even`, `finance_depreciation`, `finance_retirement_calculator`, `finance_currency_convert`

Health: `health_bmi`, `health_bmr`, `health_macro_calculator`, `health_heart_rate_zones`, `health_hydration`, `health_body_fat_estimate`

Network: `network_subnet_calculator`, `network_ip_info`, `network_cidr_range`, `network_port_reference`, `network_subnet_compare`

Other: `generate_uuid`, `generate_password`, `generate_lorem_ipsum`, `generate_fake_data`, `generate_test_dataset`, `generate_random`, `generate_sequence`, `number_base_convert`, `number_ieee754`, `number_twos_complement`, `number_bitwise`, `number_ascii_table`, `color_convert`, `color_contrast`, `color_palette`, `define_word`, `cron_explain`, `cron_build`, `sql_format`, `sql_explain`, `sql_build`, `sql_reference`, `probability_combinatorics`, `probability_distribution`, `probability_bayes`, `probability_expected_value`, `probability_markov_chain`, `astronomy_planet_info`, `astronomy_star_properties`, `astronomy_orbital_mechanics`, `astronomy_moon_phase`
