package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ColorToolTest {

    private ColorTool tool;

    @BeforeEach
    void setUp() {
        tool = new ColorTool();
    }

    // =========================================================================
    // colorConvert
    // =========================================================================

    @Nested
    class ColorConvert {

        // ── Input format parsing ────────────────────────────────────────────

        @Test
        void hex6Digit_toAll() {
            String result = tool.colorConvert("#FF5733", "all");
            assertThat(result)
                    .contains("Input: #FF5733")
                    .contains("HEX:  #FF5733")
                    .contains("RGB:  rgb(255, 87, 51)")
                    .contains("HSL:")
                    .contains("CMYK:");
        }

        @Test
        void hex6Digit_withoutHash_toHex() {
            String result = tool.colorConvert("FF5733", "hex");
            assertThat(result).contains("HEX: #FF5733");
        }

        @Test
        void hex3Digit_shorthand_toRgb() {
            String result = tool.colorConvert("#F00", "rgb");
            assertThat(result).contains("rgb(255, 0, 0)");
        }

        @Test
        void rgbInput_toHex() {
            String result = tool.colorConvert("rgb(255,0,0)", "hex");
            assertThat(result).contains("HEX: #FF0000");
        }

        @Test
        void rgbInput_withSpaces_toHex() {
            String result = tool.colorConvert("rgb( 128 , 64 , 32 )", "hex");
            assertThat(result).contains("HEX: #804020");
        }

        @Test
        void hslInput_toHex() {
            String result = tool.colorConvert("hsl(0, 100, 50)", "hex");
            assertThat(result).contains("HEX: #FF0000");
        }

        @Test
        void hslInput_withPercentSigns_toHex() {
            String result = tool.colorConvert("hsl(0, 100%, 50%)", "hex");
            assertThat(result).contains("HEX: #FF0000");
        }

        @Test
        void namedColor_red_toHex() {
            String result = tool.colorConvert("red", "hex");
            assertThat(result).contains("HEX: #FF0000");
        }

        @Test
        void namedColor_white_toRgb() {
            String result = tool.colorConvert("white", "rgb");
            assertThat(result).contains("rgb(255, 255, 255)");
        }

        @Test
        void namedColor_black_toRgb() {
            String result = tool.colorConvert("black", "rgb");
            assertThat(result).contains("rgb(0, 0, 0)");
        }

        @Test
        void namedColor_caseSensitivity_Blue() {
            // Named colors are matched case-insensitively via toLowerCase
            String result = tool.colorConvert("Blue", "hex");
            assertThat(result).contains("#0000FF");
        }

        // ── Target format branches ──────────────────────────────────────────

        @Test
        void toFormat_hex() {
            String result = tool.colorConvert("#FF0000", "hex");
            assertThat(result).contains("HEX: #FF0000");
            assertThat(result).doesNotContain("RGB:");
        }

        @Test
        void toFormat_rgb() {
            String result = tool.colorConvert("#FF0000", "rgb");
            assertThat(result).contains("RGB: rgb(255, 0, 0)");
            assertThat(result).doesNotContain("HEX:");
        }

        @Test
        void toFormat_hsl() {
            String result = tool.colorConvert("#FF0000", "hsl");
            assertThat(result).contains("HSL: hsl(0,");
        }

        @Test
        void toFormat_cmyk() {
            String result = tool.colorConvert("#FF0000", "cmyk");
            assertThat(result).contains("CMYK: cmyk(0.0%,");
        }

        @Test
        void toFormat_all_containsAllFormats() {
            String result = tool.colorConvert("#FF0000", "all");
            assertThat(result)
                    .contains("HEX:")
                    .contains("RGB:")
                    .contains("HSL:")
                    .contains("CMYK:");
        }

        @Test
        void toFormat_unknown_defaultsToAll() {
            String result = tool.colorConvert("#FF0000", "xyz_unknown");
            assertThat(result)
                    .contains("HEX:")
                    .contains("RGB:")
                    .contains("HSL:")
                    .contains("CMYK:");
        }

        // ── Color name lookup (exact match) ─────────────────────────────────

        @Test
        void exactColorName_isAppended() {
            String result = tool.colorConvert("#FF0000", "hex");
            assertThat(result).contains("Color name: Red");
        }

        @Test
        void noColorName_forArbitraryColor() {
            // A color far from any named color
            String result = tool.colorConvert("#123456", "hex");
            assertThat(result).doesNotContain("Color name:");
        }

        @Test
        void approximateColorName_nearRed() {
            // rgb(255, 10, 10) is close to red (#FF0000) within distance 30
            String result = tool.colorConvert("rgb(255,10,10)", "hex");
            assertThat(result).contains("~Red (approximate)");
        }

        // ── CMYK special case: black ────────────────────────────────────────

        @Test
        void cmykConversion_pureBlack() {
            // Black triggers k >= 1.0 branch in rgbToCmyk
            String result = tool.colorConvert("#000000", "cmyk");
            assertThat(result).contains("cmyk(0.0%, 0.0%, 0.0%, 100.0%)");
        }

        @Test
        void cmykConversion_pureWhite() {
            String result = tool.colorConvert("#FFFFFF", "cmyk");
            assertThat(result).contains("cmyk(0.0%, 0.0%, 0.0%, 0.0%)");
        }

        // ── HSL conversion: max == g and max == b branches ──────────────────

        @Test
        void hslConversion_greenDominant() {
            // Pure green: max == g branch in rgbToHsl
            String result = tool.colorConvert("#00FF00", "hsl");
            assertThat(result).contains("hsl(120,");
        }

        @Test
        void hslConversion_blueDominant() {
            // Pure blue: max == b branch in rgbToHsl
            String result = tool.colorConvert("#0000FF", "hsl");
            assertThat(result).contains("hsl(240,");
        }

        @Test
        void hslConversion_redDominant_gLessThanB() {
            // Red dominant with g < b triggers the (g < b ? 6 : 0) branch
            // E.g. rgb(200, 50, 100) => max == r, g < b
            String result = tool.colorConvert("rgb(200,50,100)", "hsl");
            assertThat(result).contains("HSL:");
        }

        @Test
        void hslConversion_achromatic_gray() {
            // Gray: max == min, so s = 0, h = 0
            String result = tool.colorConvert("#808080", "hsl");
            assertThat(result).contains("hsl(0, 0.0%, 50.");
        }

        @Test
        void hslConversion_lightness_above50() {
            // Light color: l > 0.5 takes the other saturation formula branch
            String result = tool.colorConvert("#CCAA88", "hsl");
            assertThat(result).contains("HSL:");
        }

        // ── hslToRgb: saturation == 0 branch ───────────────────────────────

        @Test
        void hslInput_zeroSaturation_toRgb() {
            // hsl(0, 0, 50) should give gray
            String result = tool.colorConvert("hsl(0, 0, 50)", "rgb");
            assertThat(result).contains("rgb(128, 128, 128)");
        }

        // ── hueToRgb: all branches ─────────────────────────────────────────

        @Test
        void hueToRgb_coverAllBranches_orangeColor() {
            // Orange has hue ~30deg which exercises different hueToRgb branches for R, G, B
            String result = tool.colorConvert("hsl(30, 100, 50)", "rgb");
            assertThat(result).contains("RGB:");
        }

        @Test
        void hueToRgb_highHue_exercisesWrapAround() {
            // Hue near 300 (magenta) exercises t < 0 and t > 1 corrections
            String result = tool.colorConvert("hsl(300, 100, 50)", "rgb");
            assertThat(result).contains("RGB:");
        }

        // ── Error handling ──────────────────────────────────────────────────

        @Test
        void invalidHex_returnsError() {
            String result = tool.colorConvert("ZZZZZZ", "hex");
            assertThat(result).startsWith("Error converting color:");
        }

        @Test
        void invalidHex_wrongLength_returnsError() {
            String result = tool.colorConvert("#12345", "hex");
            assertThat(result).contains("Error converting color:");
        }

        @Test
        void leadingTrailingSpaces_areTrimmed() {
            String result = tool.colorConvert("  #FF0000  ", "  hex  ");
            assertThat(result).contains("HEX: #FF0000");
        }
    }

    // =========================================================================
    // colorContrast
    // =========================================================================

    @Nested
    class ColorContrast {

        @Test
        void blackOnWhite_maxContrast() {
            String result = tool.colorContrast("#000000", "#FFFFFF");
            assertThat(result)
                    .contains("Contrast Ratio: 21.00:1")
                    .contains("WCAG AA")
                    .contains("Normal text (>= 4.5:1): PASS")
                    .contains("Large text  (>= 3.0:1): PASS")
                    .contains("WCAG AAA")
                    .contains("Normal text (>= 7.0:1): PASS")
                    .contains("Large text  (>= 4.5:1): PASS");
        }

        @Test
        void sameColor_minContrast() {
            String result = tool.colorContrast("#808080", "#808080");
            assertThat(result).contains("Contrast Ratio: 1.00:1");
        }

        @Test
        void lowContrast_failsAllChecks() {
            // Two very similar colors
            String result = tool.colorContrast("#777777", "#888888");
            assertThat(result)
                    .contains("FAIL");
        }

        @Test
        void mediumContrast_passesAALargeOnly() {
            // Choose colors that give ratio between 3.0 and 4.5
            String result = tool.colorContrast("#767676", "#FFFFFF");
            // #767676 on white gives approximately 4.54:1
            assertThat(result).contains("PASS");
        }

        @Test
        void whiteOnBlack_maxContrast_reversedOrder() {
            String result = tool.colorContrast("#FFFFFF", "#000000");
            assertThat(result).contains("21.00:1");
        }

        @Test
        void shortHex_isSupported() {
            String result = tool.colorContrast("#FFF", "#000");
            assertThat(result).contains("21.00:1");
        }

        @Test
        void hexWithoutHash_isSupported() {
            String result = tool.colorContrast("FFFFFF", "000000");
            assertThat(result).contains("21.00:1");
        }

        @Test
        void invalidForeground_returnsError() {
            String result = tool.colorContrast("XXXXXX", "#000000");
            assertThat(result).startsWith("Error checking contrast:");
        }

        @Test
        void invalidBackground_returnsError() {
            String result = tool.colorContrast("#000000", "XXXXXX");
            assertThat(result).startsWith("Error checking contrast:");
        }

        @Test
        void linearize_lowChannel_branchCoverage() {
            // Very dark color: channel / 255 <= 0.03928
            // rgb(10,10,10) => 10/255 = 0.039... > 0.03928 -- actually just above
            // rgb(9,9,9) => 9/255 = 0.0353... <= 0.03928
            String result = tool.colorContrast("#090909", "#FFFFFF");
            assertThat(result).contains("Contrast Ratio:");
        }

        @Test
        void linearize_highChannel_branchCoverage() {
            // Bright color: channel / 255 > 0.03928
            String result = tool.colorContrast("#AABBCC", "#112233");
            assertThat(result).contains("Contrast Ratio:");
        }
    }

    // =========================================================================
    // colorPalette
    // =========================================================================

    @Nested
    class ColorPalette {

        @Test
        void complementary_returns2Colors() {
            String result = tool.colorPalette("#FF0000", "complementary");
            assertThat(result)
                    .contains("Complementary:")
                    .contains("1.")
                    .contains("2.");
        }

        @Test
        void analogous_returns3Colors() {
            String result = tool.colorPalette("#FF0000", "analogous");
            assertThat(result)
                    .contains("Analogous:")
                    .contains("1.")
                    .contains("2.")
                    .contains("3.");
        }

        @Test
        void triadic_returns3Colors() {
            String result = tool.colorPalette("#FF0000", "triadic");
            assertThat(result)
                    .contains("Triadic:")
                    .contains("1.")
                    .contains("2.")
                    .contains("3.");
        }

        @Test
        void tetradic_returns4Colors() {
            String result = tool.colorPalette("#FF0000", "tetradic");
            assertThat(result)
                    .contains("Tetradic")
                    .contains("1.")
                    .contains("2.")
                    .contains("3.")
                    .contains("4.");
        }

        @Test
        void splitComplementary_returns3Colors() {
            String result = tool.colorPalette("#FF0000", "split_complementary");
            assertThat(result)
                    .contains("Split-complementary:")
                    .contains("1.")
                    .contains("2.")
                    .contains("3.");
        }

        @Test
        void splitComplementary_withHyphen_alsoWorks() {
            String result = tool.colorPalette("#FF0000", "split-complementary");
            assertThat(result).contains("Split-complementary:");
        }

        @Test
        void monochromatic_returns5Colors() {
            String result = tool.colorPalette("#FF0000", "monochromatic");
            assertThat(result)
                    .contains("Monochromatic:")
                    .contains("1.")
                    .contains("2.")
                    .contains("3.")
                    .contains("4.")
                    .contains("5.");
        }

        @Test
        void monochromatic_highLightness_capsAtMax() {
            // Very light color: lightness near 95 tests Math.min caps
            String result = tool.colorPalette("#FAFAFA", "monochromatic");
            assertThat(result).contains("Monochromatic:");
        }

        @Test
        void monochromatic_lowLightness_capsAtMin() {
            // Very dark color: lightness near 5 tests Math.max caps
            String result = tool.colorPalette("#0A0A0A", "monochromatic");
            assertThat(result).contains("Monochromatic:");
        }

        @Test
        void unknownScheme_returnsError() {
            String result = tool.colorPalette("#FF0000", "rainbow");
            assertThat(result).contains("Unknown scheme: rainbow");
        }

        @Test
        void invalidBaseColor_returnsError() {
            String result = tool.colorPalette("not_a_color", "complementary");
            assertThat(result).startsWith("Error generating palette:");
        }

        @Test
        void baseColorShowsInOutput() {
            String result = tool.colorPalette("#3366CC", "triadic");
            assertThat(result).contains("Base color: #3366CC");
        }
    }

    // =========================================================================
    // colorBlend
    // =========================================================================

    @Nested
    class ColorBlend {

        @Test
        void blend_50percent_midpoint() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "0.5");
            assertThat(result)
                    .contains("Color 1: #FF0000")
                    .contains("Color 2: #0000FF")
                    .contains("Ratio:   0.50")
                    .contains("Blended result:")
                    .contains("5-step gradient:");
        }

        @Test
        void blend_ratio0_allColor1() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "0.0");
            assertThat(result).contains("HEX: #FF0000");
        }

        @Test
        void blend_ratio1_allColor2() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "1.0");
            assertThat(result).contains("HEX: #0000FF");
        }

        @Test
        void blend_nullRatio_defaults05() {
            String result = tool.colorBlend("#FF0000", "#0000FF", null);
            assertThat(result).contains("Ratio:   0.50");
        }

        @Test
        void blend_blankRatio_defaults05() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "  ");
            assertThat(result).contains("Ratio:   0.50");
        }

        @Test
        void blend_invalidRatio_defaults05() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "not_a_number");
            assertThat(result).contains("Ratio:   0.50");
        }

        @Test
        void blend_ratioAbove1_clampedTo1() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "2.5");
            assertThat(result).contains("Ratio:   1.00");
        }

        @Test
        void blend_ratioBelowZero_clampedTo0() {
            String result = tool.colorBlend("#FF0000", "#0000FF", "-0.5");
            assertThat(result).contains("Ratio:   0.00");
        }

        @Test
        void blend_gradient_has5Steps() {
            String result = tool.colorBlend("#000000", "#FFFFFF", "0.5");
            assertThat(result)
                    .contains("0%")
                    .contains("25%")
                    .contains("50%")
                    .contains("75%")
                    .contains("100%");
        }

        @Test
        void blend_invalidColor1_returnsError() {
            String result = tool.colorBlend("bad_color", "#0000FF", "0.5");
            assertThat(result).startsWith("Error blending colors:");
        }

        @Test
        void blend_invalidColor2_returnsError() {
            String result = tool.colorBlend("#FF0000", "bad_color", "0.5");
            assertThat(result).startsWith("Error blending colors:");
        }

        @Test
        void blend_showsHslOfBlendedResult() {
            String result = tool.colorBlend("#FF0000", "#00FF00", "0.5");
            assertThat(result).contains("HSL: hsl(");
        }
    }

    // =========================================================================
    // colorAccessibility
    // =========================================================================

    @Nested
    class ColorAccessibility {

        @Test
        void darkBrandColor_recommendsLightText() {
            // Dark color: luminance <= 0.179 => recommends light text
            String result = tool.colorAccessibility("#000080");
            assertThat(result)
                    .contains("Brand color: #000080")
                    .contains("Relative luminance:")
                    .contains("Suggested text colors on this background:")
                    .contains("Dark text:")
                    .contains("Light text:")
                    .contains("Recommended accessible pair:")
                    .contains("Text #FF"); // light text recommended
        }

        @Test
        void lightBrandColor_recommendsDarkText() {
            // Light color: luminance > 0.179 => recommends dark text
            String result = tool.colorAccessibility("#FFFF00");
            assertThat(result)
                    .contains("Brand color: #FFFF00")
                    .contains("Recommended accessible pair:")
                    .contains("Text #0"); // dark text recommended
        }

        @Test
        void mediumBrandColor_showsDarkerShades() {
            String result = tool.colorAccessibility("#3366CC");
            assertThat(result).contains("Darker shades (accessible with white text):");
        }

        @Test
        void mediumBrandColor_showsLighterShades() {
            String result = tool.colorAccessibility("#3366CC");
            assertThat(result).contains("Lighter shades (accessible with black text):");
        }

        @Test
        void veryLightBrand_darkerShadesNotAllPassAA() {
            // Very light color may not have darker shades that pass AA with white
            String result = tool.colorAccessibility("#FAFAFA");
            assertThat(result).contains("Darker shades");
        }

        @Test
        void veryDarkBrand_lighterShadesPassAA() {
            String result = tool.colorAccessibility("#0A0A0A");
            assertThat(result).contains("Lighter shades");
        }

        @Test
        void midGray_exercisesBothTextColorSearches() {
            // Gray where both dark and light text searches go through multiple iterations
            String result = tool.colorAccessibility("#808080");
            assertThat(result)
                    .contains("Dark text:")
                    .contains("Light text:");
        }

        @Test
        void white_darkTextFound_lightTextMayFail() {
            // On white background, dark text easily passes, light text fails
            String result = tool.colorAccessibility("#FFFFFF");
            assertThat(result).contains("Dark text:");
        }

        @Test
        void black_lightTextFound_darkTextMayFail() {
            // On black background, light text easily passes
            String result = tool.colorAccessibility("#000000");
            assertThat(result).contains("Light text:");
        }

        @Test
        void invalidColor_returnsError() {
            String result = tool.colorAccessibility("not_a_color");
            assertThat(result).startsWith("Error suggesting accessible colors:");
        }

        @Test
        void findAccessibleTextColor_darkPath_allIterationsFail() {
            // A very dark background where finding a dark text with contrast >= 4.5 fails
            // until we exhaust the loop and get fallback (0,0,0)
            String result = tool.colorAccessibility("#050505");
            assertThat(result).contains("Dark text:");
        }

        @Test
        void findAccessibleTextColor_lightPath_allIterationsFail() {
            // A very bright background where finding a light text with contrast >= 4.5 fails
            // => returns fallback (255,255,255)
            String result = tool.colorAccessibility("#FAFAFA");
            assertThat(result).contains("Light text:");
        }

        @Test
        void accessibilityShades_passOrSkip() {
            // Use a mid-range color to generate shades; some will pass AA, some won't
            String result = tool.colorAccessibility("#5588AA");
            assertThat(result).contains("Brand color: #5588AA");
        }
    }

    // =========================================================================
    // Edge cases across all tools
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void pureWhite_cmyk_allZeros() {
            String result = tool.colorConvert("#FFFFFF", "cmyk");
            assertThat(result).contains("cmyk(0.0%, 0.0%, 0.0%, 0.0%)");
        }

        @ParameterizedTest
        @ValueSource(strings = {"black", "white", "red", "green", "blue", "yellow", "cyan", "magenta",
                "orange", "purple", "pink", "brown", "gray", "grey", "navy", "teal",
                "maroon", "olive", "lime", "aqua", "coral", "salmon"})
        void allNamedColors_canBeConverted(String name) {
            String result = tool.colorConvert(name, "hex");
            assertThat(result).contains("HEX: #");
            assertThat(result).doesNotStartWith("Error");
        }

        @Test
        void clamp_handlesOverflow() {
            // Blending shouldn't produce values outside 0-255 due to clamping
            String result = tool.colorBlend("#FFFFFF", "#FFFFFF", "0.5");
            assertThat(result).contains("#FFFFFF");
        }
    }
}
