package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncodingToolTest {

    private EncodingTool tool;

    @BeforeEach
    void setUp() {
        tool = new EncodingTool();
    }

    // ── Base64 ───────────────────────────────────────────────────────────────

    @Test
    void base64_encode() {
        String result = tool.encodeDecode("hello", "base64", "encode");
        assertThat(result).contains("aGVsbG8=");
    }

    @Test
    void base64_decode() {
        String result = tool.encodeDecode("aGVsbG8=", "base64", "decode");
        assertThat(result).contains("hello");
    }

    @Test
    void base64_roundtrip() {
        String encoded = tool.encodeDecode("Hello World", "base64", "encode");
        // Extract the output value from the formatted response
        assertThat(encoded).contains("SGVsbG8gV29ybGQ=");
        String decoded = tool.encodeDecode("SGVsbG8gV29ybGQ=", "base64", "decode");
        assertThat(decoded).contains("Hello World");
    }

    // ── URL ──────────────────────────────────────────────────────────────────

    @Test
    void url_encode() {
        String result = tool.encodeDecode("hello world", "url", "encode");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("hello+world"), s -> assertThat(s).contains("hello%20world"));
    }

    @Test
    void url_decode() {
        String result = tool.encodeDecode("hello+world", "url", "decode");
        assertThat(result).contains("hello world");
    }

    // ── HTML ─────────────────────────────────────────────────────────────────

    @Test
    void html_encode() {
        String result = tool.encodeDecode("<b>hi</b>", "html", "encode");
        assertThat(result).contains("&lt;b&gt;");
    }

    @Test
    void html_decode() {
        String result = tool.encodeDecode("&lt;b&gt;hi&lt;/b&gt;", "html", "decode");
        assertThat(result).contains("<b>hi</b>");
    }

    // ── Hex ──────────────────────────────────────────────────────────────────

    @Test
    void hex_encode() {
        // 'A' = 0x41
        String result = tool.encodeDecode("A", "hex", "encode");
        assertThat(result).containsIgnoringCase("41");
    }

    @Test
    void hex_decode() {
        String result = tool.encodeDecode("41", "hex", "decode");
        assertThat(result).contains("A");
    }

    // ── Hash ─────────────────────────────────────────────────────────────────

    @Test
    void hash_sha256_knownValue() {
        // SHA-256 of "hello" is known
        String result = tool.hashText("hello", "sha256");
        assertThat(result).containsIgnoringCase("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void hash_md5() {
        String result = tool.hashText("hello", "md5");
        assertThat(result).containsIgnoringCase("5d41402abc4b2a76b9719d911017c592");
    }

    @Test
    void hash_defaultAlgorithmIsSha256() {
        String result = tool.hashText("hello", null);
        assertThat(result).containsIgnoringCase("SHA-256");
    }

    // ── UUID ─────────────────────────────────────────────────────────────────

    @Test
    void uuid_standard_format() {
        String result = tool.generateUuid(1, "standard");
        // UUID v4 has 36 chars with dashes: 8-4-4-4-12
        assertThat(result).containsPattern("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    void uuid_compact_hasNoDashes() {
        String result = tool.generateUuid(1, "compact");
        // Should contain 32 hex chars without dashes
        assertThat(result).containsPattern("[0-9a-fA-F]{32}");
    }

    @Test
    void uuid_multiple() {
        String result = tool.generateUuid(3, "standard");
        // Three numbered entries
        assertThat(result).contains("1.").contains("2.").contains("3.");
    }

    // ── Password ─────────────────────────────────────────────────────────────

    @Test
    void password_defaultLength16() {
        String result = tool.generatePassword(null, null, 1);
        assertThat(result).containsIgnoringCase("Length: 16");
    }

    @Test
    void password_customLength() {
        String result = tool.generatePassword(24, "l", 1);
        assertThat(result).containsIgnoringCase("Length: 24");
    }

    @Test
    void password_entropyIncluded() {
        String result = tool.generatePassword(16, "ulds", 1);
        assertThat(result).containsIgnoringCase("Entropy");
    }

    @Test
    void encodeDecode_unknownFormat_returnsError() {
        String result = tool.encodeDecode("test", "rot13", "encode");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("unknown"));
    }
}
