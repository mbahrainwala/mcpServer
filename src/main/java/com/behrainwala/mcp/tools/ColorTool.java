package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP tool for color operations: conversion, contrast checking, palette generation,
 * blending, and accessibility suggestions.
 */
@Service
public class ColorTool {

    // ~20 common CSS named colors mapped to their hex values
    private static final Map<String, String> NAMED_COLORS = new LinkedHashMap<>();

    static {
        NAMED_COLORS.put("black", "#000000");
        NAMED_COLORS.put("white", "#FFFFFF");
        NAMED_COLORS.put("red", "#FF0000");
        NAMED_COLORS.put("green", "#008000");
        NAMED_COLORS.put("blue", "#0000FF");
        NAMED_COLORS.put("yellow", "#FFFF00");
        NAMED_COLORS.put("cyan", "#00FFFF");
        NAMED_COLORS.put("magenta", "#FF00FF");
        NAMED_COLORS.put("orange", "#FFA500");
        NAMED_COLORS.put("purple", "#800080");
        NAMED_COLORS.put("pink", "#FFC0CB");
        NAMED_COLORS.put("brown", "#A52A2A");
        NAMED_COLORS.put("gray", "#808080");
        NAMED_COLORS.put("grey", "#808080");
        NAMED_COLORS.put("navy", "#000080");
        NAMED_COLORS.put("teal", "#008080");
        NAMED_COLORS.put("maroon", "#800000");
        NAMED_COLORS.put("olive", "#808000");
        NAMED_COLORS.put("lime", "#00FF00");
        NAMED_COLORS.put("aqua", "#00FFFF");
        NAMED_COLORS.put("coral", "#FF7F50");
        NAMED_COLORS.put("salmon", "#FA8072");
    }

    // -------------------------------------------------------------------------
    // Tool: color_convert
    // -------------------------------------------------------------------------

    @Tool(name = "color_convert", description = "Convert a color between formats. "
            + "Accepts hex (#FF5733 or FF5733 or #F00), rgb(255,87,51), or hsl(11,100%,60%). "
            + "Returns the requested format or all representations.")
    public String colorConvert(
            @ToolParam(description = "Color value: hex (#FF5733), rgb(255,87,51), or hsl(11,100%,60%)") String color,
            @ToolParam(description = "Target format: 'all', 'hex', 'rgb', 'hsl', or 'cmyk'") String to_format) {

        try {
            int[] rgb = parseColorToRgb(color.strip());
            String fmt = to_format.strip().toLowerCase();
            StringBuilder sb = new StringBuilder();
            sb.append("Input: ").append(color).append("\n\n");

            String hex = rgbToHex(rgb);
            String rgbStr = String.format("rgb(%d, %d, %d)", rgb[0], rgb[1], rgb[2]);
            double[] hsl = rgbToHsl(rgb);
            String hslStr = String.format("hsl(%.0f, %.1f%%, %.1f%%)", hsl[0], hsl[1], hsl[2]);
            double[] cmyk = rgbToCmyk(rgb);
            String cmykStr = String.format("cmyk(%.1f%%, %.1f%%, %.1f%%, %.1f%%)", cmyk[0], cmyk[1], cmyk[2], cmyk[3]);

            switch (fmt) {
                case "hex" -> sb.append("HEX: ").append(hex);
                case "rgb" -> sb.append("RGB: ").append(rgbStr);
                case "hsl" -> sb.append("HSL: ").append(hslStr);
                case "cmyk" -> sb.append("CMYK: ").append(cmykStr);
                default -> {
                    sb.append("HEX:  ").append(hex).append("\n");
                    sb.append("RGB:  ").append(rgbStr).append("\n");
                    sb.append("HSL:  ").append(hslStr).append("\n");
                    sb.append("CMYK: ").append(cmykStr);
                }
            }

            String name = lookupColorName(rgb);
            if (name != null) {
                sb.append("\n\nColor name: ").append(name);
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error converting color: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Tool: color_contrast
    // -------------------------------------------------------------------------

    @Tool(name = "color_contrast", description = "Check WCAG contrast ratio between a foreground and background color. "
            + "Reports contrast ratio and pass/fail for WCAG AA and AAA at normal and large text sizes.")
    public String colorContrast(
            @ToolParam(description = "Foreground color in hex (e.g. #FFFFFF)") String foreground,
            @ToolParam(description = "Background color in hex (e.g. #000000)") String background) {

        try {
            int[] fgRgb = parseHexToRgb(foreground.strip());
            int[] bgRgb = parseHexToRgb(background.strip());

            double fgLum = relativeLuminance(fgRgb);
            double bgLum = relativeLuminance(bgRgb);

            double lighter = Math.max(fgLum, bgLum);
            double darker = Math.min(fgLum, bgLum);
            double ratio = (lighter + 0.05) / (darker + 0.05);

            return "Foreground: " + rgbToHex(fgRgb) + "\n" +
                    "Background: " + rgbToHex(bgRgb) + "\n\n" +
                    String.format("Contrast Ratio: %.2f:1%n%n", ratio) +
                    "WCAG AA\n" +
                    String.format("  Normal text (>= 4.5:1): %s%n", ratio >= 4.5 ? "PASS" : "FAIL") +
                    String.format("  Large text  (>= 3.0:1): %s%n%n", ratio >= 3.0 ? "PASS" : "FAIL") +
                    "WCAG AAA\n" +
                    String.format("  Normal text (>= 7.0:1): %s%n", ratio >= 7.0 ? "PASS" : "FAIL") +
                    String.format("  Large text  (>= 4.5:1): %s", ratio >= 4.5 ? "PASS" : "FAIL");
        } catch (Exception e) {
            return "Error checking contrast: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Tool: color_palette
    // -------------------------------------------------------------------------

    @Tool(name = "color_palette", description = "Generate a color palette from a base color using a color scheme. "
            + "Schemes: complementary, analogous, triadic, tetradic, split_complementary, monochromatic.")
    public String colorPalette(
            @ToolParam(description = "Base color in hex (e.g. #FF5733)") String base_color,
            @ToolParam(description = "Scheme: complementary, analogous, triadic, tetradic, split_complementary, or monochromatic") String scheme) {

        try {
            int[] rgb = parseHexToRgb(base_color.strip());
            double[] hsl = rgbToHsl(rgb);
            String schemeName = scheme.strip().toLowerCase().replace("-", "_");

            List<double[]> hslColors = new ArrayList<>();
            hslColors.add(hsl);
            String explanation;

            switch (schemeName) {
                case "complementary" -> {
                    explanation = "Complementary: two colors opposite on the color wheel (180 degrees apart).";
                    hslColors.add(new double[]{(hsl[0] + 180) % 360, hsl[1], hsl[2]});
                }
                case "analogous" -> {
                    explanation = "Analogous: colors adjacent on the color wheel (30 degrees apart), creating harmony.";
                    hslColors.add(new double[]{(hsl[0] + 330) % 360, hsl[1], hsl[2]});
                    hslColors.add(new double[]{(hsl[0] + 30) % 360, hsl[1], hsl[2]});
                }
                case "triadic" -> {
                    explanation = "Triadic: three colors evenly spaced on the color wheel (120 degrees apart).";
                    hslColors.add(new double[]{(hsl[0] + 120) % 360, hsl[1], hsl[2]});
                    hslColors.add(new double[]{(hsl[0] + 240) % 360, hsl[1], hsl[2]});
                }
                case "tetradic" -> {
                    explanation = "Tetradic (rectangle): four colors forming a rectangle on the color wheel (90 degree intervals).";
                    hslColors.add(new double[]{(hsl[0] + 90) % 360, hsl[1], hsl[2]});
                    hslColors.add(new double[]{(hsl[0] + 180) % 360, hsl[1], hsl[2]});
                    hslColors.add(new double[]{(hsl[0] + 270) % 360, hsl[1], hsl[2]});
                }
                case "split_complementary" -> {
                    explanation = "Split-complementary: base color plus two colors adjacent to its complement (150 and 210 degrees).";
                    hslColors.add(new double[]{(hsl[0] + 150) % 360, hsl[1], hsl[2]});
                    hslColors.add(new double[]{(hsl[0] + 210) % 360, hsl[1], hsl[2]});
                }
                case "monochromatic" -> {
                    explanation = "Monochromatic: variations in lightness and saturation of a single hue.";
                    hslColors.clear();
                    hslColors.add(new double[]{hsl[0], hsl[1], Math.min(95, hsl[2] + 30)});
                    hslColors.add(new double[]{hsl[0], hsl[1], Math.min(85, hsl[2] + 15)});
                    hslColors.add(hsl);
                    hslColors.add(new double[]{hsl[0], hsl[1], Math.max(15, hsl[2] - 15)});
                    hslColors.add(new double[]{hsl[0], hsl[1], Math.max(5, hsl[2] - 30)});
                }
                default -> {
                    return "Unknown scheme: " + scheme
                            + ". Use: complementary, analogous, triadic, tetradic, split_complementary, or monochromatic.";
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Base color: ").append(rgbToHex(rgb)).append("\n");
            sb.append("Scheme: ").append(explanation).append("\n\n");
            sb.append("Palette:\n");

            for (int i = 0; i < hslColors.size(); i++) {
                double[] h = hslColors.get(i);
                int[] r = hslToRgb(h);
                String hex = rgbToHex(r);
                sb.append(String.format("  %d. %s  rgb(%d, %d, %d)  hsl(%.0f, %.1f%%, %.1f%%)%n",
                        i + 1, hex, r[0], r[1], r[2], h[0], h[1], h[2]));
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error generating palette: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Tool: color_blend
    // -------------------------------------------------------------------------

    @Tool(name = "color_blend", description = "Blend two colors together at a given ratio and show a 5-step gradient between them.")
    public String colorBlend(
            @ToolParam(description = "First color in hex (e.g. #FF0000)") String color1,
            @ToolParam(description = "Second color in hex (e.g. #0000FF)") String color2,
            @ToolParam(description = "Blend ratio from 0.0 (all color1) to 1.0 (all color2). Default 0.5") String ratio) {

        try {
            int[] rgb1 = parseHexToRgb(color1.strip());
            int[] rgb2 = parseHexToRgb(color2.strip());

            double r;
            try {
                r = (ratio == null || ratio.isBlank()) ? 0.5 : Double.parseDouble(ratio.strip());
            } catch (NumberFormatException e) {
                r = 0.5;
            }
            r = Math.max(0.0, Math.min(1.0, r));

            int[] blended = blendRgb(rgb1, rgb2, r);
            double[] blendedHsl = rgbToHsl(blended);

            StringBuilder sb = new StringBuilder();
            sb.append("Color 1: ").append(rgbToHex(rgb1)).append("\n");
            sb.append("Color 2: ").append(rgbToHex(rgb2)).append("\n");
            sb.append(String.format("Ratio:   %.2f%n%n", r));

            sb.append("Blended result:\n");
            sb.append("  HEX: ").append(rgbToHex(blended)).append("\n");
            sb.append(String.format("  RGB: rgb(%d, %d, %d)%n", blended[0], blended[1], blended[2]));
            sb.append(String.format("  HSL: hsl(%.0f, %.1f%%, %.1f%%)%n%n", blendedHsl[0], blendedHsl[1], blendedHsl[2]));

            sb.append("5-step gradient:\n");
            for (int i = 0; i <= 4; i++) {
                double step = i / 4.0;
                int[] stepRgb = blendRgb(rgb1, rgb2, step);
                sb.append(String.format("  %d%% → %s  rgb(%d, %d, %d)%n",
                        (int) (step * 100), rgbToHex(stepRgb), stepRgb[0], stepRgb[1], stepRgb[2]));
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error blending colors: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Tool: color_accessibility
    // -------------------------------------------------------------------------

    @Tool(name = "color_accessibility", description = "Suggest accessible color pairs for a given brand color. "
            + "Returns text colors (light and dark) that meet WCAG AA, plus lighter/darker shades that maintain accessibility.")
    public String colorAccessibility(
            @ToolParam(description = "Brand color in hex (e.g. #3366CC)") String brand_color) {

        try {
            int[] rgb = parseHexToRgb(brand_color.strip());
            double lum = relativeLuminance(rgb);
            double[] hsl = rgbToHsl(rgb);

            StringBuilder sb = new StringBuilder();
            sb.append("Brand color: ").append(rgbToHex(rgb)).append("\n");
            sb.append(String.format("Relative luminance: %.4f%n%n", lum));

            // Find suitable dark text color
            sb.append("Suggested text colors on this background:\n");
            int[] darkText = findAccessibleTextColor(rgb, true);
            int[] lightText = findAccessibleTextColor(rgb, false);

            double darkRatio = contrastRatio(darkText, rgb);
            double lightRatio = contrastRatio(lightText, rgb);

            sb.append(String.format("  Dark text:  %s  (contrast %.2f:1) %s%n",
                    rgbToHex(darkText), darkRatio, darkRatio >= 4.5 ? "AA PASS" : "AA FAIL"));
            sb.append(String.format("  Light text: %s  (contrast %.2f:1) %s%n%n",
                    rgbToHex(lightText), lightRatio, lightRatio >= 4.5 ? "AA PASS" : "AA FAIL"));

            // Background shades for white text
            sb.append("Darker shades (accessible with white text):\n");
            for (int i = 1; i <= 3; i++) {
                double newL = Math.max(5, hsl[2] - i * 12);
                int[] shade = hslToRgb(new double[]{hsl[0], hsl[1], newL});
                double ratio = contrastRatio(new int[]{255, 255, 255}, shade);
                if (ratio >= 4.5) {
                    sb.append(String.format("  %s  (contrast with white: %.2f:1) AA PASS%n", rgbToHex(shade), ratio));
                }
            }

            // Background shades for black text
            sb.append("\nLighter shades (accessible with black text):\n");
            for (int i = 1; i <= 3; i++) {
                double newL = Math.min(95, hsl[2] + i * 12);
                int[] shade = hslToRgb(new double[]{hsl[0], hsl[1], newL});
                double ratio = contrastRatio(new int[]{0, 0, 0}, shade);
                if (ratio >= 4.5) {
                    sb.append(String.format("  %s  (contrast with black: %.2f:1) AA PASS%n", rgbToHex(shade), ratio));
                }
            }

            // Recommended pair
            sb.append("\nRecommended accessible pair:\n");
            if (lum > 0.179) {
                sb.append(String.format("  Background %s + Text %s (contrast %.2f:1)",
                        rgbToHex(rgb), rgbToHex(darkText), darkRatio));
            } else {
                sb.append(String.format("  Background %s + Text %s (contrast %.2f:1)",
                        rgbToHex(rgb), rgbToHex(lightText), lightRatio));
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error suggesting accessible colors: " + e.getMessage();
        }
    }

    // =========================================================================
    // Parsing helpers
    // =========================================================================

    private int[] parseColorToRgb(String color) {
        String c = color.strip();

        // Try named color
        String lower = c.toLowerCase();
        if (NAMED_COLORS.containsKey(lower)) {
            return parseHexToRgb(NAMED_COLORS.get(lower));
        }

        // Try rgb(...)
        Matcher rgbMatcher = Pattern.compile("(?i)rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)").matcher(c);
        if (rgbMatcher.matches()) {
            return new int[]{
                    Integer.parseInt(rgbMatcher.group(1)),
                    Integer.parseInt(rgbMatcher.group(2)),
                    Integer.parseInt(rgbMatcher.group(3))
            };
        }

        // Try hsl(...)
        Matcher hslMatcher = Pattern.compile("(?i)hsl\\s*\\(\\s*([\\d.]+)\\s*,\\s*([\\d.]+)%?\\s*,\\s*([\\d.]+)%?\\s*\\)").matcher(c);
        if (hslMatcher.matches()) {
            double h = Double.parseDouble(hslMatcher.group(1));
            double s = Double.parseDouble(hslMatcher.group(2));
            double l = Double.parseDouble(hslMatcher.group(3));
            return hslToRgb(new double[]{h, s, l});
        }

        // Try hex
        return parseHexToRgb(c);
    }

    private int[] parseHexToRgb(String hex) {
        String h = hex.strip().replace("#", "");

        // 3-digit shorthand
        if (h.length() == 3) {
            h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        }

        if (h.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        return new int[]{
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    // =========================================================================
    // Conversion helpers
    // =========================================================================

    private String rgbToHex(int[] rgb) {
        return String.format("#%02X%02X%02X", clamp(rgb[0]), clamp(rgb[1]), clamp(rgb[2]));
    }

    private double[] rgbToHsl(int[] rgb) {
        double r = rgb[0] / 255.0;
        double g = rgb[1] / 255.0;
        double b = rgb[2] / 255.0;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2.0;
        double h = 0, s = 0;

        if (max != min) {
            double d = max - min;
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);

            if (max == r) {
                h = ((g - b) / d + (g < b ? 6 : 0)) / 6.0;
            } else if (max == g) {
                h = ((b - r) / d + 2) / 6.0;
            } else {
                h = ((r - g) / d + 4) / 6.0;
            }
        }

        return new double[]{h * 360, s * 100, l * 100};
    }

    private int[] hslToRgb(double[] hsl) {
        double h = hsl[0] / 360.0;
        double s = hsl[1] / 100.0;
        double l = hsl[2] / 100.0;

        double r, g, b;

        if (s == 0) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3.0);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3.0);
        }

        return new int[]{
                (int) Math.round(r * 255),
                (int) Math.round(g * 255),
                (int) Math.round(b * 255)
        };
    }

    private double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }

    private double[] rgbToCmyk(int[] rgb) {
        double r = rgb[0] / 255.0;
        double g = rgb[1] / 255.0;
        double b = rgb[2] / 255.0;

        double k = 1 - Math.max(r, Math.max(g, b));
        if (k >= 1.0) {
            return new double[]{0, 0, 0, 100};
        }

        double c = (1 - r - k) / (1 - k) * 100;
        double m = (1 - g - k) / (1 - k) * 100;
        double y = (1 - b - k) / (1 - k) * 100;
        return new double[]{c, m, y, k * 100};
    }

    // =========================================================================
    // Luminance and contrast helpers
    // =========================================================================

    private double relativeLuminance(int[] rgb) {
        double r = linearize(rgb[0] / 255.0);
        double g = linearize(rgb[1] / 255.0);
        double b = linearize(rgb[2] / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private double linearize(double channel) {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private double contrastRatio(int[] fg, int[] bg) {
        double fgLum = relativeLuminance(fg);
        double bgLum = relativeLuminance(bg);
        double lighter = Math.max(fgLum, bgLum);
        double darker = Math.min(fgLum, bgLum);
        return (lighter + 0.05) / (darker + 0.05);
    }

    // =========================================================================
    // Blending helper
    // =========================================================================

    private int[] blendRgb(int[] rgb1, int[] rgb2, double ratio) {
        return new int[]{
                clamp((int) Math.round(rgb1[0] + (rgb2[0] - rgb1[0]) * ratio)),
                clamp((int) Math.round(rgb1[1] + (rgb2[1] - rgb1[1]) * ratio)),
                clamp((int) Math.round(rgb1[2] + (rgb2[2] - rgb1[2]) * ratio))
        };
    }

    // =========================================================================
    // Accessibility helpers
    // =========================================================================

    private int[] findAccessibleTextColor(int[] bgRgb, boolean dark) {
        // Start from black or white and adjust toward the middle if needed
        if (dark) {
            // Try progressively lighter darks
            for (int v = 0; v <= 100; v += 5) {
                int[] candidate = {v, v, v};
                if (contrastRatio(candidate, bgRgb) >= 4.5) {
                    return candidate;
                }
            }
            return new int[]{0, 0, 0};
        } else {
            // Try progressively darker lights
            for (int v = 255; v >= 155; v -= 5) {
                int[] candidate = {v, v, v};
                if (contrastRatio(candidate, bgRgb) >= 4.5) {
                    return candidate;
                }
            }
            return new int[]{255, 255, 255};
        }
    }

    // =========================================================================
    // Color name lookup
    // =========================================================================

    private String lookupColorName(int[] rgb) {
        String hex = rgbToHex(rgb).toUpperCase();
        for (Map.Entry<String, String> entry : NAMED_COLORS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(hex)) {
                return entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
            }
        }
        // Fuzzy match within distance of 10 per channel
        int bestDist = Integer.MAX_VALUE;
        String bestName = null;
        for (Map.Entry<String, String> entry : NAMED_COLORS.entrySet()) {
            int[] named = parseHexToRgb(entry.getValue());
            int dist = Math.abs(rgb[0] - named[0]) + Math.abs(rgb[1] - named[1]) + Math.abs(rgb[2] - named[2]);
            if (dist < bestDist && dist <= 30) {
                bestDist = dist;
                bestName = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
            }
        }
        return bestName != null ? "~" + bestName + " (approximate)" : null;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
