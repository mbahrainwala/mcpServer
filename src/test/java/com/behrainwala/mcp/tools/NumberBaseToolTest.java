package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberBaseToolTest {

    private NumberBaseTool tool;

    @BeforeEach
    void setUp() {
        tool = new NumberBaseTool();
    }

    // ── numberBaseConvert ────────────────────────────────────────────────────

    @Test
    void convert_decimalToHex() {
        String result = tool.numberBaseConvert("255", 10, 16);
        assertThat(result).containsIgnoringCase("FF");
    }

    @Test
    void convert_decimalToBinary() {
        String result = tool.numberBaseConvert("10", 10, 2);
        assertThat(result).contains("1010");
    }

    @Test
    void convert_hexToDecimal() {
        String result = tool.numberBaseConvert("0xFF", 16, 10);
        assertThat(result).contains("255");
    }

    @Test
    void convert_binaryToDecimal() {
        String result = tool.numberBaseConvert("0b1010", 2, 10);
        assertThat(result).contains("10");
    }

    @Test
    void convert_showsAllBases() {
        String result = tool.numberBaseConvert("42", 10, 2);
        assertThat(result).containsIgnoringCase("Binary").containsIgnoringCase("Hex").containsIgnoringCase("Octal");
    }

    @Test
    void convert_invalidBase_returnsError() {
        String result = tool.numberBaseConvert("10", 1, 10);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void convert_blankNumber_returnsError() {
        String result = tool.numberBaseConvert("", 10, 16);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void convert_invalidCharsForBase_returnsError() {
        String result = tool.numberBaseConvert("Z9", 10, 16);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── numberIeee754 ────────────────────────────────────────────────────────

    @Test
    void ieee754_single_positive() {
        String result = tool.numberIeee754(1.0, "single");
        assertThat(result).containsIgnoringCase("single").containsIgnoringCase("sign");
    }

    @Test
    void ieee754_double_positive() {
        String result = tool.numberIeee754(3.14, "double");
        assertThat(result).containsIgnoringCase("double").containsIgnoringCase("sign");
    }

    @Test
    void ieee754_single_zero() {
        String result = tool.numberIeee754(0.0, "single");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("zero"), s -> assertThat(s).contains("0x00000000"));
    }

    // ── numberTwosComplement ─────────────────────────────────────────────────

    @Test
    void twosComplement_positiveValue() {
        String result = tool.numberTwosComplement(5, 8);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("0000 0101"), s -> assertThat(s).contains("00000101"));
    }

    @Test
    void twosComplement_negativeValue() {
        // -1 in 8-bit = 1111 1111
        String result = tool.numberTwosComplement(-1, 8);
        assertThat(result).contains("1111");
    }

    @Test
    void twosComplement_invalidBits() {
        String result = tool.numberTwosComplement(5, 7);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void twosComplement_outOfRange() {
        String result = tool.numberTwosComplement(200, 8); // max is 127
        assertThat(result).containsIgnoringCase("error");
    }

    // ── numberBitwise ────────────────────────────────────────────────────────

    @Test
    void bitwise_and() {
        String result = tool.numberBitwise(5, 3, "and", 0);
        assertThat(result).contains("1"); // 5 & 3 = 1
    }

    @Test
    void bitwise_or() {
        String result = tool.numberBitwise(5, 3, "or", 0);
        assertThat(result).contains("7"); // 5 | 3 = 7
    }

    @Test
    void bitwise_xor() {
        String result = tool.numberBitwise(5, 3, "xor", 0);
        assertThat(result).contains("6"); // 5 ^ 3 = 6
    }

    @Test
    void bitwise_leftShift() {
        String result = tool.numberBitwise(1, 0, "left_shift", 3);
        assertThat(result).contains("8"); // 1 << 3 = 8
    }

    @Test
    void bitwise_rightShift() {
        String result = tool.numberBitwise(8, 0, "right_shift", 2);
        assertThat(result).contains("2"); // 8 >> 2 = 2
    }

    @Test
    void bitwise_unknownOp() {
        String result = tool.numberBitwise(5, 3, "multiply", 0);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("error"), s -> assertThat(s).containsIgnoringCase("unknown"));
    }

    // ── numberAsciiTable ─────────────────────────────────────────────────────

    @Test
    void ascii_charToCode() {
        String result = tool.numberAsciiTable("A", "char_to_code");
        assertThat(result).contains("65").containsIgnoringCase("41");
    }

    @Test
    void ascii_codeToChar() {
        String result = tool.numberAsciiTable("65", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_codeToChar_hex() {
        String result = tool.numberAsciiTable("0x41", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_range() {
        String result = tool.numberAsciiTable("65-70", "range");
        assertThat(result).contains("65").contains("70").contains("A");
    }
}
