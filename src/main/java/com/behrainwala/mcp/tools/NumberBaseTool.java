package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for number base conversions, bitwise operations, IEEE 754 representation,
 * two's complement, and ASCII/Unicode lookups.
 */
@Service
public class NumberBaseTool {

    // ───────────────────────────────────────────────────────────────────────────
    // 1. Base conversion
    // ───────────────────────────────────────────────────────────────────────────

    @Tool(name = "number_base_convert", description = "Convert a number between bases (2-36). "
            + "Supports binary, octal, decimal, hexadecimal and arbitrary bases up to 36. "
            + "Handles negative numbers. Output uses prefix notation (0b, 0o, 0x) and groups binary digits.")
    public String numberBaseConvert(
            @ToolParam(description = "The number to convert, as a string (e.g. '255', '0xFF', '0b1010')") String number,
            @ToolParam(description = "Source base (2-36)") int fromBase,
            @ToolParam(description = "Target base (2-36)") int toBase) {

        if (fromBase < 2 || fromBase > 36 || toBase < 2 || toBase > 36) {
            return "Error: bases must be between 2 and 36.";
        }
        if (number == null || number.isBlank()) {
            return "Error: number is required.";
        }

        try {
            String cleaned = number.strip();
            boolean negative = cleaned.startsWith("-");
            if (negative) {
                cleaned = cleaned.substring(1);
            }

            // Strip common prefixes if they match the declared fromBase
            cleaned = stripPrefix(cleaned, fromBase);

            long value = Long.parseLong(cleaned, fromBase);
            String converted = Long.toString(value, toBase).toUpperCase();

            if (negative) {
                converted = "-" + converted;
            }

            String prefixed = addPrefix(converted, toBase);
            String display = (toBase == 2) ? groupBinary(converted.replace("-", "")) : converted;
            if (negative && toBase == 2) {
                display = "-" + display;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Number Base Conversion\n");
            sb.append("══════════════════════\n");
            sb.append(String.format("  Input:       %s (base %d)\n", number.strip(), fromBase));
            sb.append(String.format("  Decimal:     %s%d\n", negative ? "-" : "", value));
            sb.append(String.format("  Result:      %s (base %d)\n", prefixed, toBase));
            if (toBase == 2) {
                sb.append(String.format("  Grouped:     %s%s\n", negative ? "-" : "", display));
            }
            sb.append("\n");

            // Show in all common bases for convenience
            sb.append("  All common bases:\n");
            sb.append(String.format("    Binary:    %s0b%s\n", negative ? "-" : "", groupBinary(Long.toBinaryString(value))));
            sb.append(String.format("    Octal:     %s0o%s\n", negative ? "-" : "", Long.toOctalString(value)));
            sb.append(String.format("    Decimal:   %s%d\n", negative ? "-" : "", value));
            sb.append(String.format("    Hex:       %s0x%s\n", negative ? "-" : "", Long.toHexString(value).toUpperCase()));

            return sb.toString();

        } catch (NumberFormatException e) {
            return "Error: '" + number + "' is not a valid base-" + fromBase + " number.";
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 2. IEEE 754
    // ───────────────────────────────────────────────────────────────────────────

    @Tool(name = "number_ieee754", description = "Show the IEEE 754 floating-point representation of a number. "
            + "Displays sign bit, exponent bits, mantissa bits, hex representation, "
            + "and detects special values (NaN, Infinity, denormalized).")
    public String numberIeee754(
            @ToolParam(description = "The number to represent") double number,
            @ToolParam(description = "Precision: 'single' (32-bit) or 'double' (64-bit)") String precision) {

        String prec = precision.strip().toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("IEEE 754 Representation\n");
        sb.append("═══════════════════════\n");
        sb.append(String.format("  Value:     %s\n", formatDouble(number)));

        if ("single".equals(prec)) {
            float f = (float) number;
            int bits = Float.floatToRawIntBits(f);

            int sign = (bits >>> 31) & 1;
            int exponent = (bits >>> 23) & 0xFF;
            int mantissa = bits & 0x7FFFFF;

            String signStr = String.valueOf(sign);
            String expStr = padLeft(Integer.toBinaryString(exponent), 8);
            String manStr = padLeft(Integer.toBinaryString(mantissa), 23);
            String hexStr = String.format("0x%08X", bits);

            sb.append("  Precision: Single (32-bit)\n\n");
            sb.append("  Bit layout:\n");
            sb.append("  ┌──────┬──────────┬─────────────────────────┐\n");
            sb.append("  │ Sign │ Exponent │        Mantissa         │\n");
            sb.append("  │  1b  │   8b     │         23b             │\n");
            sb.append("  ├──────┼──────────┼─────────────────────────┤\n");
            sb.append(String.format("  │  %s   │ %s │ %s │\n", signStr, expStr, manStr));
            sb.append("  └──────┴──────────┴─────────────────────────┘\n\n");

            sb.append(String.format("  Sign:      %d (%s)\n", sign, sign == 0 ? "positive" : "negative"));
            sb.append(String.format("  Exponent:  %s (biased: %d, unbiased: %d)\n", expStr, exponent, exponent - 127));
            sb.append(String.format("  Mantissa:  %s\n", manStr));
            sb.append(String.format("  Hex:       %s\n", hexStr));
            sb.append(String.format("  Float val: %.9g\n", f));

            appendSpecialSingle(sb, exponent, mantissa);

        } else {
            // double precision
            long bits = Double.doubleToRawLongBits(number);

            int sign = (int) ((bits >>> 63) & 1);
            int exponent = (int) ((bits >>> 52) & 0x7FF);
            long mantissa = bits & 0xFFFFFFFFFFFFFL;

            String signStr = String.valueOf(sign);
            String expStr = padLeft(Integer.toBinaryString(exponent), 11);
            String manStr = padLeft(Long.toBinaryString(mantissa), 52);
            String hexStr = String.format("0x%016X", bits);

            sb.append("  Precision: Double (64-bit)\n\n");
            sb.append("  Bit layout:\n");
            sb.append("  ┌──────┬─────────────┬────────────────────────────────────────────────────────┐\n");
            sb.append("  │ Sign │  Exponent   │                      Mantissa                         │\n");
            sb.append("  │  1b  │    11b      │                        52b                            │\n");
            sb.append("  ├──────┼─────────────┼────────────────────────────────────────────────────────┤\n");
            sb.append(String.format("  │  %s   │ %s │ %s │\n", signStr, expStr, manStr));
            sb.append("  └──────┴─────────────┴────────────────────────────────────────────────────────┘\n\n");

            sb.append(String.format("  Sign:      %d (%s)\n", sign, sign == 0 ? "positive" : "negative"));
            sb.append(String.format("  Exponent:  %s (biased: %d, unbiased: %d)\n", expStr, exponent, exponent - 1023));
            sb.append(String.format("  Mantissa:  %s\n", manStr));
            sb.append(String.format("  Hex:       %s\n", hexStr));
            sb.append(String.format("  Double val: %.17g\n", number));

            appendSpecialDouble(sb, exponent, mantissa);
        }

        return sb.toString();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 3. Two's complement
    // ───────────────────────────────────────────────────────────────────────────

    @Tool(name = "number_twos_complement", description = "Show the two's complement representation of an integer. "
            + "Displays binary and hex, conversion steps for negatives, and the valid range for the bit width.")
    public String numberTwosComplement(
            @ToolParam(description = "The integer value") int number,
            @ToolParam(description = "Bit width: 8, 16, or 32") int bits) {

        if (bits != 8 && bits != 16 && bits != 32) {
            return "Error: bits must be 8, 16, or 32.";
        }

        long mask = (1L << bits) - 1;
        long minVal = -(1L << (bits - 1));
        long maxVal = (1L << (bits - 1)) - 1;

        if (number < minVal || number > maxVal) {
            return String.format("Error: %d is out of range for %d-bit two's complement [%d, %d].",
                    number, bits, minVal, maxVal);
        }

        long representation = number & mask;
        String binary = padLeft(Long.toBinaryString(representation), bits);
        String hex = padLeft(Long.toHexString(representation).toUpperCase(), bits / 4);

        StringBuilder sb = new StringBuilder();
        sb.append("Two's Complement Representation\n");
        sb.append("═══════════════════════════════\n");
        sb.append(String.format("  Value:     %d\n", number));
        sb.append(String.format("  Bit width: %d bits\n\n", bits));

        sb.append(String.format("  Binary:    %s\n", groupBits(binary, 4)));
        sb.append(String.format("  Hex:       0x%s\n", hex));
        sb.append(String.format("  Unsigned:  %d\n\n", representation));

        if (number < 0) {
            sb.append("  Conversion steps (negative):\n");
            long absVal = Math.abs((long) number);
            String absBin = padLeft(Long.toBinaryString(absVal), bits);
            sb.append(String.format("    1. Absolute value:   %s  (%d)\n", groupBits(absBin, 4), absVal));

            long inverted = (~absVal) & mask;
            String invertedBin = padLeft(Long.toBinaryString(inverted), bits);
            sb.append(String.format("    2. Invert bits:      %s  (one's complement)\n", groupBits(invertedBin, 4)));

            sb.append(String.format("    3. Add 1:            %s  (two's complement)\n", groupBits(binary, 4)));
            sb.append("\n");
        }

        sb.append(String.format("  Range for %d-bit signed:\n", bits));
        sb.append(String.format("    Min: %d\n", minVal));
        sb.append(String.format("    Max: %d\n", maxVal));

        return sb.toString();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 4. Bitwise operations
    // ───────────────────────────────────────────────────────────────────────────

    @Tool(name = "number_bitwise", description = "Perform bitwise operations on two integers. "
            + "Operations: and, or, xor, not_a, not_b, left_shift, right_shift. "
            + "Shows result in decimal, binary, and hex with visually aligned bit representation.")
    public String numberBitwise(
            @ToolParam(description = "First operand") int a,
            @ToolParam(description = "Second operand") int b,
            @ToolParam(description = "Operation: 'and', 'or', 'xor', 'not_a', 'not_b', 'left_shift', 'right_shift'") String operation,
            @ToolParam(description = "Shift amount (used only for shift operations, defaults to 1)") int shiftAmount) {

        if (shiftAmount <= 0) {
            shiftAmount = 1;
        }

        String op = operation.strip().toLowerCase();

        int result;
        String symbol;

        switch (op) {
            case "and" -> { result = a & b; symbol = "&"; }
            case "or" -> { result = a | b; symbol = "|"; }
            case "xor" -> { result = a ^ b; symbol = "^"; }
            case "not_a" -> { result = ~a; symbol = "~"; }
            case "not_b" -> { result = ~b; symbol = "~"; }
            case "left_shift" -> { result = a << shiftAmount; symbol = "<<"; }
            case "right_shift" -> { result = a >> shiftAmount; symbol = ">>"; }
            default -> { return "Error: unknown operation '" + operation + "'. Use: and, or, xor, not_a, not_b, left_shift, right_shift"; }
        }

        // Determine display width: enough bits to show all values
        int displayBits = 32;

        String aBin = padLeft(toBinaryStr(a, displayBits), displayBits);
        String bBin = padLeft(toBinaryStr(b, displayBits), displayBits);
        String resBin = padLeft(toBinaryStr(result, displayBits), displayBits);

        StringBuilder sb = new StringBuilder();
        sb.append("Bitwise Operation\n");
        sb.append("═════════════════\n\n");

        switch (op) {
            case "not_a" -> {
                sb.append(String.format("  ~A  (NOT %d)\n\n", a));
                sb.append(String.format("    A:      %s  (%d)\n", groupBits(aBin, 8), a));
                sb.append(String.format("            %s\n", "─".repeat(displayBits + displayBits / 8 - 1)));
                sb.append(String.format("    ~A:     %s  (%d)\n", groupBits(resBin, 8), result));
            }
            case "not_b" -> {
                sb.append(String.format("  ~B  (NOT %d)\n\n", b));
                sb.append(String.format("    B:      %s  (%d)\n", groupBits(bBin, 8), b));
                sb.append(String.format("            %s\n", "─".repeat(displayBits + displayBits / 8 - 1)));
                sb.append(String.format("    ~B:     %s  (%d)\n", groupBits(resBin, 8), result));
            }
            case "left_shift", "right_shift" -> {
                sb.append(String.format("  %d %s %d\n\n", a, symbol, shiftAmount));
                sb.append(String.format("    A:      %s  (%d)\n", groupBits(aBin, 8), a));
                sb.append(String.format("            %s\n", "─".repeat(displayBits + displayBits / 8 - 1)));
                sb.append(String.format("    Result: %s  (%d)\n", groupBits(resBin, 8), result));
            }
            default -> {
                sb.append(String.format("  %d %s %d\n\n", a, symbol, b));
                sb.append(String.format("    A:      %s  (%d)\n", groupBits(aBin, 8), a));
                sb.append(String.format("    B:      %s  (%d)\n", groupBits(bBin, 8), b));
                sb.append(String.format("            %s\n", "─".repeat(displayBits + displayBits / 8 - 1)));
                sb.append(String.format("    Result: %s  (%d)\n", groupBits(resBin, 8), result));
            }
        }

        sb.append("\n");
        sb.append(String.format("  Decimal: %d\n", result));
        sb.append(String.format("  Binary:  %s\n", groupBits(resBin, 4)));
        sb.append(String.format("  Hex:     0x%s\n", padLeft(Integer.toHexString(result).toUpperCase(), 8)));

        return sb.toString();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 5. ASCII / Unicode lookup
    // ───────────────────────────────────────────────────────────────────────────

    @Tool(name = "number_ascii_table", description = "ASCII and Unicode character lookup. "
            + "Can convert characters to codes, codes to characters, or display a formatted table of a range. "
            + "Input can be a character, decimal number, or hex (e.g. '0x41').")
    public String numberAsciiTable(
            @ToolParam(description = "Input: a character (e.g. 'A'), decimal code (e.g. '65'), hex code (e.g. '0x41'), "
                    + "or for range mode a range like '32-126' or 'printable'") String input,
            @ToolParam(description = "Mode: 'char_to_code', 'code_to_char', or 'range'") String mode) {

        String m = mode.strip().toLowerCase();
        String in = input.strip();

        try {
            return switch (m) {
                case "char_to_code" -> charToCode(in);
                case "code_to_char" -> codeToChar(in);
                case "range" -> asciiRange(in);
                default -> "Error: unknown mode '" + mode + "'. Use: char_to_code, code_to_char, range";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ───────────────────────────────────────────────────────────────────────────

    private String charToCode(String input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Character → Code\n");
        sb.append("════════════════\n\n");
        sb.append(String.format("  %-6s  %-8s  %-8s  %-8s  %s\n", "Char", "Dec", "Hex", "Oct", "Binary"));
        sb.append(String.format("  %-6s  %-8s  %-8s  %-8s  %s\n", "────", "───", "───", "───", "──────"));

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            sb.append(String.format("  %-6s  %-8d  0x%-6s  0%-7s  %s\n",
                    displayChar(c),
                    (int) c,
                    padLeft(Integer.toHexString(c).toUpperCase(), 2),
                    Integer.toOctalString(c),
                    groupBits(padLeft(Integer.toBinaryString(c), 8), 4)));
        }

        return sb.toString();
    }

    private String codeToChar(String input) {
        int code;
        if (input.startsWith("0x") || input.startsWith("0X")) {
            code = Integer.parseInt(input.substring(2), 16);
        } else if (input.startsWith("0b") || input.startsWith("0B")) {
            code = Integer.parseInt(input.substring(2), 2);
        } else if (input.startsWith("0o") || input.startsWith("0O")) {
            code = Integer.parseInt(input.substring(2), 8);
        } else {
            code = Integer.parseInt(input);
        }

        char c = (char) code;

        StringBuilder sb = new StringBuilder();
        sb.append("Code → Character\n");
        sb.append("════════════════\n\n");
        sb.append(String.format("  Input:     %s\n", input));
        sb.append(String.format("  Character: %s\n", displayChar(c)));
        sb.append(String.format("  Decimal:   %d\n", code));
        sb.append(String.format("  Hex:       0x%s\n", padLeft(Integer.toHexString(code).toUpperCase(), 2)));
        sb.append(String.format("  Octal:     0%s\n", Integer.toOctalString(code)));
        sb.append(String.format("  Binary:    %s\n", groupBits(padLeft(Integer.toBinaryString(code), 8), 4)));

        if (code < 128) {
            sb.append(String.format("  Category:  %s\n", asciiCategory(code)));
        }

        return sb.toString();
    }

    private String asciiRange(String input) {
        int start, end;

        if ("printable".equalsIgnoreCase(input)) {
            start = 32;
            end = 126;
        } else if (input.contains("-")) {
            String[] parts = input.split("-");
            start = Integer.parseInt(parts[0].strip());
            end = Integer.parseInt(parts[1].strip());
        } else {
            start = 0;
            end = 127;
        }

        if (end - start > 256) {
            return "Error: range too large (max 256 characters).";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ASCII Table [%d – %d]\n", start, end));
        sb.append("════════════════════\n\n");
        sb.append(String.format("  %-5s  %-6s  %-6s  %s\n", "Dec", "Hex", "Oct", "Char"));
        sb.append(String.format("  %-5s  %-6s  %-6s  %s\n", "───", "───", "───", "────"));

        for (int i = start; i <= end; i++) {
            sb.append(String.format("  %-5d  0x%-4s  0%-5s  %s\n",
                    i,
                    padLeft(Integer.toHexString(i).toUpperCase(), 2),
                    Integer.toOctalString(i),
                    displayChar((char) i)));
        }

        return sb.toString();
    }

    private String displayChar(char c) {
        if (c < 32) {
            return switch (c) {
                case 0 -> "NUL";
                case 7 -> "BEL";
                case 8 -> "BS";
                case 9 -> "TAB";
                case 10 -> "LF";
                case 13 -> "CR";
                case 27 -> "ESC";
                default -> String.format("^%c", c + 64);
            };
        }
        if (c == 127) return "DEL";
        return String.valueOf(c);
    }

    private String asciiCategory(int code) {
        if (code < 32) return "Control character";
        if (code == 32) return "Space";
        if (code >= 48 && code <= 57) return "Digit";
        if (code >= 65 && code <= 90) return "Uppercase letter";
        if (code >= 97 && code <= 122) return "Lowercase letter";
        if (code == 127) return "Delete (control)";
        return "Punctuation / Symbol";
    }

    private String stripPrefix(String s, int base) {
        String lower = s.toLowerCase();
        if (base == 16 && lower.startsWith("0x")) return s.substring(2);
        if (base == 2 && lower.startsWith("0b")) return s.substring(2);
        if (base == 8 && lower.startsWith("0o")) return s.substring(2);
        return s;
    }

    private String addPrefix(String value, int base) {
        return switch (base) {
            case 2 -> "0b" + value;
            case 8 -> "0o" + value;
            case 16 -> "0x" + value;
            default -> value;
        };
    }

    private String groupBinary(String binary) {
        return groupBits(binary, 4);
    }

    private String groupBits(String bits, int groupSize) {
        // Pad to a multiple of groupSize
        int padded = ((bits.length() + groupSize - 1) / groupSize) * groupSize;
        bits = padLeft(bits, padded);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length(); i++) {
            if (i > 0 && i % groupSize == 0) sb.append(' ');
            sb.append(bits.charAt(i));
        }
        return sb.toString();
    }

    private String padLeft(String s, int width) {
        if (s.length() >= width) return s;
        return "0".repeat(width - s.length()) + s;
    }

    private String toBinaryStr(int value, int bits) {
        long mask = (1L << bits) - 1;
        long unsigned = value & mask;
        return padLeft(Long.toBinaryString(unsigned), bits);
    }

    private void appendSpecialSingle(StringBuilder sb, int exponent, int mantissa) {
        sb.append("\n  Special value detection:\n");
        if (exponent == 0xFF && mantissa != 0) {
            sb.append("    ⚠ NaN (Not a Number)\n");
        } else if (exponent == 0xFF && mantissa == 0) {
            sb.append("    ⚠ Infinity\n");
        } else if (exponent == 0 && mantissa != 0) {
            sb.append("    ⚠ Denormalized number (subnormal)\n");
        } else if (exponent == 0 && mantissa == 0) {
            sb.append("    Zero\n");
        } else {
            sb.append("    Normal number\n");
        }
    }

    private void appendSpecialDouble(StringBuilder sb, int exponent, long mantissa) {
        sb.append("\n  Special value detection:\n");
        if (exponent == 0x7FF && mantissa != 0) {
            sb.append("    ⚠ NaN (Not a Number)\n");
        } else if (exponent == 0x7FF && mantissa == 0) {
            sb.append("    ⚠ Infinity\n");
        } else if (exponent == 0 && mantissa != 0) {
            sb.append("    ⚠ Denormalized number (subnormal)\n");
        } else if (exponent == 0 && mantissa == 0) {
            sb.append("    Zero\n");
        } else {
            sb.append("    Normal number\n");
        }
    }

    private String formatDouble(double d) {
        if (Double.isNaN(d)) return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "+Infinity" : "-Infinity";
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
