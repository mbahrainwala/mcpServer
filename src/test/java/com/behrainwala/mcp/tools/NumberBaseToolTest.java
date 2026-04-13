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
        assertThat(result).contains("FF").contains("0xFF");
    }

    @Test
    void convert_decimalToBinary() {
        String result = tool.numberBaseConvert("10", 10, 2);
        assertThat(result).contains("1010").contains("Grouped:");
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
    void convert_octalToDecimal() {
        String result = tool.numberBaseConvert("0o77", 8, 10);
        assertThat(result).contains("63");
    }

    @Test
    void convert_decimalToOctal() {
        String result = tool.numberBaseConvert("63", 10, 8);
        assertThat(result).contains("0o77");
    }

    @Test
    void convert_negativeNumber() {
        String result = tool.numberBaseConvert("-10", 10, 2);
        assertThat(result).contains("-").contains("1010");
    }

    @Test
    void convert_negativeNumberToBinary_groupedWithSign() {
        String result = tool.numberBaseConvert("-10", 10, 2);
        assertThat(result).contains("Grouped:     -");
    }

    @Test
    void convert_base10ToBase10() {
        String result = tool.numberBaseConvert("42", 10, 10);
        assertThat(result).contains("42");
    }

    @Test
    void convert_nonBinaryNonOctalNonHex_resultHasNoPrefix() {
        String result = tool.numberBaseConvert("10", 10, 5);
        // The result line for base 5 should not have 0x/0b/0o prefix (but common bases section will)
        assertThat(result).contains("Result:      20 (base 5)");
    }

    @Test
    void convert_showsAllCommonBases() {
        String result = tool.numberBaseConvert("42", 10, 2);
        assertThat(result).contains("Binary:").contains("Octal:").contains("Decimal:").contains("Hex:");
    }

    @Test
    void convert_invalidFromBase_returnsError() {
        String result = tool.numberBaseConvert("10", 1, 10);
        assertThat(result).contains("Error: bases must be between 2 and 36.");
    }

    @Test
    void convert_invalidToBase_returnsError() {
        String result = tool.numberBaseConvert("10", 10, 37);
        assertThat(result).contains("Error: bases must be between 2 and 36.");
    }

    @Test
    void convert_nullNumber_returnsError() {
        String result = tool.numberBaseConvert(null, 10, 16);
        assertThat(result).contains("Error: number is required.");
    }

    @Test
    void convert_blankNumber_returnsError() {
        String result = tool.numberBaseConvert("   ", 10, 16);
        assertThat(result).contains("Error: number is required.");
    }

    @Test
    void convert_invalidCharsForBase_returnsError() {
        String result = tool.numberBaseConvert("Z9", 10, 16);
        assertThat(result).contains("Error:").contains("not a valid base-10");
    }

    @Test
    void convert_stripPrefix_onlyWhenMatchingBase() {
        // 0x prefix with base 16
        String result = tool.numberBaseConvert("0xFF", 16, 10);
        assertThat(result).contains("255");
    }

    @Test
    void convert_hexPrefix_notStripped_differentBase() {
        // 0x prefix with base 10 should fail
        String result = tool.numberBaseConvert("0xFF", 10, 16);
        assertThat(result).contains("Error:");
    }

    // ── numberIeee754 ────────────────────────────────────────────────────────

    @Test
    void ieee754_single_positive() {
        String result = tool.numberIeee754(1.0, "single");
        assertThat(result).contains("Single (32-bit)").contains("Sign:").contains("positive").contains("Normal number");
    }

    @Test
    void ieee754_single_negative() {
        String result = tool.numberIeee754(-1.0, "single");
        assertThat(result).contains("negative");
    }

    @Test
    void ieee754_single_zero() {
        String result = tool.numberIeee754(0.0, "single");
        assertThat(result).contains("Zero");
    }

    @Test
    void ieee754_single_infinity() {
        String result = tool.numberIeee754(Float.POSITIVE_INFINITY, "single");
        assertThat(result).contains("Infinity");
    }

    @Test
    void ieee754_single_nan() {
        String result = tool.numberIeee754(Float.NaN, "single");
        assertThat(result).contains("NaN");
    }

    @Test
    void ieee754_single_denormalized() {
        String result = tool.numberIeee754(Float.MIN_VALUE, "single");
        assertThat(result).contains("Denormalized");
    }

    @Test
    void ieee754_double_positive() {
        String result = tool.numberIeee754(3.14, "double");
        assertThat(result).contains("Double (64-bit)").contains("positive").contains("Normal number");
    }

    @Test
    void ieee754_double_negative() {
        String result = tool.numberIeee754(-3.14, "double");
        assertThat(result).contains("negative");
    }

    @Test
    void ieee754_double_zero() {
        String result = tool.numberIeee754(0.0, "double");
        assertThat(result).contains("Zero");
    }

    @Test
    void ieee754_double_infinity() {
        String result = tool.numberIeee754(Double.POSITIVE_INFINITY, "double");
        assertThat(result).contains("Infinity");
    }

    @Test
    void ieee754_double_nan() {
        String result = tool.numberIeee754(Double.NaN, "double");
        assertThat(result).contains("NaN");
    }

    @Test
    void ieee754_double_denormalized() {
        String result = tool.numberIeee754(Double.MIN_VALUE, "double");
        assertThat(result).contains("Denormalized");
    }

    @Test
    void ieee754_formatDouble_nan() {
        String result = tool.numberIeee754(Double.NaN, "double");
        assertThat(result).contains("Value:     NaN");
    }

    @Test
    void ieee754_formatDouble_posInfinity() {
        String result = tool.numberIeee754(Double.POSITIVE_INFINITY, "double");
        assertThat(result).contains("Value:     +Infinity");
    }

    @Test
    void ieee754_formatDouble_negInfinity() {
        String result = tool.numberIeee754(Double.NEGATIVE_INFINITY, "double");
        assertThat(result).contains("Value:     -Infinity");
    }

    @Test
    void ieee754_formatDouble_wholeNumber() {
        String result = tool.numberIeee754(42.0, "double");
        assertThat(result).contains("Value:     42");
    }

    @Test
    void ieee754_formatDouble_decimal() {
        String result = tool.numberIeee754(3.14, "double");
        assertThat(result).contains("Value:     3.14");
    }

    @Test
    void ieee754_defaultsToDouble_unknownPrecision() {
        String result = tool.numberIeee754(1.0, "unknown");
        assertThat(result).contains("Double (64-bit)");
    }

    // ── numberTwosComplement ─────────────────────────────────────────────────

    @Test
    void twosComplement_positive8bit() {
        String result = tool.numberTwosComplement(5, 8);
        assertThat(result).contains("0000 0101").contains("Two's Complement");
    }

    @Test
    void twosComplement_negative8bit() {
        String result = tool.numberTwosComplement(-1, 8);
        assertThat(result).contains("1111 1111").contains("Conversion steps (negative)");
    }

    @Test
    void twosComplement_16bit() {
        String result = tool.numberTwosComplement(256, 16);
        assertThat(result).contains("16 bits");
    }

    @Test
    void twosComplement_32bit() {
        String result = tool.numberTwosComplement(-100, 32);
        assertThat(result).contains("32 bits").contains("Conversion steps");
    }

    @Test
    void twosComplement_zero() {
        String result = tool.numberTwosComplement(0, 8);
        assertThat(result).contains("0000 0000");
    }

    @Test
    void twosComplement_maxPositive8bit() {
        String result = tool.numberTwosComplement(127, 8);
        assertThat(result).contains("0111 1111");
    }

    @Test
    void twosComplement_minNegative8bit() {
        String result = tool.numberTwosComplement(-128, 8);
        assertThat(result).contains("1000 0000");
    }

    @Test
    void twosComplement_invalidBits_7() {
        String result = tool.numberTwosComplement(5, 7);
        assertThat(result).contains("Error: bits must be 8, 16, or 32.");
    }

    @Test
    void twosComplement_outOfRange_positive() {
        String result = tool.numberTwosComplement(200, 8);
        assertThat(result).contains("Error:").contains("out of range");
    }

    @Test
    void twosComplement_outOfRange_negative() {
        String result = tool.numberTwosComplement(-200, 8);
        assertThat(result).contains("Error:").contains("out of range");
    }

    @Test
    void twosComplement_showsRange() {
        String result = tool.numberTwosComplement(0, 8);
        assertThat(result).contains("Min: -128").contains("Max: 127");
    }

    // ── numberBitwise ────────────────────────────────────────────────────────

    @Test
    void bitwise_and() {
        // 5 & 3 = 1
        String result = tool.numberBitwise(5, 3, "and", 0);
        assertThat(result).contains("Decimal: 1");
    }

    @Test
    void bitwise_or() {
        // 5 | 3 = 7
        String result = tool.numberBitwise(5, 3, "or", 0);
        assertThat(result).contains("Decimal: 7");
    }

    @Test
    void bitwise_xor() {
        // 5 ^ 3 = 6
        String result = tool.numberBitwise(5, 3, "xor", 0);
        assertThat(result).contains("Decimal: 6");
    }

    @Test
    void bitwise_not_a() {
        // ~5 = -6
        String result = tool.numberBitwise(5, 0, "not_a", 0);
        assertThat(result).contains("NOT 5").contains("Decimal: -6");
    }

    @Test
    void bitwise_not_b() {
        // ~3 = -4
        String result = tool.numberBitwise(0, 3, "not_b", 0);
        assertThat(result).contains("NOT 3").contains("Decimal: -4");
    }

    @Test
    void bitwise_leftShift() {
        // 1 << 3 = 8
        String result = tool.numberBitwise(1, 0, "left_shift", 3);
        assertThat(result).contains("Decimal: 8").contains("<<");
    }

    @Test
    void bitwise_rightShift() {
        // 8 >> 2 = 2
        String result = tool.numberBitwise(8, 0, "right_shift", 2);
        assertThat(result).contains("Decimal: 2").contains(">>");
    }

    @Test
    void bitwise_defaultShiftAmount1_whenZero() {
        // 4 << 1 = 8 (shiftAmount=0 defaults to 1)
        String result = tool.numberBitwise(4, 0, "left_shift", 0);
        assertThat(result).contains("Decimal: 8");
    }

    @Test
    void bitwise_defaultShiftAmount1_whenNegative() {
        String result = tool.numberBitwise(4, 0, "left_shift", -1);
        assertThat(result).contains("Decimal: 8");
    }

    @Test
    void bitwise_unknownOp() {
        String result = tool.numberBitwise(5, 3, "multiply", 0);
        assertThat(result).contains("Error: unknown operation");
    }

    @Test
    void bitwise_showsHex() {
        String result = tool.numberBitwise(255, 0, "not_a", 0);
        assertThat(result).contains("Hex:     0x");
    }

    // ── numberAsciiTable ─────────────────────────────────────────────────────

    @Test
    void ascii_charToCode_singleChar() {
        String result = tool.numberAsciiTable("A", "char_to_code");
        assertThat(result).contains("65").contains("0x41");
    }

    @Test
    void ascii_charToCode_multipleChars() {
        String result = tool.numberAsciiTable("AB", "char_to_code");
        assertThat(result).contains("65").contains("66");
    }

    @Test
    void ascii_codeToChar_decimal() {
        String result = tool.numberAsciiTable("65", "code_to_char");
        assertThat(result).contains("A").contains("Uppercase letter");
    }

    @Test
    void ascii_codeToChar_hex() {
        String result = tool.numberAsciiTable("0x41", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_codeToChar_hexUpperCase() {
        String result = tool.numberAsciiTable("0X42", "code_to_char");
        assertThat(result).contains("B");
    }

    @Test
    void ascii_codeToChar_binary() {
        String result = tool.numberAsciiTable("0b1000001", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_codeToChar_binaryUpperCase() {
        String result = tool.numberAsciiTable("0B1000001", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_codeToChar_octal() {
        String result = tool.numberAsciiTable("0o101", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_codeToChar_octalUpperCase() {
        String result = tool.numberAsciiTable("0O101", "code_to_char");
        assertThat(result).contains("A");
    }

    @Test
    void ascii_codeToChar_controlChar() {
        String result = tool.numberAsciiTable("0", "code_to_char");
        assertThat(result).contains("NUL").contains("Control character");
    }

    @Test
    void ascii_codeToChar_space() {
        String result = tool.numberAsciiTable("32", "code_to_char");
        assertThat(result).contains("Space");
    }

    @Test
    void ascii_codeToChar_digit() {
        String result = tool.numberAsciiTable("48", "code_to_char");
        assertThat(result).contains("Digit");
    }

    @Test
    void ascii_codeToChar_lowercase() {
        String result = tool.numberAsciiTable("97", "code_to_char");
        assertThat(result).contains("Lowercase letter");
    }

    @Test
    void ascii_codeToChar_del() {
        String result = tool.numberAsciiTable("127", "code_to_char");
        assertThat(result).contains("DEL").contains("Delete (control)");
    }

    @Test
    void ascii_codeToChar_punctuation() {
        String result = tool.numberAsciiTable("33", "code_to_char");
        assertThat(result).contains("Punctuation / Symbol");
    }

    @Test
    void ascii_codeToChar_aboveAscii_noCategory() {
        String result = tool.numberAsciiTable("200", "code_to_char");
        assertThat(result).doesNotContain("Category:");
    }

    @Test
    void ascii_range_specific() {
        String result = tool.numberAsciiTable("65-70", "range");
        assertThat(result).contains("65").contains("70").contains("A").contains("F");
    }

    @Test
    void ascii_range_printable() {
        String result = tool.numberAsciiTable("printable", "range");
        assertThat(result).contains("32").contains("126");
    }

    @Test
    void ascii_range_default_fullAscii() {
        String result = tool.numberAsciiTable("all", "range");
        assertThat(result).contains("0").contains("127");
    }

    @Test
    void ascii_range_tooLarge_returnsError() {
        String result = tool.numberAsciiTable("0-500", "range");
        assertThat(result).contains("Error: range too large");
    }

    @Test
    void ascii_unknownMode_returnsError() {
        String result = tool.numberAsciiTable("A", "unknown_mode");
        assertThat(result).contains("Error: unknown mode");
    }

    @Test
    void ascii_invalidInput_returnsError() {
        String result = tool.numberAsciiTable("not_a_number", "code_to_char");
        assertThat(result).contains("Error:");
    }

    // ── displayChar edge cases ───────────────────────────────────────────────

    @Test
    void ascii_charToCode_controlChars() {
        // BEL = 7, BS = 8, TAB = 9, LF = 10, CR = 13, ESC = 27
        String result = tool.numberAsciiTable("7", "code_to_char");
        assertThat(result).contains("BEL");
    }

    @Test
    void ascii_charToCode_tab() {
        String result = tool.numberAsciiTable("9", "code_to_char");
        assertThat(result).contains("TAB");
    }

    @Test
    void ascii_charToCode_lf() {
        String result = tool.numberAsciiTable("10", "code_to_char");
        assertThat(result).contains("LF");
    }

    @Test
    void ascii_charToCode_cr() {
        String result = tool.numberAsciiTable("13", "code_to_char");
        assertThat(result).contains("CR");
    }

    @Test
    void ascii_charToCode_esc() {
        String result = tool.numberAsciiTable("27", "code_to_char");
        assertThat(result).contains("ESC");
    }

    @Test
    void ascii_charToCode_bs() {
        String result = tool.numberAsciiTable("8", "code_to_char");
        assertThat(result).contains("BS");
    }

    @Test
    void ascii_charToCode_otherControlChar() {
        // 1 = ^A
        String result = tool.numberAsciiTable("1", "code_to_char");
        assertThat(result).contains("^A");
    }
}
