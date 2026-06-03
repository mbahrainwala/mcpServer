package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MCP tool focused on AI / LLM workflows: token estimation, RAG-style chunking,
 * keyword extraction, text similarity, and language detection. Designed to help an
 * LLM prepare and reason about text inputs without round-trips to remote services.
 */
@Service
public class AiTextTool {

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+(?:['’][\\p{L}\\p{N}]+)*");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z\"'(\\[])");

    private static final Set<String> STOP_WORDS_EN = Set.of(
            "the","a","an","and","or","but","if","then","else","when","at","by","for","with","about",
            "against","between","into","through","during","before","after","above","below","to","from",
            "up","down","in","out","on","off","over","under","again","further","once","is","am","are",
            "was","were","be","been","being","have","has","had","having","do","does","did","doing",
            "this","that","these","those","i","me","my","myself","we","our","ours","ourselves","you",
            "your","yours","yourself","yourselves","he","him","his","himself","she","her","hers",
            "herself","it","its","itself","they","them","their","theirs","themselves","what","which",
            "who","whom","whose","of","as","not","no","nor","so","than","too","very","can","will",
            "just","should","now","also","any","all","some","such","only","own","same","there","here"
    );

    // ── Language detection signatures (top stop words per language) ──
    private static final Map<String, Set<String>> LANG_MARKERS = new LinkedHashMap<>();
    static {
        LANG_MARKERS.put("English", Set.of("the","and","of","to","in","is","that","it","for","with","as","on","be","by","this","are"));
        LANG_MARKERS.put("Spanish", Set.of("el","la","de","que","y","en","los","las","un","una","es","por","con","para","del","se"));
        LANG_MARKERS.put("French",  Set.of("le","la","de","et","les","des","un","une","est","que","pour","dans","sur","avec","ce","du"));
        LANG_MARKERS.put("German",  Set.of("der","die","das","und","ist","den","von","zu","mit","ein","eine","auf","im","für","auch","dem"));
        LANG_MARKERS.put("Italian", Set.of("il","la","di","e","che","è","un","una","per","con","del","della","sono","non","gli","alla"));
        LANG_MARKERS.put("Portuguese", Set.of("o","a","de","que","e","do","da","em","um","para","com","não","uma","os","no","se"));
        LANG_MARKERS.put("Dutch",   Set.of("de","het","een","van","en","is","in","op","dat","met","voor","te","zijn","aan","door","niet"));
    }

    // ─────────────────────────────────────────────────────────────
    // Token estimation
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "count_tokens", description = "Estimate the number of LLM tokens in a text. "
            + "Uses heuristics calibrated against well-known tokenizers (~4 chars/token for English, "
            + "or ~0.75 tokens/word). Supports model families: 'gpt', 'claude', 'llama', 'mistral', 'generic'. "
            + "Useful for budgeting prompts before sending to an API or fitting context windows.")
    public String countTokens(
            @ToolParam(description = "The text to estimate tokens for") String text,
            @ToolParam(description = "Model family: 'gpt' (default), 'claude', 'llama', 'mistral', 'generic'.",
                    required = false) String model) {

        if (text == null) return "Error: text is required";

        String family = model == null ? "gpt" : model.strip().toLowerCase();
        int chars = text.length();
        int words = countWords(text);

        // Calibration factors derived from public tokenizer benchmarks for English text.
        // These are heuristics — for code or non-English text, results vary 10-30%.
        double charsPerToken = switch (family) {
            case "claude" -> 3.5;    // Claude's tokenizer is slightly denser
            case "llama", "llama2", "llama3" -> 3.8;
            case "mistral" -> 3.9;
            case "generic" -> 4.0;
            default -> 4.0;           // gpt-3.5/gpt-4 BPE: ~4 chars/token
        };
        double wordsPerToken = switch (family) {
            case "claude" -> 0.72;
            case "llama", "llama2", "llama3" -> 0.74;
            case "mistral" -> 0.74;
            default -> 0.75;
        };

        // Two estimates — char-based is more reliable for code, word-based for prose
        int byChars = (int) Math.ceil(chars / charsPerToken);
        int byWords = (int) Math.ceil(words / wordsPerToken);
        // Weighted: chars dominates for short text, average otherwise
        int estimate = chars < 200 ? byChars : (int) Math.round((byChars + byWords) / 2.0);

        return "Token Estimate\n"
                + "──────────────\n"
                + "Model family : " + family + "\n"
                + "Characters   : " + chars + "\n"
                + "Words        : " + words + "\n"
                + "Estimate     : ~" + estimate + " tokens\n"
                + "  by chars   : " + byChars + " (using " + charsPerToken + " chars/token)\n"
                + "  by words   : " + byWords + " (using " + wordsPerToken + " tokens/word)\n"
                + "Note: heuristic only; exact counts require the model's tokenizer.";
    }

    // ─────────────────────────────────────────────────────────────
    // Chunking for RAG / embedding pipelines
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "chunk_text", description = "Split text into overlapping chunks suitable for embedding / RAG. "
            + "Chunks respect sentence boundaries when possible. Returns chunks separated by '---CHUNK n---' markers. "
            + "Use 'characters' or 'words' as the unit. Typical settings: chunkSize=512, overlap=64 (words).")
    public String chunkText(
            @ToolParam(description = "The text to chunk") String text,
            @ToolParam(description = "Target chunk size") int chunkSize,
            @ToolParam(description = "Overlap between consecutive chunks (must be < chunkSize)") int overlap,
            @ToolParam(description = "Unit: 'words' (default) or 'characters'", required = false) String unit) {

        if (text == null || text.isBlank()) return "Error: text is required";
        if (chunkSize <= 0) return "Error: chunkSize must be > 0";
        if (overlap < 0 || overlap >= chunkSize) return "Error: overlap must be 0 <= overlap < chunkSize";

        String u = unit == null ? "words" : unit.strip().toLowerCase();
        List<String> chunks = "characters".equals(u) || "chars".equals(u)
                ? chunkByChars(text, chunkSize, overlap)
                : chunkByWords(text, chunkSize, overlap);

        StringBuilder sb = new StringBuilder();
        sb.append("Text Chunks (").append(chunks.size()).append(" chunks, ")
                .append(chunkSize).append(" ").append(u).append(" each, ")
                .append(overlap).append(" overlap)\n");
        sb.repeat("─", 50).append("\n");
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("---CHUNK ").append(i + 1).append("---\n");
            sb.append(chunks.get(i)).append("\n\n");
        }
        return sb.toString().strip();
    }

    private List<String> chunkByWords(String text, int chunkSize, int overlap) {
        List<String> sentences = splitSentences(text);
        // Build word-aligned chunks but try to break on sentence ends
        List<String> chunks = new ArrayList<>();
        List<String> wordBuf = new ArrayList<>();
        List<Integer> sentenceEndAt = new ArrayList<>();

        for (String s : sentences) {
            String[] ws = s.split("\\s+");
            for (String w : ws) if (!w.isEmpty()) wordBuf.add(w);
            sentenceEndAt.add(wordBuf.size());
        }

        int i = 0;
        while (i < wordBuf.size()) {
            int end = Math.min(i + chunkSize, wordBuf.size());
            // Try to snap end down to the nearest sentence end within the last 25% of the chunk
            int snap = end;
            int minSnap = i + Math.max(1, chunkSize * 3 / 4);
            for (int se : sentenceEndAt) {
                if (se > minSnap && se <= end) snap = se;
            }
            chunks.add(String.join(" ", wordBuf.subList(i, snap)));
            if (snap >= wordBuf.size()) break;
            i = snap - overlap;
            if (i <= 0 || i >= snap) i = snap;
        }
        return chunks;
    }

    private List<String> chunkByChars(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + chunkSize, text.length());
            // Snap to whitespace if possible (within last 10% of chunk)
            int snap = end;
            int minSnap = i + Math.max(1, chunkSize * 9 / 10);
            for (int k = end - 1; k >= minSnap; k--) {
                if (Character.isWhitespace(text.charAt(k))) { snap = k; break; }
            }
            chunks.add(text.substring(i, snap).strip());
            if (snap >= text.length()) break;
            i = snap - overlap;
            if (i <= 0 || i >= snap) i = snap;
        }
        return chunks;
    }

    // ─────────────────────────────────────────────────────────────
    // Keyword extraction (TF / TF-IDF-lite)
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "extract_keywords", description = "Extract the most informative keywords from a text using "
            + "term-frequency with English stop-word filtering and a light inverse-document-frequency weighting "
            + "based on common-word penalties. Returns top-N keywords ranked by score. "
            + "Useful for tagging, indexing, or building queries.")
    public String extractKeywords(
            @ToolParam(description = "The text to extract keywords from") String text,
            @ToolParam(description = "Number of keywords to return (default 10)", required = false) Integer topN,
            @ToolParam(description = "Minimum keyword length in chars (default 3)", required = false) Integer minLen) {

        if (text == null || text.isBlank()) return "Error: text is required";
        int n = topN == null || topN <= 0 ? 10 : topN;
        int ml = minLen == null || minLen <= 0 ? 3 : minLen;

        Map<String, Integer> tf = new HashMap<>();
        Matcher m = WORD_PATTERN.matcher(text.toLowerCase());
        int totalTokens = 0;
        while (m.find()) {
            String w = m.group();
            if (w.length() < ml) continue;
            if (STOP_WORDS_EN.contains(w)) continue;
            // Skip pure numbers
            if (w.chars().allMatch(c -> Character.isDigit(c) || c == '.')) continue;
            tf.merge(w, 1, Integer::sum);
            totalTokens++;
        }

        if (tf.isEmpty()) return "No keywords found (text may be too short or all stop words).";

        // Score = tf * (1 + log(uniqueness factor))
        // uniqueness factor approximated as 1/(1+log(1+commonness)); since we don't have a corpus,
        // we penalize words that appear with very high frequency (likely topic words show up moderate freq)
        double total = totalTokens;
        List<Map.Entry<String, Double>> scored = tf.entrySet().stream()
                .map(e -> {
                    double freq = e.getValue() / total;
                    // Bell-curve scoring: penalize ultra-rare (1 occurrence in long text) and ultra-common
                    double tfWeight = e.getValue();
                    double idfLike = 1.0 + Math.log(1.0 + e.getKey().length() / 4.0);
                    double penalty = freq > 0.10 ? 0.5 : 1.0;
                    return Map.entry(e.getKey(), tfWeight * idfLike * penalty);
                })
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Top Keywords (").append(scored.size()).append(")\n");
        sb.repeat("─", 30).append("\n");
        for (int i = 0; i < scored.size(); i++) {
            var e = scored.get(i);
            sb.append(String.format("%2d. %-20s  count=%d  score=%.2f%n",
                    i + 1, e.getKey(), tf.get(e.getKey()), e.getValue()));
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // Text similarity
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "text_similarity", description = "Compute similarity between two texts using one of: "
            + "'cosine' (TF cosine over tokens), 'jaccard' (token-set overlap), "
            + "'levenshtein' (edit-distance ratio), 'shingle' (3-gram Jaccard). "
            + "Returns a similarity score in [0, 1] where 1 = identical.")
    public String textSimilarity(
            @ToolParam(description = "First text") String text1,
            @ToolParam(description = "Second text") String text2,
            @ToolParam(description = "Algorithm: 'cosine' (default), 'jaccard', 'levenshtein', 'shingle'",
                    required = false) String method) {

        if (text1 == null || text2 == null) return "Error: both text1 and text2 are required";
        String alg = method == null ? "cosine" : method.strip().toLowerCase();

        double sim = switch (alg) {
            case "cosine" -> cosineSim(text1, text2);
            case "jaccard" -> jaccardSim(text1, text2);
            case "levenshtein", "edit" -> levenshteinSim(text1, text2);
            case "shingle", "ngram" -> shingleSim(text1, text2, 3);
            default -> Double.NaN;
        };

        if (Double.isNaN(sim)) {
            return "Error: unknown algorithm '" + method + "'. Use cosine, jaccard, levenshtein, or shingle.";
        }
        return String.format("Text Similarity (%s)%n──────────────────%nScore: %.4f  (0 = different, 1 = identical)",
                alg, sim);
    }

    private double cosineSim(String a, String b) {
        Map<String, Integer> tfA = termFreq(a);
        Map<String, Integer> tfB = termFreq(b);
        if (tfA.isEmpty() || tfB.isEmpty()) return 0;
        Set<String> union = new HashSet<>(tfA.keySet());
        union.addAll(tfB.keySet());
        double dot = 0, na = 0, nb = 0;
        for (String t : union) {
            int va = tfA.getOrDefault(t, 0);
            int vb = tfB.getOrDefault(t, 0);
            dot += (double) va * vb;
            na += (double) va * va;
            nb += (double) vb * vb;
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double jaccardSim(String a, String b) {
        Set<String> sa = termFreq(a).keySet();
        Set<String> sb = termFreq(b).keySet();
        if (sa.isEmpty() && sb.isEmpty()) return 1;
        Set<String> intersect = new HashSet<>(sa);
        intersect.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0 : (double) intersect.size() / union.size();
    }

    private double levenshteinSim(String a, String b) {
        int d = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) d / maxLen;
    }

    private int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }

    private double shingleSim(String a, String b, int k) {
        Set<String> sa = shingles(a, k);
        Set<String> sb = shingles(b, k);
        if (sa.isEmpty() && sb.isEmpty()) return 1;
        Set<String> intersect = new HashSet<>(sa);
        intersect.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0 : (double) intersect.size() / union.size();
    }

    private Set<String> shingles(String text, int k) {
        String normalized = text.toLowerCase().replaceAll("\\s+", " ").strip();
        Set<String> out = new HashSet<>();
        if (normalized.length() < k) {
            if (!normalized.isEmpty()) out.add(normalized);
            return out;
        }
        for (int i = 0; i <= normalized.length() - k; i++) {
            out.add(normalized.substring(i, i + k));
        }
        return out;
    }

    private Map<String, Integer> termFreq(String text) {
        Map<String, Integer> tf = new HashMap<>();
        Matcher m = WORD_PATTERN.matcher(text.toLowerCase());
        while (m.find()) tf.merge(m.group(), 1, Integer::sum);
        return tf;
    }

    // ─────────────────────────────────────────────────────────────
    // Language detection
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "detect_language", description = "Detect the language of a text using stop-word signatures. "
            + "Supports English, Spanish, French, German, Italian, Portuguese, Dutch. "
            + "Returns the most likely language with a confidence score. For short texts (<20 words) "
            + "results may be unreliable.")
    public String detectLanguage(
            @ToolParam(description = "Text to identify the language of") String text) {

        if (text == null || text.isBlank()) return "Error: text is required";

        Map<String, Integer> wordCounts = new HashMap<>();
        Matcher m = WORD_PATTERN.matcher(text.toLowerCase());
        int total = 0;
        while (m.find()) {
            wordCounts.merge(m.group(), 1, Integer::sum);
            total++;
        }
        if (total == 0) return "Error: no words found";

        Map<String, Double> scores = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : LANG_MARKERS.entrySet()) {
            int hits = 0;
            for (String marker : e.getValue()) {
                hits += wordCounts.getOrDefault(marker, 0);
            }
            scores.put(e.getKey(), (double) hits / total);
        }

        String best = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
        double bestScore = scores.get(best);

        StringBuilder sb = new StringBuilder();
        sb.append("Language Detection\n");
        sb.append("──────────────────\n");
        sb.append("Best guess : ").append(best);
        if (bestScore < 0.02) sb.append("  (low confidence — text may be too short or in an unsupported language)");
        sb.append("\nScore      : ").append(String.format("%.3f", bestScore))
                .append("  (fraction of words matching language markers)\n");
        sb.append("\nAll scores:\n");
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("  %-12s %.3f%n", e.getKey(), e.getValue())));
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // Summarization helper — extract top sentences
    // ─────────────────────────────────────────────────────────────

    @Tool(name = "extract_sentences", description = "Extract the most representative sentences from a text using "
            + "a TextRank-like scoring (sentences containing high-frequency content words rank higher). "
            + "Useful for quick summarization before passing to an LLM, reducing token usage substantially.")
    public String extractSentences(
            @ToolParam(description = "Text to summarize") String text,
            @ToolParam(description = "Number of top sentences to return (default 5)", required = false) Integer topN) {

        if (text == null || text.isBlank()) return "Error: text is required";
        int n = topN == null || topN <= 0 ? 5 : topN;

        List<String> sentences = splitSentences(text);
        if (sentences.size() <= n) return String.join(" ", sentences);

        // Build word importance (TF with stop-word filter)
        Map<String, Integer> tf = new HashMap<>();
        for (String s : sentences) {
            Matcher m = WORD_PATTERN.matcher(s.toLowerCase());
            while (m.find()) {
                String w = m.group();
                if (w.length() > 2 && !STOP_WORDS_EN.contains(w)) tf.merge(w, 1, Integer::sum);
            }
        }

        // Score sentences by sum of word frequencies, normalized by length
        List<double[]> scores = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            Matcher m = WORD_PATTERN.matcher(s.toLowerCase());
            double score = 0;
            int words = 0;
            while (m.find()) {
                String w = m.group();
                if (w.length() > 2 && !STOP_WORDS_EN.contains(w)) {
                    score += tf.getOrDefault(w, 0);
                    words++;
                }
            }
            scores.add(new double[]{i, words == 0 ? 0 : score / Math.sqrt(words)});
        }

        // Take top N by score, then sort back into original order
        scores.sort((x, y) -> Double.compare(y[1], x[1]));
        List<Integer> topIdx = scores.stream().limit(n).map(x -> (int) x[0]).sorted().toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Top ").append(topIdx.size()).append(" Sentences (of ").append(sentences.size()).append(")\n");
        sb.repeat("─", 40).append("\n");
        for (int i : topIdx) sb.append(sentences.get(i)).append(" ");
        return sb.toString().strip();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        Matcher m = WORD_PATTERN.matcher(text);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    private List<String> splitSentences(String text) {
        // Quick sentence splitter; treats . ! ? as terminators
        String[] parts = SENTENCE_SPLIT.split(text.strip());
        return Arrays.stream(parts).map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
