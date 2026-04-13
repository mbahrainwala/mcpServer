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
        assertThat(result).contains("aGVsbG8=").contains("Encode (base64)");
    }

    @Test
    void base64_decode() {
        String result = tool.encodeDecode("aGVsbG8=", "base64", "decode");
        assertThat(result).contains("hello").contains("Decode (base64)");
    }

    @Test
    void base64_roundtrip() {
        String encoded = tool.encodeDecode("Hello World", "base64", "encode");
        assertThat(encoded).contains("SGVsbG8gV29ybGQ=");
        String decoded = tool.encodeDecode("SGVsbG8gV29ybGQ=", "base64", "decode");
        assertThat(decoded).contains("Hello World");
    }

    // ── URL ──────────────────────────────────────────────────────────────────

    @Test
    void url_encode() {
        String result = tool.encodeDecode("hello world&foo=bar", "url", "encode");
        assertThat(result).contains("hello+world").contains("Encode (url)");
    }

    @Test
    void url_decode() {
        String result = tool.encodeDecode("hello+world%26foo%3Dbar", "url", "decode");
        assertThat(result).contains("hello world&foo=bar");
    }

    // ── HTML ─────────────────────────────────────────────────────────────────

    @Test
    void html_encode_allEntities() {
        String result = tool.encodeDecode("<b>\"Hello\" & 'World'</b>", "html", "encode");
        assertThat(result)
                .contains("&lt;b&gt;")
                .contains("&quot;Hello&quot;")
                .contains("&amp;")
                .contains("&#39;World&#39;");
    }

    @Test
    void html_decode_allEntities() {
        String result = tool.encodeDecode("&lt;b&gt;&quot;Hello&quot; &amp; &#39;World&#39;&lt;/b&gt;", "html", "decode");
        assertThat(result).contains("<b>\"Hello\" & 'World'</b>");
    }

    @Test
    void html_decode_hexEntity() {
        String result = tool.encodeDecode("&#x27;", "html", "decode");
        assertThat(result).contains("'");
    }

    @Test
    void html_decode_aposEntity() {
        String result = tool.encodeDecode("&apos;", "html", "decode");
        assertThat(result).contains("'");
    }

    // ── Hex ──────────────────────────────────────────────────────────────────

    @Test
    void hex_encode() {
        String result = tool.encodeDecode("AB", "hex", "encode");
        assertThat(result).contains("4142");
    }

    @Test
    void hex_decode() {
        String result = tool.encodeDecode("4142", "hex", "decode");
        assertThat(result).contains("AB");
    }

    @Test
    void hex_decode_withWhitespace() {
        String result = tool.encodeDecode("41 42", "hex", "decode");
        assertThat(result).contains("AB");
    }

    // ── Unknown format ──────────────────────────────────────────────────────

    @Test
    void encodeDecode_unknownFormat_returnsError() {
        String result = tool.encodeDecode("test", "rot13", "encode");
        assertThat(result).startsWith("Error:");
    }

    // ── Direction parsing ────────────────────────────────────────────────────

    @Test
    void encodeDecode_directionStartsWithE_encodes() {
        String result = tool.encodeDecode("hello", "base64", "Encode");
        assertThat(result).contains("Encode");
    }

    @Test
    void encodeDecode_directionStartsWithD_decodes() {
        String result = tool.encodeDecode("aGVsbG8=", "base64", "  Decode ");
        assertThat(result).contains("Decode");
    }

    // ── Invalid base64 decode ────────────────────────────────────────────────

    @Test
    void base64_decode_invalid_returnsError() {
        String result = tool.encodeDecode("!!!not-base64!!!", "base64", "decode");
        assertThat(result).contains("Error:");
    }

    // ── Truncation ───────────────────────────────────────────────────────────

    @Test
    void encodeDecode_longInput_truncated() {
        String longInput = "A".repeat(600);
        String result = tool.encodeDecode(longInput, "base64", "encode");
        assertThat(result).contains("truncated");
    }

    // ── Hash ─────────────────────────────────────────────────────────────────

    @Test
    void hash_sha256_knownValue() {
        String result = tool.hashText("hello", "sha256");
        assertThat(result).contains("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void hash_md5() {
        String result = tool.hashText("hello", "md5");
        assertThat(result).contains("5d41402abc4b2a76b9719d911017c592");
    }

    @Test
    void hash_sha1() {
        String result = tool.hashText("hello", "sha1");
        assertThat(result).contains("SHA-1").contains("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d");
    }

    @Test
    void hash_sha1_hyphen() {
        String result = tool.hashText("hello", "sha-1");
        assertThat(result).contains("SHA-1");
    }

    @Test
    void hash_sha256_hyphen() {
        String result = tool.hashText("hello", "sha-256");
        assertThat(result).contains("SHA-256");
    }

    @Test
    void hash_sha384() {
        String result = tool.hashText("hello", "sha384");
        assertThat(result).contains("SHA-384");
    }

    @Test
    void hash_sha384_hyphen() {
        String result = tool.hashText("hello", "sha-384");
        assertThat(result).contains("SHA-384");
    }

    @Test
    void hash_sha512() {
        String result = tool.hashText("hello", "sha512");
        assertThat(result).contains("SHA-512");
    }

    @Test
    void hash_sha512_hyphen() {
        String result = tool.hashText("hello", "sha-512");
        assertThat(result).contains("SHA-512");
    }

    @Test
    void hash_nullAlgo_defaultsSha256() {
        String result = tool.hashText("hello", null);
        assertThat(result).contains("SHA-256");
    }

    @Test
    void hash_blankAlgo_defaultsSha256() {
        String result = tool.hashText("hello", "   ");
        assertThat(result).contains("SHA-256");
    }

    @Test
    void hash_unknownAlgo_returnsError() {
        String result = tool.hashText("hello", "FAKE-ALGO");
        assertThat(result).contains("Error:");
    }

    @Test
    void hash_outputContainsBitLength() {
        String result = tool.hashText("hello", "sha256");
        assertThat(result).contains("256 bits");
    }

    // ── UUID ─────────────────────────────────────────────────────────────────

    @Test
    void uuid_standard_format() {
        String result = tool.generateUuid(1, "standard");
        assertThat(result).containsPattern("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    void uuid_compact_noDashes() {
        String result = tool.generateUuid(1, "compact");
        assertThat(result).containsPattern("[0-9a-fA-F]{32}");
    }

    @Test
    void uuid_base64_format() {
        String result = tool.generateUuid(1, "base64");
        assertThat(result).contains("Generated UUID");
    }

    @Test
    void uuid_short_format() {
        String result = tool.generateUuid(1, "short");
        assertThat(result).containsPattern("[0-9a-fA-F]{8}");
    }

    @Test
    void uuid_defaultFormat_isStandard() {
        String result = tool.generateUuid(1, null);
        assertThat(result).containsPattern("[0-9a-fA-F]{8}-");
    }

    @Test
    void uuid_unknownFormat_defaultsToStandard() {
        String result = tool.generateUuid(1, "unknown_format");
        assertThat(result).containsPattern("[0-9a-fA-F]{8}-");
    }

    @Test
    void uuid_multiple() {
        String result = tool.generateUuid(3, "standard");
        assertThat(result).contains("1.").contains("2.").contains("3.");
    }

    @Test
    void uuid_nullCount_defaultsTo1() {
        String result = tool.generateUuid(null, "standard");
        assertThat(result).doesNotContain("1.");  // no numbered prefix for single
    }

    @Test
    void uuid_zeroCount_defaultsTo1() {
        String result = tool.generateUuid(0, "standard");
        assertThat(result).doesNotContain("2.");
    }

    @Test
    void uuid_maxCount_cappedAt20() {
        String result = tool.generateUuid(25, "standard");
        assertThat(result).contains("20.").doesNotContain("21.");
    }

    @Test
    void uuid_blankFormat_defaultsToStandard() {
        String result = tool.generateUuid(1, "  ");
        assertThat(result).containsPattern("[0-9a-fA-F]{8}-");
    }

    // ── Password ─────────────────────────────────────────────────────────────

    @Test
    void password_defaultLength16() {
        String result = tool.generatePassword(null, null, 1);
        assertThat(result).contains("Length: 16");
    }

    @Test
    void password_customLength() {
        String result = tool.generatePassword(24, "l", 1);
        assertThat(result).contains("Length: 24");
    }

    @Test
    void password_minLength8() {
        String result = tool.generatePassword(4, null, 1);
        // length < 8 should be treated as 16 (default)
        assertThat(result).contains("Length: 16");
    }

    @Test
    void password_maxLength128() {
        String result = tool.generatePassword(200, null, 1);
        assertThat(result).contains("Length: 128");
    }

    @Test
    void password_uppercaseOnly() {
        String result = tool.generatePassword(16, "u", 1);
        assertThat(result).contains("Character sets: u");
    }

    @Test
    void password_digitsOnly() {
        String result = tool.generatePassword(16, "d", 1);
        assertThat(result).contains("Character sets: d");
    }

    @Test
    void password_symbolsOnly() {
        String result = tool.generatePassword(16, "s", 1);
        assertThat(result).contains("Character sets: s");
    }

    @Test
    void password_emptyCharSets_usesDefault() {
        String result = tool.generatePassword(16, "xyz", 1);
        // No u/l/d/s match, so chars is empty, falls back to alphanumeric
        assertThat(result).contains("Entropy");
    }

    @Test
    void password_nullCharSets_usesAll() {
        String result = tool.generatePassword(16, null, 1);
        assertThat(result).contains("Character sets: ulds");
    }

    @Test
    void password_blankCharSets_usesAll() {
        String result = tool.generatePassword(16, "  ", 1);
        assertThat(result).contains("Character sets: ulds");
    }

    @Test
    void password_multiplePasswords() {
        String result = tool.generatePassword(16, null, 5);
        assertThat(result).contains("1.").contains("5.");
    }

    @Test
    void password_maxCount10() {
        String result = tool.generatePassword(16, null, 20);
        assertThat(result).contains("10.").doesNotContain("11.");
    }

    @Test
    void password_nullCount_defaultsTo3() {
        String result = tool.generatePassword(16, null, null);
        assertThat(result).contains("3.");
    }

    @Test
    void password_zeroCount_defaultsTo3() {
        String result = tool.generatePassword(16, null, 0);
        assertThat(result).contains("3.");
    }

    @Test
    void password_entropyIncluded() {
        String result = tool.generatePassword(16, "ulds", 1);
        assertThat(result).contains("Entropy: ~");
    }
}
