package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * MCP tool for looking up word definitions using the free Dictionary API.
 */
@Service
public class DictionaryTool {

    private static final Logger log = LoggerFactory.getLogger(DictionaryTool.class);
    private static final String DICTIONARY_API = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    private final McpProperties properties;

    public DictionaryTool(McpProperties properties) {
        this.properties = properties;
    }

    @Tool(name = "define_word", description = "Look up the definition, pronunciation, and usage of an English word. "
            + "Returns definitions organized by part of speech, with examples when available.")
    public String define(
            @ToolParam(description = "The English word to look up") String word) {

        if (word == null || word.isBlank()) {
            return "Error: word is required";
        }

        try {
            String url = DICTIONARY_API + URLEncoder.encode(word.strip().toLowerCase(), StandardCharsets.UTF_8);

            log.debug("Looking up word: {}", word);

            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .timeout(properties.getSearch().getTimeout() * 1000)
                    .get();

            String json = doc.body().text();

            return parseDictionaryResponse(word.strip(), json);

        } catch (org.jsoup.HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                return "No definition found for '" + word + "'. Check the spelling and try again.";
            }
            return "Dictionary lookup failed: " + e.getMessage();
        } catch (Exception e) {
            log.error("Dictionary lookup failed for '{}': {}", word, e.getMessage());
            return "Dictionary lookup failed: " + e.getMessage();
        }
    }

    /**
     * Parses the Dictionary API JSON response into a readable format.
     * Uses simple string parsing to avoid adding a JSON library dependency.
     */
    private String parseDictionaryResponse(String word, String json) {
        StringBuilder sb = new StringBuilder();
        sb.append("Definition: ").append(word).append("\n");
        sb.append("=".repeat(30)).append("\n\n");

        // Extract phonetic
        String phonetic = extractValue(json, "phonetic");
        if (phonetic != null && !phonetic.isEmpty()) {
            sb.append("Pronunciation: ").append(phonetic).append("\n\n");
        }

        // Parse meanings (part of speech + definitions)
        int searchFrom = 0;
        int meaningIndex = json.indexOf("\"meanings\"", searchFrom);

        if (meaningIndex == -1) {
            // Fallback: just extract any definitions we can find
            return sb.append(extractAllDefinitions(json)).toString();
        }

        // Walk through partOfSpeech and definition entries
        int posIndex = json.indexOf("\"partOfSpeech\"", meaningIndex);

        while (posIndex != -1) {
            String partOfSpeech = extractValueAfter(json, posIndex);
            if (partOfSpeech != null) {
                sb.append(partOfSpeech.toUpperCase()).append("\n");
                sb.append("-".repeat(partOfSpeech.length())).append("\n");
            }

            // Find definitions within this meaning block
            int nextPosIndex = json.indexOf("\"partOfSpeech\"", posIndex + 1);
            int boundary = nextPosIndex != -1 ? nextPosIndex : json.length();

            int defIndex = json.indexOf("\"definition\"", posIndex);
            int defNum = 1;

            while (defIndex != -1 && defIndex < boundary) {
                String definition = extractValueAfter(json, defIndex);
                if (definition != null) {
                    sb.append("  ").append(defNum).append(". ").append(definition).append("\n");

                    // Look for example
                    int exIndex = json.indexOf("\"example\"", defIndex);
                    int nextDefIndex = json.indexOf("\"definition\"", defIndex + 1);
                    if (exIndex != -1 && exIndex < boundary && (nextDefIndex == -1 || exIndex < nextDefIndex)) {
                        String example = extractValueAfter(json, exIndex);
                        if (example != null) {
                            sb.append("     Example: \"").append(example).append("\"\n");
                        }
                    }

                    defNum++;
                }

                defIndex = json.indexOf("\"definition\"", defIndex + 1);
                if (defIndex >= boundary) break;
            }

            sb.append("\n");
            posIndex = nextPosIndex;
        }

        return sb.toString().strip();
    }

    private String extractValue(String json, String key) {
        int index = json.indexOf("\"" + key + "\"");
        if (index == -1) return null;
        return extractValueAfter(json, index);
    }

    private String extractValueAfter(String json, int keyIndex) {
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        int start = json.indexOf("\"", colonIndex);
        if (start == -1) return null;
        start++;

        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }

        return json.substring(start, end)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    private String extractAllDefinitions(String json) {
        StringBuilder sb = new StringBuilder();
        int defIndex = json.indexOf("\"definition\"");
        int num = 1;

        while (defIndex != -1 && num <= 10) {
            String def = extractValueAfter(json, defIndex);
            if (def != null) {
                sb.append(num).append(". ").append(def).append("\n");
                num++;
            }
            defIndex = json.indexOf("\"definition\"", defIndex + 1);
        }

        return sb.toString();
    }
}
