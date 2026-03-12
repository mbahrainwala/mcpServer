package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

/**
 * MCP tool for unit conversions across common measurement categories.
 */
@Service
public class UnitConverterTool {

    // All conversion factors are relative to a base unit per category
    // Temperature is handled separately due to non-linear conversions

    private static final Map<String, Map<String, Double>> CONVERSIONS = Map.ofEntries(
            // Length: base unit = meters
            Map.entry("length", Map.ofEntries(
                    Map.entry("mm", 0.001), Map.entry("millimeter", 0.001), Map.entry("millimeters", 0.001),
                    Map.entry("cm", 0.01), Map.entry("centimeter", 0.01), Map.entry("centimeters", 0.01),
                    Map.entry("m", 1.0), Map.entry("meter", 1.0), Map.entry("meters", 1.0),
                    Map.entry("km", 1000.0), Map.entry("kilometer", 1000.0), Map.entry("kilometers", 1000.0),
                    Map.entry("in", 0.0254), Map.entry("inch", 0.0254), Map.entry("inches", 0.0254),
                    Map.entry("ft", 0.3048), Map.entry("foot", 0.3048), Map.entry("feet", 0.3048),
                    Map.entry("yd", 0.9144), Map.entry("yard", 0.9144), Map.entry("yards", 0.9144),
                    Map.entry("mi", 1609.344), Map.entry("mile", 1609.344), Map.entry("miles", 1609.344),
                    Map.entry("nmi", 1852.0), Map.entry("nautical mile", 1852.0)
            )),
            // Weight: base unit = grams
            Map.entry("weight", Map.ofEntries(
                    Map.entry("mg", 0.001), Map.entry("milligram", 0.001), Map.entry("milligrams", 0.001),
                    Map.entry("g", 1.0), Map.entry("gram", 1.0), Map.entry("grams", 1.0),
                    Map.entry("kg", 1000.0), Map.entry("kilogram", 1000.0), Map.entry("kilograms", 1000.0),
                    Map.entry("oz", 28.3495), Map.entry("ounce", 28.3495), Map.entry("ounces", 28.3495),
                    Map.entry("lb", 453.592), Map.entry("pound", 453.592), Map.entry("pounds", 453.592),
                    Map.entry("ton", 907185.0), Map.entry("tons", 907185.0),
                    Map.entry("tonne", 1_000_000.0), Map.entry("tonnes", 1_000_000.0),
                    Map.entry("st", 6350.29), Map.entry("stone", 6350.29)
            )),
            // Volume: base unit = liters
            Map.entry("volume", Map.ofEntries(
                    Map.entry("ml", 0.001), Map.entry("milliliter", 0.001), Map.entry("milliliters", 0.001),
                    Map.entry("l", 1.0), Map.entry("liter", 1.0), Map.entry("liters", 1.0),
                    Map.entry("gal", 3.78541), Map.entry("gallon", 3.78541), Map.entry("gallons", 3.78541),
                    Map.entry("qt", 0.946353), Map.entry("quart", 0.946353), Map.entry("quarts", 0.946353),
                    Map.entry("pt", 0.473176), Map.entry("pint", 0.473176), Map.entry("pints", 0.473176),
                    Map.entry("cup", 0.236588), Map.entry("cups", 0.236588),
                    Map.entry("floz", 0.0295735), Map.entry("fl oz", 0.0295735), Map.entry("fluid ounce", 0.0295735),
                    Map.entry("tbsp", 0.0147868), Map.entry("tablespoon", 0.0147868),
                    Map.entry("tsp", 0.00492892), Map.entry("teaspoon", 0.00492892)
            )),
            // Speed: base unit = m/s
            Map.entry("speed", Map.ofEntries(
                    Map.entry("m/s", 1.0), Map.entry("mps", 1.0),
                    Map.entry("km/h", 0.277778), Map.entry("kph", 0.277778), Map.entry("kmh", 0.277778),
                    Map.entry("mph", 0.44704), Map.entry("mi/h", 0.44704),
                    Map.entry("knot", 0.514444), Map.entry("knots", 0.514444), Map.entry("kn", 0.514444),
                    Map.entry("ft/s", 0.3048), Map.entry("fps", 0.3048)
            )),
            // Area: base unit = square meters
            Map.entry("area", Map.ofEntries(
                    Map.entry("mm2", 1e-6), Map.entry("sq mm", 1e-6),
                    Map.entry("cm2", 1e-4), Map.entry("sq cm", 1e-4),
                    Map.entry("m2", 1.0), Map.entry("sq m", 1.0), Map.entry("square meter", 1.0),
                    Map.entry("km2", 1e6), Map.entry("sq km", 1e6),
                    Map.entry("sq in", 0.00064516), Map.entry("sq ft", 0.092903),
                    Map.entry("sq yd", 0.836127), Map.entry("sq mi", 2.59e6),
                    Map.entry("acre", 4046.86), Map.entry("acres", 4046.86),
                    Map.entry("hectare", 10000.0), Map.entry("hectares", 10000.0), Map.entry("ha", 10000.0)
            )),
            // Time: base unit = seconds
            Map.entry("time", Map.ofEntries(
                    Map.entry("ms", 0.001), Map.entry("millisecond", 0.001), Map.entry("milliseconds", 0.001),
                    Map.entry("s", 1.0), Map.entry("sec", 1.0), Map.entry("second", 1.0), Map.entry("seconds", 1.0),
                    Map.entry("min", 60.0), Map.entry("minute", 60.0), Map.entry("minutes", 60.0),
                    Map.entry("h", 3600.0), Map.entry("hr", 3600.0), Map.entry("hour", 3600.0), Map.entry("hours", 3600.0),
                    Map.entry("day", 86400.0), Map.entry("days", 86400.0),
                    Map.entry("week", 604800.0), Map.entry("weeks", 604800.0),
                    Map.entry("month", 2592000.0), Map.entry("months", 2592000.0),
                    Map.entry("year", 31536000.0), Map.entry("years", 31536000.0)
            )),
            // Digital storage: base unit = bytes
            Map.entry("data", Map.ofEntries(
                    Map.entry("b", 1.0), Map.entry("byte", 1.0), Map.entry("bytes", 1.0),
                    Map.entry("kb", 1024.0), Map.entry("kilobyte", 1024.0),
                    Map.entry("mb", 1048576.0), Map.entry("megabyte", 1048576.0),
                    Map.entry("gb", 1073741824.0), Map.entry("gigabyte", 1073741824.0),
                    Map.entry("tb", 1099511627776.0), Map.entry("terabyte", 1099511627776.0),
                    Map.entry("pb", 1125899906842624.0), Map.entry("petabyte", 1125899906842624.0)
            ))
    );

    @Tool(name = "convert_units", description = "Convert a value from one unit to another. "
            + "Supports: length (km, mi, ft, m, in, cm, etc.), weight (kg, lb, oz, g, etc.), "
            + "volume (l, gal, cup, ml, etc.), temperature (C, F, K), speed (km/h, mph, m/s, knots), "
            + "area (sq m, acres, hectares, sq ft), time (seconds, minutes, hours, days, years), "
            + "data (bytes, KB, MB, GB, TB).")
    public String convert(
            @ToolParam(description = "The numeric value to convert") double value,
            @ToolParam(description = "The source unit (e.g. 'km', 'pounds', 'celsius', 'GB')") String fromUnit,
            @ToolParam(description = "The target unit (e.g. 'miles', 'kg', 'fahrenheit', 'MB')") String toUnit) {

        String from = fromUnit.strip().toLowerCase();
        String to = toUnit.strip().toLowerCase();

        // Handle temperature separately (non-linear conversions)
        if (isTemperatureUnit(from) && isTemperatureUnit(to)) {
            return convertTemperature(value, from, to);
        }

        // Find which category contains both units
        for (Map.Entry<String, Map<String, Double>> category : CONVERSIONS.entrySet()) {
            Map<String, Double> units = category.getValue();
            if (units.containsKey(from) && units.containsKey(to)) {
                double baseValue = value * units.get(from);
                double result = baseValue / units.get(to);

                BigDecimal bd = BigDecimal.valueOf(result).round(new MathContext(10)).stripTrailingZeros();

                return String.format("Unit Conversion\n--------------\n%.6g %s = %s %s",
                        value, fromUnit, bd.toPlainString(), toUnit);
            }
        }

        return "Error: Cannot convert from '" + fromUnit + "' to '" + toUnit + "'. "
                + "Either the units are not recognized or they belong to different categories. "
                + "Supported categories: length, weight, volume, temperature, speed, area, time, data.";
    }

    private boolean isTemperatureUnit(String unit) {
        return unit.matches("(c|f|k|celsius|fahrenheit|kelvin|°c|°f|°k)");
    }

    private String convertTemperature(double value, String from, String to) {
        // Normalize unit names
        from = normalizeTemp(from);
        to = normalizeTemp(to);

        if (from.equals(to)) {
            return String.format("%.2f %s = %.2f %s (same unit)", value, from, value, to);
        }

        // Convert to Celsius first, then to target
        double celsius = switch (from) {
            case "c" -> value;
            case "f" -> (value - 32) * 5.0 / 9.0;
            case "k" -> value - 273.15;
            default -> throw new IllegalArgumentException("Unknown temperature unit: " + from);
        };

        double result = switch (to) {
            case "c" -> celsius;
            case "f" -> celsius * 9.0 / 5.0 + 32;
            case "k" -> celsius + 273.15;
            default -> throw new IllegalArgumentException("Unknown temperature unit: " + to);
        };

        String fromLabel = tempLabel(from);
        String toLabel = tempLabel(to);

        return String.format("Temperature Conversion\n---------------------\n%.2f %s = %.2f %s",
                value, fromLabel, result, toLabel);
    }

    private String normalizeTemp(String unit) {
        return switch (unit) {
            case "celsius", "°c", "c" -> "c";
            case "fahrenheit", "°f", "f" -> "f";
            case "kelvin", "°k", "k" -> "k";
            default -> unit;
        };
    }

    private String tempLabel(String unit) {
        return switch (unit) {
            case "c" -> "°C (Celsius)";
            case "f" -> "°F (Fahrenheit)";
            case "k" -> "K (Kelvin)";
            default -> unit;
        };
    }
}
