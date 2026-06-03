package com.behrainwala.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP tool for prompt engineering helpers: Mustache-style template rendering,
 * JSON Schema inference from an example JSON, and few-shot prompt assembly.
 * Lets an LLM prepare prompts and structured-output schemas without going off-box.
 */
@Service
public class PromptTool {

    private static final Pattern MUSTACHE = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");
    private static final Pattern KV_LINE = Pattern.compile("^([\\w.\\-]+)\\s*[=:]\\s*(.*)$");

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // ─────────────────────────────────────────────────────────────
    // Template rendering
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "prompt_template", description = "Render a Mustache-style prompt template. "
            + "Replaces {{variable}} placeholders with values from the variables string "
            + "(one 'name=value' per line, or JSON object). Reports any unfilled variables. "
            + "Useful for building reusable prompt scaffolds without string-concatenation hacks.")
    public String promptTemplate(
            @ToolParam(description = "Template text containing {{variable}} placeholders") String template,
            @ToolParam(description = "Variables as 'name=value' lines OR a JSON object like {\"name\":\"value\"}")
            String variables) {

        if (template == null) return "Error: template is required";
        Map<String, String> vars = parseVars(variables);

        Matcher m = MUSTACHE.matcher(template);
        StringBuilder out = new StringBuilder();
        java.util.Set<String> missing = new java.util.LinkedHashSet<>();
        java.util.Set<String> filled = new java.util.LinkedHashSet<>();

        while (m.find()) {
            String key = m.group(1);
            String value = vars.get(key);
            if (value == null) {
                missing.add(key);
                m.appendReplacement(out, Matcher.quoteReplacement("{{" + key + "}}"));
            } else {
                filled.add(key);
                m.appendReplacement(out, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(out);

        return "Rendered Template\n" +
                "─────────────────\n" +
                out + "\n\n" +
                "Filled    : " + (filled.isEmpty() ? "(none)" : String.join(", ", filled)) + "\n" +
                "Missing   : " + (missing.isEmpty() ? "(none)" : String.join(", ", missing));
    }

    private Map<String, String> parseVars(String vars) {
        Map<String, String> out = new HashMap<>();
        if (vars == null || vars.isBlank()) return out;
        String t = vars.strip();

        // Try JSON object first
        if (t.startsWith("{")) {
            try {
                JsonNode node = mapper.readTree(t);
                if (node.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> e = it.next();
                        JsonNode v = e.getValue();
                        out.put(e.getKey(), v.isTextual() ? v.asText() : v.toString());
                    }
                    return out;
                }
            } catch (JsonProcessingException ignored) {
                // fall through to k/v parsing
            }
        }

        for (String raw : t.split("\\r?\\n")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            Matcher m = KV_LINE.matcher(line);
            if (m.matches()) {
                out.put(m.group(1), m.group(2).strip());
            }
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────
    // JSON Schema inference
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "json_to_schema", description = "Infer a JSON Schema (draft 2020-12) from one or more example "
            + "JSON documents. Detects types, nullable fields, required keys, array element types, and nested "
            + "object structure. The resulting schema can be passed to an LLM as a structured-output constraint.")
    public String jsonToSchema(
            @ToolParam(description = "Example JSON (object or array of objects). Pass multiple objects in a JSON array "
                    + "for better schema inference.") String exampleJson,
            @ToolParam(description = "Schema title (optional)", required = false) String title) {

        if (exampleJson == null || exampleJson.isBlank()) return "Error: exampleJson is required";
        try {
            JsonNode root = mapper.readTree(exampleJson);
            ObjectNode schema;
            if (root.isArray()) {
                // Merge all elements to infer a union schema for items
                ObjectNode itemSchema = (ObjectNode) inferSchema(!root.isEmpty() ? root.get(0) : root);
                for (int i = 1; i < root.size(); i++) {
                    itemSchema = mergeSchemas(itemSchema, (ObjectNode) inferSchema(root.get(i)));
                }
                schema = mapper.createObjectNode();
                schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
                if (title != null && !title.isBlank()) schema.put("title", title);
                schema.put("type", "array");
                schema.set("items", itemSchema);
            } else {
                schema = (ObjectNode) inferSchema(root);
                ObjectNode wrapped = mapper.createObjectNode();
                wrapped.put("$schema", "https://json-schema.org/draft/2020-12/schema");
                if (title != null && !title.isBlank()) wrapped.put("title", title);
                wrapped.setAll(schema);
                schema = wrapped;
            }
            return mapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            return "Error parsing example JSON: " + e.getOriginalMessage();
        }
    }

    private JsonNode inferSchema(JsonNode node) {
        if (node.isNull()) {
            ObjectNode s = mapper.createObjectNode();
            s.put("type", "null");
            return s;
        }
        if (node.isObject()) {
            ObjectNode s = mapper.createObjectNode();
            s.put("type", "object");
            ObjectNode props = mapper.createObjectNode();
            ArrayNode required = mapper.createArrayNode();
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                props.set(e.getKey(), inferSchema(e.getValue()));
                if (!e.getValue().isNull()) required.add(e.getKey());
            }
            s.set("properties", props);
            if (!required.isEmpty()) s.set("required", required);
            s.put("additionalProperties", false);
            return s;
        }
        if (node.isArray()) {
            ObjectNode s = mapper.createObjectNode();
            s.put("type", "array");
            if (node.isEmpty()) {
                s.set("items", mapper.createObjectNode());
            } else {
                ObjectNode itemSchema = (ObjectNode) inferSchema(node.get(0));
                for (int i = 1; i < node.size(); i++) {
                    itemSchema = mergeSchemas(itemSchema, (ObjectNode) inferSchema(node.get(i)));
                }
                s.set("items", itemSchema);
            }
            return s;
        }
        ObjectNode s = mapper.createObjectNode();
        if (node.isTextual()) s.put("type", "string");
        else if (node.isBoolean()) s.put("type", "boolean");
        else if (node.isIntegralNumber()) s.put("type", "integer");
        else if (node.isNumber()) s.put("type", "number");
        else s.put("type", "string");
        return s;
    }

    private ObjectNode mergeSchemas(ObjectNode a, ObjectNode b) {
        if (a.equals(b)) return a;
        String ta = a.has("type") ? a.get("type").asText() : "";
        String tb = b.has("type") ? b.get("type").asText() : "";
        if (ta.equals(tb) && ta.equals("object")) {
            ObjectNode merged = mapper.createObjectNode();
            merged.put("type", "object");
            ObjectNode mergedProps = mapper.createObjectNode();
            ObjectNode aProps = (ObjectNode) a.get("properties");
            ObjectNode bProps = (ObjectNode) b.get("properties");
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            if (aProps != null) aProps.fieldNames().forEachRemaining(keys::add);
            if (bProps != null) bProps.fieldNames().forEachRemaining(keys::add);
            ArrayNode requiredA = a.has("required") ? (ArrayNode) a.get("required") : mapper.createArrayNode();
            ArrayNode requiredB = b.has("required") ? (ArrayNode) b.get("required") : mapper.createArrayNode();
            java.util.Set<String> reqA = new java.util.HashSet<>();
            requiredA.forEach(n -> reqA.add(n.asText()));
            java.util.Set<String> reqB = new java.util.HashSet<>();
            requiredB.forEach(n -> reqB.add(n.asText()));
            ArrayNode mergedReq = mapper.createArrayNode();
            for (String k : keys) {
                JsonNode av = aProps != null ? aProps.get(k) : null;
                JsonNode bv = bProps != null ? bProps.get(k) : null;
                if (av != null && bv != null) {
                    mergedProps.set(k, mergeSchemas((ObjectNode) av, (ObjectNode) bv));
                } else if (av != null) {
                    mergedProps.set(k, av);
                } else {
                    mergedProps.set(k, bv);
                }
                if (reqA.contains(k) && reqB.contains(k)) mergedReq.add(k);
            }
            merged.set("properties", mergedProps);
            if (!mergedReq.isEmpty()) merged.set("required", mergedReq);
            merged.put("additionalProperties", false);
            return merged;
        }
        // Union types via type array
        ObjectNode union = mapper.createObjectNode();
        ArrayNode types = mapper.createArrayNode();
        if (!ta.isEmpty()) types.add(ta);
        if (!tb.isEmpty() && !ta.equals(tb)) types.add(tb);
        if (types.size() == 1) union.put("type", types.get(0).asText());
        else union.set("type", types);
        return union;
    }

    // ─────────────────────────────────────────────────────────────
    // Few-shot prompt assembly
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "build_few_shot_prompt", description = "Build a few-shot prompt by assembling a task description "
            + "and a list of (input, output) examples in a clean, model-friendly format. "
            + "Examples should be passed as a JSON array of objects with 'input' and 'output' fields. "
            + "Returns the rendered prompt ready to send to an LLM.")
    public String buildFewShotPrompt(
            @ToolParam(description = "Task description / instruction for the model") String instruction,
            @ToolParam(description = "Examples as JSON array: [{\"input\":\"...\",\"output\":\"...\"}, ...]") String examplesJson,
            @ToolParam(description = "The new input to ask the model about") String query,
            @ToolParam(description = "Optional input label (default 'Input')", required = false) String inputLabel,
            @ToolParam(description = "Optional output label (default 'Output')", required = false) String outputLabel) {

        if (instruction == null || instruction.isBlank()) return "Error: instruction is required";
        if (query == null) return "Error: query is required";
        String inLabel = inputLabel == null || inputLabel.isBlank() ? "Input" : inputLabel;
        String outLabel = outputLabel == null || outputLabel.isBlank() ? "Output" : outputLabel;

        StringBuilder sb = new StringBuilder();
        sb.append(instruction.strip()).append("\n\n");

        if (examplesJson != null && !examplesJson.isBlank()) {
            try {
                JsonNode arr = mapper.readTree(examplesJson);
                if (!arr.isArray()) return "Error: examplesJson must be a JSON array";
                int i = 1;
                for (JsonNode ex : arr) {
                    String in = ex.has("input") ? ex.get("input").asText() : "";
                    String out = ex.has("output") ? ex.get("output").asText() : "";
                    sb.append("Example ").append(i++).append("\n");
                    sb.append(inLabel).append(": ").append(in).append("\n");
                    sb.append(outLabel).append(": ").append(out).append("\n\n");
                }
            } catch (JsonProcessingException e) {
                return "Error parsing examples JSON: " + e.getOriginalMessage();
            }
        }

        sb.append(inLabel).append(": ").append(query).append("\n");
        sb.append(outLabel).append(":");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // Message-list token budgeting
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "fit_to_context", description = "Trim a list of conversation messages (oldest-first) so the total "
            + "estimated token count fits under a target budget. Always keeps the system message (first message) "
            + "and the most recent N messages. Messages are passed as a JSON array of {role, content} objects. "
            + "Returns the trimmed message list as JSON.")
    public String fitToContext(
            @ToolParam(description = "Messages as JSON array: [{\"role\":\"...\",\"content\":\"...\"}, ...]") String messagesJson,
            @ToolParam(description = "Target token budget (e.g. 4000)") int budget,
            @ToolParam(description = "Minimum recent messages to always keep (default 2)", required = false)
            Integer keepRecent) {

        if (messagesJson == null || messagesJson.isBlank()) return "Error: messagesJson is required";
        if (budget <= 0) return "Error: budget must be > 0";
        int keep = keepRecent == null || keepRecent < 0 ? 2 : keepRecent;

        try {
            JsonNode arr = mapper.readTree(messagesJson);
            if (!arr.isArray()) return "Error: messagesJson must be a JSON array";
            List<JsonNode> msgs = new java.util.ArrayList<>();
            for (JsonNode n : arr) msgs.add(n);

            // Estimate tokens per message (rough: 4 chars/token + 4 overhead)
            int[] tokens = new int[msgs.size()];
            int total = 0;
            for (int i = 0; i < msgs.size(); i++) {
                String content = msgs.get(i).has("content") ? msgs.get(i).get("content").asText() : "";
                tokens[i] = (int) Math.ceil(content.length() / 4.0) + 4;
                total += tokens[i];
            }

            if (total <= budget) {
                ObjectNode wrap = mapper.createObjectNode();
                wrap.put("status", "fits");
                wrap.put("estimated_tokens", total);
                wrap.put("budget", budget);
                wrap.set("messages", arr);
                return mapper.writeValueAsString(wrap);
            }

            // Trim middle: keep first (system) and last `keep` messages, drop oldest non-system until under budget
            boolean[] keepFlag = new boolean[msgs.size()];
            if (!msgs.isEmpty()) keepFlag[0] = true;
            for (int i = Math.max(0, msgs.size() - keep); i < msgs.size(); i++) keepFlag[i] = true;
            int budgetUsed = 0;
            for (int i = 0; i < msgs.size(); i++) if (keepFlag[i]) budgetUsed += tokens[i];

            // Add messages from most recent (excluding already kept) until budget exceeded
            for (int i = msgs.size() - keep - 1; i >= 1; i--) {
                if (budgetUsed + tokens[i] > budget) break;
                keepFlag[i] = true;
                budgetUsed += tokens[i];
            }

            ArrayNode trimmed = mapper.createArrayNode();
            int dropped = 0;
            for (int i = 0; i < msgs.size(); i++) {
                if (keepFlag[i]) trimmed.add(msgs.get(i));
                else dropped++;
            }

            ObjectNode wrap = mapper.createObjectNode();
            wrap.put("status", dropped == 0 ? "fits" : "trimmed");
            wrap.put("estimated_tokens", budgetUsed);
            wrap.put("budget", budget);
            wrap.put("dropped_messages", dropped);
            wrap.set("messages", trimmed);
            return mapper.writeValueAsString(wrap);
        } catch (JsonProcessingException e) {
            return "Error parsing messages JSON: " + e.getOriginalMessage();
        }
    }
}
