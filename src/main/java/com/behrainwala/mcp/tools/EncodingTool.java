package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * MCP tool for encoding, decoding, and hashing operations commonly needed in development.
 */
@Service
public class EncodingTool {

    @Tool(name = "encode_decode", description = "Encode or decode text using common formats: "
            + "base64, url, html, hex. Use this for data transformation tasks in coding.")
    public String encodeDecode(
            @ToolParam(description = "The text to encode or decode") String input,
            @ToolParam(description = "Format: 'base64', 'url', 'html', or 'hex'") String format,
            @ToolParam(description = "Direction: 'encode' or 'decode'") String direction) {

        boolean encode = direction.strip().toLowerCase().startsWith("e");
        String fmt = format.strip().toLowerCase();

        try {
            String result = switch (fmt) {
                case "base64" -> encode
                        ? Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8))
                        : new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
                case "url" -> encode
                        ? URLEncoder.encode(input, StandardCharsets.UTF_8)
                        : URLDecoder.decode(input, StandardCharsets.UTF_8);
                case "html" -> encode ? htmlEncode(input) : htmlDecode(input);
                case "hex" -> encode
                        ? HexFormat.of().formatHex(input.getBytes(StandardCharsets.UTF_8))
                        : new String(HexFormat.of().parseHex(input.replaceAll("\\s+", "")), StandardCharsets.UTF_8);
                default -> throw new IllegalArgumentException("Unknown format: " + format + ". Use: base64, url, html, hex");
            };

            return (encode ? "Encode" : "Decode") + " (" + fmt + ")\n" +
                    "──────────────────\n" +
                    "Input:  " + truncate(input, 500) + "\n" +
                    "Output: " + truncate(result, 500);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "hash_text", description = "Generate a cryptographic hash of the input text. "
            + "Supports MD5, SHA-1, SHA-256, SHA-384, SHA-512. "
            + "Use this to verify checksums or generate content hashes.")
    public String hashText(
            @ToolParam(description = "The text to hash") String input,
            @ToolParam(description = "Algorithm: 'md5', 'sha1', 'sha256', 'sha384', or 'sha512'. Default: sha256.", required = false) String algorithm) {

        String algo = (algorithm != null && !algorithm.isBlank()) ? algorithm.strip().toLowerCase() : "sha256";

        // Map common names to Java MessageDigest names
        String digestAlgo = switch (algo) {
            case "md5" -> "MD5";
            case "sha1", "sha-1" -> "SHA-1";
            case "sha256", "sha-256" -> "SHA-256";
            case "sha384", "sha-384" -> "SHA-384";
            case "sha512", "sha-512" -> "SHA-512";
            default -> algo.toUpperCase();
        };

        try {
            MessageDigest digest = MessageDigest.getInstance(digestAlgo);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);

            return "Hash (" + digestAlgo + ")\n" +
                    "────────────────\n" +
                    "Input: " + truncate(input, 200) + "\n" +
                    "Hash:  " + hex + "\n" +
                    "Length: " + hash.length * 8 + " bits (" + hex.length() + " hex chars)";

        } catch (Exception e) {
            return "Error: " + e.getMessage() + ". Supported: md5, sha1, sha256, sha384, sha512";
        }
    }

    @Tool(name = "generate_uuid", description = "Generate one or more UUIDs (v4 random). "
            + "Also supports converting between UUID formats or generating short IDs.")
    public String generateUuid(
            @ToolParam(description = "Number of UUIDs to generate (1-20). Default: 1.", required = false) Integer count,
            @ToolParam(description = "Format: 'standard' (with dashes), 'compact' (no dashes), "
                    + "'base64' (URL-safe base64), or 'short' (first 8 chars). Default: standard.", required = false) String format) {

        int n = (count != null && count > 0) ? Math.min(count, 20) : 1;
        String fmt = (format != null && !format.isBlank()) ? format.strip().toLowerCase() : "standard";

        StringBuilder sb = new StringBuilder();
        sb.append("Generated UUID(s)\n");
        sb.append("──────────────────\n");

        for (int i = 0; i < n; i++) {
            UUID uuid = UUID.randomUUID();
            String formatted = switch (fmt) {
                case "standard" -> uuid.toString();
                case "compact" -> uuid.toString().replace("-", "");
                case "base64" -> Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(uuidToBytes(uuid));
                case "short" -> uuid.toString().substring(0, 8);
                default -> uuid.toString();
            };
            sb.append(n > 1 ? (i + 1) + ". " : "").append(formatted).append("\n");
        }

        return sb.toString().strip();
    }

    @Tool(name = "generate_password", description = "Generate a secure random password or passphrase with specified requirements.")
    public String generatePassword(
            @ToolParam(description = "Password length (8-128). Default: 16.", required = false) Integer length,
            @ToolParam(description = "Character sets to include as a string of flags: "
                    + "'u' (uppercase), 'l' (lowercase), 'd' (digits), 's' (symbols). "
                    + "Default: 'ulds' (all).", required = false) String charSets,
            @ToolParam(description = "Number of passwords to generate (1-10). Default: 3.", required = false) Integer count) {

        int len = (length != null && length >= 8) ? Math.min(length, 128) : 16;
        int num = (count != null && count > 0) ? Math.min(count, 10) : 3;
        String sets = (charSets != null && !charSets.isBlank()) ? charSets.toLowerCase() : "ulds";

        StringBuilder chars = new StringBuilder();
        if (sets.contains("u")) chars.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (sets.contains("l")) chars.append("abcdefghijklmnopqrstuvwxyz");
        if (sets.contains("d")) chars.append("0123456789");
        if (sets.contains("s")) chars.append("!@#$%^&*()-_=+[]{}|;:,.<>?");
        if (chars.isEmpty()) chars.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

        java.security.SecureRandom random = new java.security.SecureRandom();

        StringBuilder sb = new StringBuilder();
        sb.append("Generated Password(s)\n");
        sb.append("─────────────────────\n");
        sb.append("Length: ").append(len).append(" | Character sets: ").append(sets).append("\n\n");

        for (int i = 0; i < num; i++) {
            StringBuilder pw = new StringBuilder();
            for (int j = 0; j < len; j++) {
                pw.append(chars.charAt(random.nextInt(chars.length())));
            }
            sb.append(num > 1 ? (i + 1) + ". " : "").append(pw).append("\n");
        }

        // Entropy estimate
        double entropy = len * (Math.log(chars.length()) / Math.log(2));
        sb.append("\nEntropy: ~").append((int) entropy).append(" bits");

        return sb.toString();
    }

    // ── Helpers ──

    private String htmlEncode(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String htmlDecode(String input) {
        return input
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&apos;", "'");
    }

    private byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
            bytes[i + 8] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return bytes;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "... [truncated]";
    }
}
