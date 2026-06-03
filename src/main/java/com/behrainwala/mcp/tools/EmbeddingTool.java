package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MCP tool for vector-space operations that show up in embedding pipelines:
 * cosine / euclidean / manhattan distance, dot product, normalization, and
 * top-k nearest-neighbor search over a candidate set. Lets a local LLM reason
 * about precomputed embeddings without calling out to a vector database.
 */
@Service
public class EmbeddingTool {

    @Tool(name = "vector_similarity", description = "Compute a similarity or distance between two numeric vectors. "
            + "Methods: 'cosine' (similarity, [-1,1]), 'euclidean' (distance, ≥0), 'manhattan' (distance, ≥0), "
            + "'dot' (dot product), 'pearson' (correlation). "
            + "Pass vectors as comma-separated floats, e.g. '0.1,0.2,0.3'. Vectors must have equal length.")
    public String vectorSimilarity(
            @ToolParam(description = "First vector as comma-separated floats") String vector1,
            @ToolParam(description = "Second vector as comma-separated floats") String vector2,
            @ToolParam(description = "Method: cosine (default), euclidean, manhattan, dot, pearson",
                    required = false) String method) {

        try {
            double[] a = parseVector(vector1);
            double[] b = parseVector(vector2);
            if (a.length != b.length) {
                return "Error: vector lengths differ (" + a.length + " vs " + b.length + ")";
            }

            String m = method == null ? "cosine" : method.strip().toLowerCase();
            double result = switch (m) {
                case "cosine" -> cosine(a, b);
                case "euclidean", "l2" -> euclidean(a, b);
                case "manhattan", "l1" -> manhattan(a, b);
                case "dot", "dot_product" -> dot(a, b);
                case "pearson", "correlation" -> pearson(a, b);
                default -> Double.NaN;
            };

            if (Double.isNaN(result)) {
                return "Error: unknown method '" + method + "'. Use cosine, euclidean, manhattan, dot, or pearson.";
            }

            String kind = switch (m) {
                case "euclidean", "l2", "manhattan", "l1" -> "distance";
                default -> "similarity";
            };
            return "Vector " + kind + " (" + m + ")\n"
                    + "─".repeat(30) + "\n"
                    + "Dimension : " + a.length + "\n"
                    + "Result    : " + String.format("%.6f", result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "vector_normalize", description = "Normalize a numeric vector to unit length (L2-norm = 1). "
            + "Useful as a preprocessing step before cosine similarity (which then becomes a dot product). "
            + "Returns the normalized vector as comma-separated floats and the original magnitude.")
    public String vectorNormalize(
            @ToolParam(description = "Vector as comma-separated floats") String vector) {
        try {
            double[] v = parseVector(vector);
            double mag = 0;
            for (double x : v) mag += x * x;
            mag = Math.sqrt(mag);
            if (mag == 0) return "Error: cannot normalize zero vector";

            StringBuilder sb = new StringBuilder();
            sb.append("Vector Normalization\n");
            sb.append("────────────────────\n");
            sb.append("Dimension     : ").append(v.length).append("\n");
            sb.append("Magnitude (L2): ").append(String.format("%.6f", mag)).append("\n");
            sb.append("Normalized    : ");
            for (int i = 0; i < v.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(String.format("%.6f", v[i] / mag));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "vector_top_k", description = "Find the top-K most similar candidate vectors to a query vector. "
            + "Candidates are passed as a multi-line string, one labeled vector per line in the form "
            + "'label: 0.1,0.2,0.3'. Default metric is cosine similarity. "
            + "Use this to do nearest-neighbor lookups over a small precomputed embedding set without "
            + "round-tripping to a vector DB.")
    public String vectorTopK(
            @ToolParam(description = "Query vector as comma-separated floats") String query,
            @ToolParam(description = "Candidate vectors, one per line: 'label: 0.1,0.2,0.3'") String candidates,
            @ToolParam(description = "Number of top results (default 5)", required = false) Integer k,
            @ToolParam(description = "Metric: cosine (default), euclidean, manhattan, dot",
                    required = false) String metric) {

        try {
            double[] q = parseVector(query);
            int topK = k == null || k <= 0 ? 5 : k;
            String m = metric == null ? "cosine" : metric.strip().toLowerCase();
            boolean smallerIsBetter = m.equals("euclidean") || m.equals("l2")
                    || m.equals("manhattan") || m.equals("l1");

            String[] lines = candidates.split("\\r?\\n");
            List<double[]> scoredIdx = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int dim = q.length;

            for (String raw : lines) {
                String line = raw.strip();
                if (line.isEmpty()) continue;
                int sep = line.indexOf(':');
                String label;
                String vecStr;
                if (sep > 0) {
                    label = line.substring(0, sep).strip();
                    vecStr = line.substring(sep + 1).strip();
                } else {
                    label = "candidate_" + (labels.size() + 1);
                    vecStr = line;
                }
                double[] cand = parseVector(vecStr);
                if (cand.length != dim) {
                    return "Error: candidate '" + label + "' has dimension " + cand.length
                            + " but query is " + dim;
                }
                double score = switch (m) {
                    case "cosine" -> cosine(q, cand);
                    case "euclidean", "l2" -> euclidean(q, cand);
                    case "manhattan", "l1" -> manhattan(q, cand);
                    case "dot", "dot_product" -> dot(q, cand);
                    default -> Double.NaN;
                };
                if (Double.isNaN(score)) {
                    return "Error: unknown metric '" + metric + "'";
                }
                labels.add(label);
                scoredIdx.add(new double[]{labels.size() - 1, score});
            }

            if (scoredIdx.isEmpty()) return "Error: no candidate vectors parsed";

            Comparator<double[]> cmp = smallerIsBetter
                    ? Comparator.comparingDouble(x -> x[1])
                    : (x, y) -> Double.compare(y[1], x[1]);
            scoredIdx.sort(cmp);

            int show = Math.min(topK, scoredIdx.size());
            StringBuilder sb = new StringBuilder();
            sb.append("Top ").append(show).append(" matches (").append(m)
                    .append(", ").append(smallerIsBetter ? "smaller is better" : "larger is better")
                    .append(")\n");
            sb.repeat("─", 40).append("\n");
            for (int i = 0; i < show; i++) {
                double[] row = scoredIdx.get(i);
                sb.append(String.format("%2d. %-20s  %s%n",
                        i + 1, labels.get((int) row[0]), String.format("%.6f", row[1])));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "vector_centroid", description = "Compute the centroid (mean) vector of a set of vectors. "
            + "Candidates are passed one vector per line (optionally with a 'label:' prefix). "
            + "Useful for clustering, query expansion, or representing a group of related embeddings.")
    public String vectorCentroid(
            @ToolParam(description = "Vectors, one per line: '0.1,0.2,0.3' or 'label: 0.1,0.2,0.3'") String vectors) {

        try {
            String[] lines = vectors.split("\\r?\\n");
            double[] sum = null;
            int count = 0;
            for (String raw : lines) {
                String line = raw.strip();
                if (line.isEmpty()) continue;
                int sep = line.indexOf(':');
                String vecStr = sep > 0 ? line.substring(sep + 1).strip() : line;
                double[] v = parseVector(vecStr);
                if (sum == null) sum = new double[v.length];
                else if (sum.length != v.length) {
                    return "Error: dimension mismatch on row " + (count + 1)
                            + " (" + v.length + " vs " + sum.length + ")";
                }
                for (int i = 0; i < v.length; i++) sum[i] += v[i];
                count++;
            }
            if (sum == null || count == 0) return "Error: no vectors parsed";

            StringBuilder sb = new StringBuilder();
            sb.append("Centroid of ").append(count).append(" vectors (dim=").append(sum.length).append(")\n");
            sb.repeat("─", 40).append("\n");
            for (int i = 0; i < sum.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(String.format("%.6f", sum[i] / count));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Math helpers
    // ─────────────────────────────────────────────────────────────

    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double euclidean(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
    }

    private double manhattan(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += Math.abs(a[i] - b[i]);
        return s;
    }

    private double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    private double pearson(double[] a, double[] b) {
        int n = a.length;
        double ma = 0, mb = 0;
        for (int i = 0; i < n; i++) { ma += a[i]; mb += b[i]; }
        ma /= n; mb /= n;
        double num = 0, da = 0, db = 0;
        for (int i = 0; i < n; i++) {
            double xa = a[i] - ma;
            double xb = b[i] - mb;
            num += xa * xb;
            da += xa * xa;
            db += xb * xb;
        }
        if (da == 0 || db == 0) return 0;
        return num / Math.sqrt(da * db);
    }

    private double[] parseVector(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("vector is empty");
        // Strip optional square brackets
        String t = s.strip();
        if (t.startsWith("[") && t.endsWith("]")) t = t.substring(1, t.length() - 1);
        String[] parts = t.split(",");
        double[] v = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].strip();
            if (p.isEmpty()) throw new IllegalArgumentException("empty value at index " + i);
            v[i] = Double.parseDouble(p);
        }
        return v;
    }
}
