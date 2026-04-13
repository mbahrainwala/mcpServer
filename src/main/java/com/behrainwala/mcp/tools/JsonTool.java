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

import java.util.*;

/**
 * MCP tool for JSON validation, formatting, querying, and transformation.
 */
@Service
public class JsonTool {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final ObjectMapper compactMapper = new ObjectMapper();

    @Tool(name = "json_validate", description = "Validate and pretty-print a JSON string. "
            + "Returns whether the JSON is valid, the structure (object/array), "
            + "key count, nesting depth, and a formatted version. "
            + "Use this to check JSON before suggesting it to users.")
    public String validateJson(
            @ToolParam(description = "The JSON string to validate") String json) {

        StringBuilder sb = new StringBuilder();
        sb.append("JSON Validation\n");
        sb.append("───────────────\n\n");

        try {
            JsonNode node = mapper.readTree(json);

            sb.append("Status: VALID ✓\n");
            sb.append("Type: ").append(node.getNodeType().name().toLowerCase()).append("\n");

            if (node.isObject()) {
                sb.append("Top-level keys: ").append(node.size()).append("\n");
                sb.append("Keys: ").append(fieldNames(node)).append("\n");
            } else if (node.isArray()) {
                sb.append("Array length: ").append(node.size()).append("\n");
            }

            int depth = maxDepth(node, 0);
            sb.append("Max nesting depth: ").append(depth).append("\n");
            sb.append("Total nodes: ").append(countNodes(node)).append("\n\n");

            String pretty = mapper.writeValueAsString(node);
            if (pretty.length() > 5000) {
                pretty = pretty.substring(0, 5000) + "\n... [truncated at 5000 chars]";
            }
            sb.append("Formatted:\n").append(pretty);

        } catch (JsonProcessingException e) {
            sb.append("Status: INVALID ✗\n\n");
            sb.append("Error: ").append(e.getOriginalMessage()).append("\n");
            long line = e.getLocation() != null ? e.getLocation().getLineNr() : -1;
            long col = e.getLocation() != null ? e.getLocation().getColumnNr() : -1;
            if (line > 0) sb.append("Location: line ").append(line).append(", column ").append(col).append("\n");
            sb.append("\nCommon fixes:\n");
            sb.append("  - Check for trailing commas (not allowed in JSON)\n");
            sb.append("  - Use double quotes for keys and strings (not single quotes)\n");
            sb.append("  - Escape special characters in strings\n");
            sb.append("  - Ensure all brackets/braces are balanced");
        }

        return sb.toString();
    }

    @Tool(name = "json_query", description = "Extract values from a JSON document using a simple dot-notation path. "
            + "Supports: 'key', 'key.nested', 'key[0]', 'key[*].field' (all array elements). "
            + "Use this to inspect specific parts of a JSON structure.")
    public String queryJson(
            @ToolParam(description = "The JSON string to query") String json,
            @ToolParam(description = "The path to extract. Examples: 'name', 'address.city', 'items[0].price', "
                    + "'users[*].email' (all emails from array)") String path) {

        try {
            JsonNode root = mapper.readTree(json);
            List<JsonNode> results = queryPath(root, path);

            StringBuilder sb = new StringBuilder();
            sb.append("JSON Query\n");
            sb.append("──────────\n");
            sb.append("Path: ").append(path).append("\n");
            sb.append("Results: ").append(results.size()).append("\n\n");

            if (results.isEmpty()) {
                sb.append("No value found at path '").append(path).append("'\n\n");
                sb.append("Available top-level keys: ").append(fieldNames(root));
            } else if (results.size() == 1) {
                sb.append("Value: ").append(mapper.writeValueAsString(results.getFirst()));
            } else {
                for (int i = 0; i < results.size() && i < 100; i++) {
                    sb.append("[").append(i).append("] ").append(compactMapper.writeValueAsString(results.get(i))).append("\n");
                }
                if (results.size() > 100) sb.append("... ").append(results.size() - 100).append(" more");
            }

            return sb.toString();

        } catch (JsonProcessingException e) {
            return "Invalid JSON: " + e.getOriginalMessage();
        }
    }

    @Tool(name = "json_diff", description = "Compare two JSON documents and show the differences. "
            + "Identifies added, removed, and changed fields.")
    public String diffJson(
            @ToolParam(description = "The first JSON string (original)") String json1,
            @ToolParam(description = "The second JSON string (modified)") String json2) {

        try {
            JsonNode node1 = mapper.readTree(json1);
            JsonNode node2 = mapper.readTree(json2);

            List<String> diffs = new ArrayList<>();
            compareNodes(node1, node2, "$", diffs);

            StringBuilder sb = new StringBuilder();
            sb.append("JSON Diff\n");
            sb.append("─────────\n\n");

            if (diffs.isEmpty()) {
                sb.append("No differences found — the JSON documents are identical.");
            } else {
                sb.append("Found ").append(diffs.size()).append(" difference(s):\n\n");
                for (String diff : diffs) {
                    sb.append(diff).append("\n");
                }
            }

            return sb.toString();

        } catch (JsonProcessingException e) {
            return "Invalid JSON: " + e.getOriginalMessage();
        }
    }

    @Tool(name = "json_transform", description = "Transform JSON: minify, sort keys, flatten nested structure, "
            + "or extract a schema (structure with types, no values).")
    public String transformJson(
            @ToolParam(description = "The JSON string to transform") String json,
            @ToolParam(description = "Transformation: 'minify', 'sort_keys', 'flatten', or 'schema'") String operation) {

        try {
            JsonNode node = mapper.readTree(json);

            return switch (operation.strip().toLowerCase()) {
                case "minify" -> {
                    String result = compactMapper.writeValueAsString(node);
                    yield "Minified (" + result.length() + " chars):\n" + truncate(result);
                }
                case "sort_keys" -> {
                    JsonNode sorted = sortKeys(node);
                    yield "Sorted keys:\n" + mapper.writeValueAsString(sorted);
                }
                case "flatten" -> {
                    Map<String, String> flat = new LinkedHashMap<>();
                    flattenNode(node, "", flat);
                    StringBuilder sb = new StringBuilder("Flattened (" + flat.size() + " entries):\n");
                    flat.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
                    yield sb.toString();
                }
                case "schema" -> {
                    JsonNode schema = extractSchema(node);
                    yield "JSON Schema/Structure:\n" + mapper.writeValueAsString(schema);
                }
                default -> "Unknown operation '" + operation + "'. Use: minify, sort_keys, flatten, or schema.";
            };

        } catch (JsonProcessingException e) {
            return "Invalid JSON: " + e.getOriginalMessage();
        }
    }

    // ── Helpers ──

    private String fieldNames(JsonNode node) {
        if (!node.isObject()) return "(not an object)";
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names.toString();
    }

    private int maxDepth(JsonNode node, int current) {
        if (node.isObject() || node.isArray()) {
            int max = current;
            for (JsonNode child : node) {
                max = Math.max(max, maxDepth(child, current + 1));
            }
            return max;
        }
        return current;
    }

    private int countNodes(JsonNode node) {
        int count = 1;
        for (JsonNode child : node) count += countNodes(child);
        return count;
    }

    private List<JsonNode> queryPath(JsonNode node, String path) {
        List<JsonNode> current = List.of(node);

        for (String segment : path.split("\\.")) {
            List<JsonNode> next = new ArrayList<>();

            // Handle array indexing: key[0] or key[*]
            String key = segment;
            String index = null;
            if (segment.contains("[")) {
                int bracketStart = segment.indexOf('[');
                key = segment.substring(0, bracketStart);
                index = segment.substring(bracketStart + 1, segment.indexOf(']'));
            }

            for (JsonNode n : current) {
                JsonNode child = key.isEmpty() ? n : n.get(key);
                if (child == null) continue;

                if (index != null) {
                    if ("*".equals(index)) {
                        if (child.isArray()) child.forEach(next::add);
                    } else {
                        int idx = Integer.parseInt(index);
                        if (child.isArray() && idx < child.size()) next.add(child.get(idx));
                    }
                } else {
                    next.add(child);
                }
            }

            current = next;
        }

        return current;
    }

    private void compareNodes(JsonNode n1, JsonNode n2, String path, List<String> diffs) {
        if (n1.equals(n2)) return;

        if (n1.getNodeType() != n2.getNodeType()) {
            diffs.add("CHANGED " + path + ": type " + n1.getNodeType() + " → " + n2.getNodeType());
            return;
        }

        if (n1.isObject()) {
            Set<String> allKeys = new TreeSet<>();
            n1.fieldNames().forEachRemaining(allKeys::add);
            n2.fieldNames().forEachRemaining(allKeys::add);

            for (String key : allKeys) {
                String childPath = path + "." + key;
                if (!n1.has(key)) diffs.add("ADDED   " + childPath + ": " + compact(n2.get(key)));
                else if (!n2.has(key)) diffs.add("REMOVED " + childPath + ": " + compact(n1.get(key)));
                else compareNodes(n1.get(key), n2.get(key), childPath, diffs);
            }
        } else if (n1.isArray()) {
            int max = Math.max(n1.size(), n2.size());
            for (int i = 0; i < max; i++) {
                String childPath = path + "[" + i + "]";
                if (i >= n1.size()) diffs.add("ADDED   " + childPath + ": " + compact(n2.get(i)));
                else if (i >= n2.size()) diffs.add("REMOVED " + childPath + ": " + compact(n1.get(i)));
                else compareNodes(n1.get(i), n2.get(i), childPath, diffs);
            }
        } else {
            diffs.add("CHANGED " + path + ": " + compact(n1) + " → " + compact(n2));
        }
    }

    private JsonNode sortKeys(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = mapper.createObjectNode();
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys);
            for (String key : keys) sorted.set(key, sortKeys(node.get(key)));
            return sorted;
        } else if (node.isArray()) {
            ArrayNode arr = mapper.createArrayNode();
            for (JsonNode child : node) arr.add(sortKeys(child));
            return arr;
        }
        return node;
    }

    private void flattenNode(JsonNode node, String prefix, Map<String, String> result) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(entry.getValue(), key, result);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flattenNode(node.get(i), prefix + "[" + i + "]", result);
            }
        } else {
            result.put(prefix, node.asText());
        }
    }

    private JsonNode extractSchema(JsonNode node) {
        if (node.isObject()) {
            ObjectNode schema = mapper.createObjectNode();
            node.fields().forEachRemaining(entry ->
                    schema.set(entry.getKey(), extractSchema(entry.getValue())));
            return schema;
        } else if (node.isArray()) {
            ArrayNode schema = mapper.createArrayNode();
            if (!node.isEmpty()) schema.add(extractSchema(node.get(0)));
            return schema;
        } else {
            return mapper.getNodeFactory().textNode(node.getNodeType().name().toLowerCase());
        }
    }

    private String compact(JsonNode node) {
        try {
            String s = compactMapper.writeValueAsString(node);
            return s.length() > 100 ? s.substring(0, 100) + "..." : s;
        } catch (Exception e) { return node.toString(); }
    }

    private String truncate(String s) {
        return s.length() <= 5000 ? s : s.substring(0, 5000) + "... [truncated]";
    }
}
