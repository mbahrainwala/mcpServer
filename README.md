# LM Studio MCP Server — AI Tool Suite

A Spring Boot MCP (Model Context Protocol) server that gives LM Studio models a comprehensive
toolkit: web search, advanced math, coding utilities (regex, JSON, HTTP client, cron),
date/time, Wikipedia, unit conversion, Excel assistance, and more — all with **zero API keys required**.

## Tools Provided

| Tool | Description                                                                            |
|------|----------------------------------------------------------------------------------------|
| `web_search` | Search the internet via DuckDuckGo. Returns titles, URLs, and snippets.                |
| `fetch_webpage` | Fetch a URL and extract the main text content (strips ads, nav, scripts).              |
| `calculate` | Evaluate math expressions with exact precision (+, -, *, /, ^, sqrt, sin, log, etc.).  |
| `solve_quadratic` | Solve quadratic equations (ax² + bx + c = 0), returns real or complex roots.           |
| `solve_linear_system` | Solve systems of 2 linear equations (Cramer's rule).                                   |
| `compute_polynomial` | Evaluate any polynomial at a given x value.                                            |
| `triangle_properties` | Full triangle analysis: area, angles, heights, medians, radii from 3 sides.            |
| `triangle_area` | Area via 4 methods: base/height, two sides + angle, coordinates, Heron's.              |
| `circle_properties` | All circle measurements from any one known value.                                      |
| `shape_area_volume` | Area/volume for rectangles, spheres, cylinders, cones, pyramids, and more.             |
| `statistics` | Descriptive stats: mean, median, mode, std dev, quartiles, IQR.                        |
| `number_properties` | Factors, prime factorization, GCD/LCM, primality, perfect squares.                     |
| `trigonometry` | Trig values for any angle, or solve a right triangle from 2 known values.              |
| `get_current_datetime` | Get the current date and time in any timezone.                                         |
| `convert_timezone` | Convert a date/time between timezones.                                                 |
| `date_difference` | Calculate the difference between two dates.                                            |
| `wikipedia_lookup` | Look up a topic on Wikipedia and get a concise summary.                                |
| `convert_units` | Convert between units: length, weight, volume, temperature, speed, area, time, data.   |
| `define_word` | Look up English word definitions, pronunciation, and usage examples.                   |
| **Coding Tools** |                                                                                        |
| `regex_test` | Test a regex pattern against input, showing all matches and captured groups.           |
| `regex_replace` | Regex find-and-replace with backreference support.                                     |
| `regex_explain` | Break down a regex pattern into plain English.                                         |
| `json_validate` | Validate, pretty-print, and analyze JSON structure.                                    |
| `json_query` | Extract values from JSON using dot-notation paths (`users[*].email`).                  |
| `json_diff` | Compare two JSON documents and show added/removed/changed fields.                      |
| `json_transform` | Minify, sort keys, flatten, or extract schema from JSON.                               |
| `encode_decode` | Base64, URL, HTML, and Hex encoding/decoding.                                          |
| `hash_text` | Generate MD5, SHA-1, SHA-256, SHA-512 hashes.                                          |
| `generate_uuid` | Generate UUIDs in standard, compact, base64, or short format.                          |
| `generate_password` | Generate secure random passwords with configurable requirements.                       |
| `http_request` | Make HTTP requests (GET/POST/PUT/DELETE) to test APIs.                                 |
| `cron_explain` | Parse a cron expression and explain it in plain English.                               |
| `cron_build` | Build a cron expression from a plain English description.                              |
| `text_diff` | Line-by-line diff of two text blocks (like code snippets).                             |
| `text_analyze` | Word count, character count, frequency analysis, readability stats.                    |
| `text_transform` | Convert between camelCase, snake_case, PascalCase, kebab-case, etc. Sort/dedupe lines. |
| **Physics Tools** |                                                                                        |
| `physics_constants` | Look up fundamental constants (speed of light, Planck, Boltzmann, etc.).               |
| `physics_kinematics` | Solve SUVAT equations — provide any 3 of s, u, v, a, t.                                |
| `physics_forces` | Newton's laws, gravity, friction, centripetal, spring, work, energy, momentum, power.  |
| `physics_waves` | Wave speed, photon energy, de Broglie, Doppler, Snell's law, thin lens.                |
| `physics_electricity` | Ohm's law, resistors/capacitors series/parallel, Coulomb, RC/RL circuits, E-field.     |
| `physics_thermodynamics` | Ideal gas law, heat transfer, Carnot efficiency, Stefan-Boltzmann, entropy.            |
| **Chemistry Tools** |                                                                                        |
| `element_lookup` | Periodic table lookup — atomic number, mass, group, period, category.                  |
| `molar_mass` | Calculate molar mass from formula (e.g. `Ca(OH)2` → 74.093 g/mol) with mass %.         |
| `chemistry_solutions` | Molarity, dilution, pH/pOH, mass↔moles, stoichiometry.                                 |
| `chemistry_gas_laws` | Ideal gas, Boyle's, Charles's, Dalton's, Graham's effusion.                            |
| `chemistry_equilibrium` | Kp/Kc, half-life, Arrhenius equation, Nernst equation.                                 |
| **Biology Tools** |                                                                                        |
| `dna_operations` | Complement, reverse complement, transcribe, translate, GC content, Tm, MW.             |
| `hardy_weinberg` | Hardy-Weinberg equilibrium — allele & genotype frequencies.                            |
| `punnett_square` | Mono/dihybrid crosses with genotype & phenotype ratios.                                |
| `population_growth` | Exponential, logistic growth, doubling time, growth rate.                              |
| `enzyme_kinetics` | Michaelis-Menten, Lineweaver-Burk, competitive inhibition, kcat.                       |
| **Calculus Tools** |                                                                                        |
| `numerical_derivative` | Numerical differentiation (1st–4th order) at a point.                                  |
| `numerical_integral` | Definite integration via Simpson's rule.                                               |
| `numerical_limit` | Evaluate limits from left, right, or both sides (including ±∞).                        |
| `series_sum` | Geometric, harmonic, p-series, Taylor series (sin, cos, e^x, ln).                      |
| `matrix_operations` | Determinant, inverse, multiply, transpose, eigenvalues, solve Ax=b.                    |
| **Excel Tools** |                                                                                        |
| `excel_function_lookup` | Search 80+ Excel functions by name, category, or keyword with syntax and examples.     |
| `excel_formula_explain` | Break down complex Excel formulas into plain English — functions, references, issues.  |
| `excel_formula_build` | Generate Excel formulas from plain English descriptions (VLOOKUP, SUMIFS, etc.).       |
| `excel_analyze_data` | Analyze CSV data — detect types, statistics, data quality, suggest formulas.           |
| `excel_chart_recommend` | Recommend the best chart type for your data (line, pie, bar, scatter, etc.).           |
| `excel_conditional_format` | Generate conditional formatting rules (duplicates, traffic light, data bars, etc.).    |
| `excel_pivot_guide` | Step-by-step pivot table creation with field placement and calculated fields.          |
| `excel_vba_snippet` | Generate VBA macros (highlight cells, remove blanks, export PDF, consolidate sheets).  |
| `excel_shortcut_reference` | Excel keyboard shortcuts by category (navigation, formatting, formulas, data).         |
| **PDF Tools** |                                                                                        |
| `pdf_to_text` | Extract text from a PDF — local file path, URL, or base64-encoded content.             |
| `pdf_metadata` | Extract PDF metadata: title, author, pages, size, encryption status, etc.              |
| `pdf_to_images` | Extract PDF pages to individual images in a subfolder for each file.                   |

### Example Workflows

- **Research:** `web_search` → `fetch_webpage` → answer with current info
- **Math:** *"What's 15% compound interest on $10,000 over 5 years?"* → `calculate`
- **Equations:** *"Solve x² - 5x + 6 = 0"* → `solve_quadratic` → roots: x=2, x=3
- **Geometry:** *"Area of a triangle with sides 3, 4, 5"* → `triangle_properties` → area, angles, radii
- **Statistics:** *"Mean and std dev of 85, 90, 78, 92, 88"* → `statistics`
- **Shapes:** *"Volume of a cylinder with radius 5 and height 10"* → `shape_area_volume`
- **Regex:** *"Does this regex match emails?"* → `regex_test` validates against real input
- **API testing:** *"Test my endpoint"* → `http_request` with custom headers/body
- **JSON:** *"Is this JSON valid?"* → `json_validate` → pretty-print + structure analysis
- **Cron:** *"What does `0 */6 * * MON-FRI` mean?"* → `cron_explain`
- **Naming:** *"Convert getUserName to snake_case"* → `text_transform`
- **Physics:** *"A ball is thrown up at 20m/s, how high does it go?"* → `physics_kinematics`
- **Chemistry:** *"What's the molar mass of C6H12O6?"* → `molar_mass` → 180.156 g/mol
- **Biology:** *"Translate ATGGCTTAG"* → `dna_operations` → Met-Ala-Stop
- **Genetics:** *"Cross Aa × Aa"* → `punnett_square` → 1:2:1 ratio
- **Calculus:** *"Integrate x² from 0 to 3"* → `numerical_integral` → 9.0
- **Matrices:** *"Determinant of [[1,2],[3,4]]"* → `matrix_operations` → -2
- **Planning:** *"What time is it in Tokyo?"* → `get_current_datetime` → `convert_timezone`
- **Learning:** *"What does 'ephemeral' mean?"* → `define_word`
- **Cooking:** *"Convert 2 cups to milliliters"* → `convert_units`
- **Excel:** *"How do I use XLOOKUP?"* → `excel_function_lookup` → syntax, examples, tips
- **Formulas:** *"Sum sales where region is East"* → `excel_formula_build` → `=SUMIFS(...)`
- **Charts:** *"Best chart for monthly revenue?"* → `excel_chart_recommend` → line chart
- **VBA:** *"Macro to export each sheet as PDF"* → `excel_vba_snippet`

## Prerequisites

- **Java 21+** (verify with `java -version`)
- **Maven 3.9+** (verify with `mvn -version`)
- **LM Studio 0.3.17+** (MCP support required)

## Quick Start

### 1. Build

```bash
cd behrainwalaMcp
mvn clean package -DskipTests
```

This produces `target/mcp-1.0.0.jar`.

### 2. Run the Server

The server supports two transport modes from a single JAR — **SSE** (HTTP) and **stdio** (subprocess) — controlled via Spring profiles.

#### Option A: SSE Mode (default)

Runs as an HTTP server. You start it separately and clients connect over the network.

```bash
java -jar target/mcp-1.0.0.jar
```

The server starts on **http://localhost:8282** by default.

Verify it's running:
```bash
curl http://localhost:8282/actuator/health   # Health check with details
curl http://localhost:8282/actuator/info     # App info, Java version, OS
curl http://localhost:8282/actuator/metrics  # Available metrics
curl http://localhost:8282/sse              # SSE endpoint (will stream)
```

#### Option B: Stdio Mode

Runs as a subprocess managed by the MCP host (Claude Code, LM Studio, etc.). No need to start the server manually — the host launches it and communicates via stdin/stdout.

```bash
java -jar target/mcp-1.0.0.jar --spring.profiles.active=stdio
```

This disables the web server, suppresses all console logging (to keep the JSON-RPC stream clean), and activates stdio transport.

#### When to use which

| | SSE | Stdio |
|---|---|---|
| **Startup** | You start it manually | Host launches it automatically |
| **Sharing** | Multiple clients can connect to one server | One client per process |
| **Network** | Accessible over HTTP (local or remote) | Local only (stdin/stdout) |
| **Best for** | LM Studio, shared/remote setups | Claude Code, single-user CLI tools |

### Alternative: Docker (SSE only)

```bash
# Build the image
docker build -t behrainwala-mcp .

# Run the container
docker run -d --name behrainwala-mcp -p 8282:8282 behrainwala-mcp

# Override settings via environment
docker run -d -p 8282:8282 \
  -e MCP_SEARCH_MAX_RESULTS=20 \
  -e MCP_FETCH_MAX_CONTENT_LENGTH=100000 \
  behrainwala-mcp
```

The image uses a multi-stage build (JDK for build, JRE for runtime) and includes a health check.

### 3. Configure Your MCP Host

#### LM Studio (SSE)

Edit the LM Studio MCP configuration file:

- **Windows:** `%USERPROFILE%/.lmstudio/mcp.json`
- **macOS/Linux:** `~/.lmstudio/mcp.json`

Or open it from the LM Studio UI: **Right sidebar → Developer tab (terminal icon) → MCP → Edit mcp.json**

```json
{
  "mcpServers": {
    "behrainwala-mcp": {
      "url": "http://localhost:8282/sse"
    }
  }
}
```

#### Claude Code (Stdio — recommended)

Register the server so Claude Code launches it automatically as a subprocess.

**Project-local** (only available in the current project):

```bash
claude mcp add behrainwala-mcp -- java -jar C:\dev\workspace\mcpServer\target\mcp-1.0.0.jar --spring.profiles.active=stdio
```

**Global** (available across all projects):

```bash
claude mcp add -s user behrainwala-mcp -- java -jar C:\dev\workspace\mcpServer\target\mcp-1.0.0.jar --spring.profiles.active=stdio
```

Verify it was added and is connected:

```bash
claude mcp get behrainwala-mcp   # should show Status: ✓ Connected
claude mcp list                  # should list behrainwala-mcp
```

To remove it later:

```bash
claude mcp remove behrainwala-mcp
```

Or add manually to `~/.claude/settings.json`:

```json
{
  "mcpServers": {
    "behrainwala-mcp": {
      "command": "java",
      "args": ["-jar", "d:\\Development\\java\\llmStudioMcp\\target\\mcp-1.0.0.jar", "--spring.profiles.active=stdio"]
    }
  }
}
```

#### Claude Code (SSE — if server is already running)

```bash
claude mcp add --transport sse behrainwala-mcp-sse http://localhost:8282/sse
```

### 4. Use It

1. Open LM Studio and load a model that supports tool calling (e.g., Qwen 2.5, Llama 3.x, Mistral, etc.)
2. In the chat, the model now has access to all 60+ tools
3. Try questions like:
   - *"What are the latest developments in quantum computing?"* (web_search + fetch_webpage)
   - *"What's the square root of 7 to 10 decimal places?"* (calculate)
   - *"What time is it in London right now?"* (get_current_datetime)
   - *"Convert 72°F to Celsius"* (convert_units)
   - *"Tell me about the Rosetta Stone"* (wikipedia_lookup)
   - *"What does 'serendipity' mean?"* (define_word)

LM Studio will show a tool-call confirmation dialog. Approve it, and the model will
receive the search results in its context.

## Configuration

All settings are in `src/main/resources/application.yml`:

```yaml
server:
  port: 8282                          # Server port

mcp:
  search:
    max-results: 10                   # Max search results per query
    timeout: 30                       # Search timeout (seconds)
  fetch:
    max-content-length: 50000         # Max chars returned from a fetched page
    timeout: 30                       # Fetch timeout (seconds)
    user-agent: "LMStudio-MCP/1.0"   # User-Agent for HTTP requests
```

Override any property at runtime:
```bash
java -jar target/llm-studio-mcp-1.0.0.jar --server.port=9090
java -jar target/llm-studio-mcp-1.0.0.jar --mcp.search.max-results=20
```

Or via environment variables:
```bash
MCP_SEARCH_MAX_RESULTS=20 java -jar target/llm-studio-mcp-1.0.0.jar
```

## Architecture

```
                                  ┌──────────────────────────┐
┌─────────────┐     SSE/HTTP      │                          │
│  LM Studio  │ ◄──────────────► │                          │
└─────────────┘   JSON-RPC 2.0   │  MCP Server              │
                                  │  (mcp-1.0.0.jar)         │
┌─────────────┐     stdio         │                          │
│ Claude Code │ ◄──────────────► │  web_search ──────► DuckDuckGo
└─────────────┘   JSON-RPC 2.0   │  fetch_webpage ───► Any URL
                                  │  wikipedia_lookup ► Wikipedia API
                                  │  define_word ─────► Dictionary API
                                  │  http_request ────► Any API
                                  │  pdf_to_text ─────► Local/URL/Base64
                                  │  calculate, statistics  (local)
                                  │  solve_quadratic, etc.  (local)
                                  │  regex_*, json_*        (local)
                                  │  cron_*, text_*         (local)
                                  │  encode, hash, uuid     (local)
                                  │  datetime, units        (local)
                                  │  excel_* formulas/data  (local)
                                  └──────────────────────────┘
```

### Key Components

| File | Purpose |
|------|---------|
| `LlmStudioMcpApplication.java` | Spring Boot entry point |
| `config/McpServerConfig.java` | Registers all tools with the MCP framework |
| `config/McpProperties.java` | Typed configuration properties |
| **Web Tools** | |
| `tools/WebSearchTool.java` | `web_search` — internet search via DuckDuckGo |
| `tools/WebContentFetcherTool.java` | `fetch_webpage` — fetch and extract page content |
| **Knowledge Tools** | |
| `tools/WikipediaTool.java` | `wikipedia_lookup` — Wikipedia article summaries |
| `tools/DictionaryTool.java` | `define_word` — English word definitions |
| **Math Tools** | |
| `tools/MathTool.java` | `calculate` — precise math expression evaluator |
| `tools/AdvancedMathTool.java` | `solve_quadratic`, `triangle_properties`, `statistics`, `trigonometry`, etc. |
| **Utility Tools** | |
| `tools/DateTimeTool.java` | `get_current_datetime`, `convert_timezone`, `date_difference` |
| `tools/UnitConverterTool.java` | `convert_units` — length, weight, volume, temp, speed, area, time, data |
| **Coding Tools** | |
| `tools/RegexTool.java` | `regex_test`, `regex_replace`, `regex_explain` |
| `tools/JsonTool.java` | `json_validate`, `json_query`, `json_diff`, `json_transform` |
| `tools/EncodingTool.java` | `encode_decode`, `hash_text`, `generate_uuid`, `generate_password` |
| `tools/HttpClientTool.java` | `http_request` — test any API endpoint |
| `tools/CronTool.java` | `cron_explain`, `cron_build` |
| `tools/TextTool.java` | `text_diff`, `text_analyze`, `text_transform` |
| **Excel Tools** | |
| `tools/ExcelFormulaTool.java` | `excel_function_lookup`, `excel_formula_explain`, `excel_formula_build` |
| `tools/ExcelDataTool.java` | `excel_analyze_data`, `excel_chart_recommend`, `excel_conditional_format`, `excel_pivot_guide`, `excel_vba_snippet`, `excel_shortcut_reference` |
| **Services** | |
| `service/WebSearchService.java` | DuckDuckGo search implementation |
| `service/WebContentService.java` | HTML fetch and text extraction |
| `model/SearchResult.java` | Search result data model |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| LM Studio doesn't show the tools | Ensure the server is running and `mcp.json` URL is correct. Restart LM Studio. |
| Search returns no results | DuckDuckGo may be rate-limiting. Wait a moment and try again. |
| Fetch returns truncated content | Increase `mcp.fetch.max-content-length` in config. |
| Connection refused | Check the port matches between `application.yml` and `mcp.json`. |
| Model doesn't call the tools | Not all models support tool calling. Use Qwen 2.5 7B+, Llama 3.1 8B+, or similar. |
| Stdio mode not connecting | Ensure you pass `--spring.profiles.active=stdio`. Check that `java` is on PATH. |
| PDF tool fails in SSE mode | Use a URL or `base64:`-prefixed content instead of a local file path. |

## License

MIT
