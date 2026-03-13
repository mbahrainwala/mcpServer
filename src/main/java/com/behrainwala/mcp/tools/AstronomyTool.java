package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool for astronomy calculations.
 * Covers planet data, stellar properties, orbital mechanics, celestial coordinates, and moon phases.
 */
@Service
public class AstronomyTool {

    // ── Physical & Astronomical Constants ──
    private static final double G = 6.674e-11;                  // gravitational constant (N*m^2/kg^2)
    private static final double STEFAN_BOLTZMANN = 5.670374419e-8; // Stefan-Boltzmann constant (W/m^2*K^4)
    private static final double SOLAR_LUMINOSITY = 3.828e26;     // W
    private static final double SOLAR_RADIUS = 6.9634e8;         // m
    private static final double SOLAR_MASS = 1.989e30;           // kg
    private static final double SOLAR_TEMPERATURE = 5778;         // K
    private static final double AU_IN_KM = 1.496e8;              // km
    private static final double PARSEC_IN_LY = 3.26156;          // light-years
    private static final double EARTH_MASS = 5.972e24;            // kg
    private static final double SYNODIC_PERIOD = 29.53058770576;  // days
    // Reference new moon: January 6, 2000 18:14 UTC (approximated as Jan 6 2000)
    private static final LocalDate REFERENCE_NEW_MOON = LocalDate.of(2000, 1, 6);

    // ── Planet Database ──
    private static final Map<String, PlanetData> PLANETS = new LinkedHashMap<>();

    static {
        PLANETS.put("mercury", new PlanetData("Mercury", 3.301e23, 0.0553, 2439.7, 0.387, 57.91e6,
                0.2408, 1407.6, 0, 3.7, "-173 to 427 C",
                "Virtually none (trace O2, Na, H2, He, K)", "Terrestrial"));
        PLANETS.put("venus", new PlanetData("Venus", 4.867e24, 0.815, 6051.8, 0.723, 108.2e6,
                0.6152, -5832.5, 0, 8.87, "462 C (average)",
                "96.5% CO2, 3.5% N2 (thick, dense atmosphere)", "Terrestrial"));
        PLANETS.put("earth", new PlanetData("Earth", 5.972e24, 1.0, 6371.0, 1.0, 149.6e6,
                1.0, 23.93, 1, 9.81, "-88 to 58 C",
                "78% N2, 21% O2, 0.9% Ar, 0.04% CO2", "Terrestrial"));
        PLANETS.put("mars", new PlanetData("Mars", 6.417e23, 0.107, 3389.5, 1.524, 227.9e6,
                1.881, 24.62, 2, 3.72, "-140 to 20 C",
                "95.3% CO2, 2.7% N2, 1.6% Ar", "Terrestrial"));
        PLANETS.put("jupiter", new PlanetData("Jupiter", 1.898e27, 317.8, 69911.0, 5.203, 778.5e6,
                11.86, 9.93, 95, 24.79, "-108 C (cloud top average)",
                "89.8% H2, 10.2% He (traces of CH4, NH3, H2O)", "Gas Giant"));
        PLANETS.put("saturn", new PlanetData("Saturn", 5.683e26, 95.16, 58232.0, 9.537, 1427e6,
                29.46, 10.66, 146, 10.44, "-139 C (cloud top average)",
                "96.3% H2, 3.25% He (traces of CH4, NH3)", "Gas Giant"));
        PLANETS.put("uranus", new PlanetData("Uranus", 8.681e25, 14.54, 25362.0, 19.19, 2871e6,
                84.01, -17.24, 28, 8.87, "-197 C (cloud top average)",
                "82.5% H2, 15.2% He, 2.3% CH4", "Ice Giant"));
        PLANETS.put("neptune", new PlanetData("Neptune", 1.024e26, 17.15, 24622.0, 30.07, 4495e6,
                164.8, 16.11, 16, 11.15, "-201 C (cloud top average)",
                "80% H2, 19% He, 1.5% CH4", "Ice Giant"));
        PLANETS.put("pluto", new PlanetData("Pluto", 1.303e22, 0.00218, 1188.3, 39.48, 5906e6,
                247.9, -153.29, 5, 0.62, "-233 to -223 C",
                "Thin: N2, CH4, CO (surface pressure ~1 Pa)", "Dwarf Planet"));
    }

    // ── Spectral Class Database ──
    private static final Map<String, SpectralData> SPECTRAL_CLASSES = new LinkedHashMap<>();

    static {
        SPECTRAL_CLASSES.put("O", new SpectralData("O", "30,000 - 50,000+", "Blue",
                "10 Lacertae, Zeta Ophiuchi", ">30,000", "16 - 150+"));
        SPECTRAL_CLASSES.put("B", new SpectralData("B", "10,000 - 30,000", "Blue-White",
                "Rigel, Spica, Regulus", "25 - 30,000", "2.1 - 16"));
        SPECTRAL_CLASSES.put("A", new SpectralData("A", "7,500 - 10,000", "White",
                "Sirius, Vega, Altair", "5 - 25", "1.4 - 2.1"));
        SPECTRAL_CLASSES.put("F", new SpectralData("F", "6,000 - 7,500", "Yellow-White",
                "Canopus, Procyon, Polaris", "1.5 - 5", "1.04 - 1.4"));
        SPECTRAL_CLASSES.put("G", new SpectralData("G", "5,200 - 6,000", "Yellow",
                "Sun, Alpha Centauri A, Capella", "0.6 - 1.5", "0.8 - 1.04"));
        SPECTRAL_CLASSES.put("K", new SpectralData("K", "3,700 - 5,200", "Orange",
                "Arcturus, Aldebaran, Alpha Centauri B", "0.08 - 0.6", "0.45 - 0.8"));
        SPECTRAL_CLASSES.put("M", new SpectralData("M", "2,400 - 3,700", "Red",
                "Betelgeuse, Proxima Centauri, Barnard's Star", "<0.08 (main sequence)", "0.08 - 0.45"));
    }

    // ═════════════════════════════════════════════════════════════════
    // Tool 1: Planet Info
    // ═════════════════════════════════════════════════════════════════

    @Tool(name = "astronomy_planet_info", description = "Look up detailed data for planets in our solar system. "
            + "Provides mass, radius, distance from sun, orbital period, rotation period, number of moons, "
            + "surface gravity, temperature range, atmosphere composition, and classification for all 8 planets plus Pluto.")
    public String planetInfo(
            @ToolParam(description = "Planet name (Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, or Pluto)") String planet) {

        if (planet == null || planet.isBlank()) {
            return "Error: Please provide a planet name.";
        }

        String key = planet.strip().toLowerCase();
        PlanetData p = PLANETS.get(key);

        if (p == null) {
            return "Error: Unknown planet '" + planet + "'.\n" +
                    "Available planets: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════╗\n");
        sb.append(String.format("║  %-48s║%n", p.name + " - " + p.classification));
        sb.append("╠══════════════════════════════════════════════════╣\n");
        sb.append(String.format("║  Mass:             %-29s║%n", sci(p.massKg) + " kg"));
        sb.append(String.format("║                    %-29s║%n", p.massEarth + " Earth masses"));
        sb.append(String.format("║  Radius:           %-29s║%n", formatNumber(p.radiusKm) + " km"));
        sb.append(String.format("║  Distance from Sun:%-29s║%n", p.distanceAU + " AU"));
        sb.append(String.format("║                    %-29s║%n", sci(p.distanceKm) + " km"));
        sb.append(String.format("║  Orbital Period:   %-29s║%n", p.orbitalPeriodYears + " years"));
        sb.append(String.format("║  Rotation Period:  %-29s║%n", p.rotationPeriodHours + " hours"));
        sb.append(String.format("║  Number of Moons:  %-29s║%n", p.moons));
        sb.append(String.format("║  Surface Gravity:  %-29s║%n", p.surfaceGravity + " m/s^2"));
        sb.append(String.format("║  Temperature:      %-29s║%n", p.temperatureRange));
        sb.append("╠══════════════════════════════════════════════════╣\n");
        sb.append("║  Atmosphere:                                    ║\n");
        // Wrap atmosphere text
        String atm = p.atmosphere;
        int maxLen = 46;
        while (atm.length() > maxLen) {
            int breakIdx = atm.lastIndexOf(' ', maxLen);
            if (breakIdx <= 0) breakIdx = maxLen;
            sb.append(String.format("║    %-46s║%n", atm.substring(0, breakIdx)));
            atm = atm.substring(breakIdx).stripLeading();
        }
        sb.append(String.format("║    %-46s║%n", atm));
        sb.append("╚══════════════════════════════════════════════════╝\n");

        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════════
    // Tool 2: Star Properties
    // ═════════════════════════════════════════════════════════════════

    @Tool(name = "astronomy_star_properties", description = "Perform stellar calculations: luminosity from radius and temperature, "
            + "absolute magnitude from apparent magnitude and distance, distance from parallax, and spectral class lookup. "
            + "Params is a comma-separated list of key=value pairs relevant to the chosen calculation.")
    public String starProperties(
            @ToolParam(description = "Calculation type: luminosity, absolute_magnitude, apparent_magnitude, distance, spectral_class") String calculation,
            @ToolParam(description = "Comma-separated key=value parameters. "
                    + "luminosity: radius_solar,temperature_k | "
                    + "absolute_magnitude: apparent_magnitude,distance_parsec | "
                    + "apparent_magnitude: absolute_magnitude,distance_parsec | "
                    + "distance: parallax_arcsec | "
                    + "spectral_class: type (O,B,A,F,G,K,M or 'all')") String params) {

        if (calculation == null || calculation.isBlank()) {
            return "Error: Please specify a calculation type.";
        }

        Map<String, String> paramMap = parseParams(params);
        String calc = calculation.strip().toLowerCase();

        try {
            return switch (calc) {
                case "luminosity" -> calcLuminosity(paramMap);
                case "absolute_magnitude" -> calcAbsoluteMagnitude(paramMap);
                case "apparent_magnitude" -> calcApparentMagnitude(paramMap);
                case "distance" -> calcStellarDistance(paramMap);
                case "spectral_class" -> lookupSpectralClass(paramMap);
                default -> "Error: Unknown calculation '" + calculation + "'. "
                        + "Available: luminosity, absolute_magnitude, apparent_magnitude, distance, spectral_class";
            };
        } catch (NumberFormatException e) {
            return "Error: Invalid numeric parameter. " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String calcLuminosity(Map<String, String> params) {
        double radiusSolar = requireDouble(params, "radius_solar");
        double tempK = requireDouble(params, "temperature_k");

        double radiusM = radiusSolar * SOLAR_RADIUS;
        double luminosity = 4.0 * Math.PI * radiusM * radiusM * STEFAN_BOLTZMANN * Math.pow(tempK, 4);
        double luminositySolar = luminosity / SOLAR_LUMINOSITY;

        StringBuilder sb = new StringBuilder();
        sb.append("Stellar Luminosity Calculation\n");
        sb.append("══════════════════════════════\n\n");
        sb.append("Formula: L = 4*pi*R^2*sigma*T^4\n\n");
        sb.append("Inputs:\n");
        sb.append("  Radius:      ").append(radiusSolar).append(" R_sun (").append(sci(radiusM)).append(" m)\n");
        sb.append("  Temperature: ").append(formatNumber(tempK)).append(" K\n\n");
        sb.append("Results:\n");
        sb.append("  Luminosity:  ").append(sci(luminosity)).append(" W\n");
        sb.append("  Luminosity:  ").append(String.format("%.4f", luminositySolar)).append(" L_sun\n");

        if (luminositySolar > 1) {
            sb.append("\n  This star is ").append(String.format("%.2f", luminositySolar)).append("x more luminous than the Sun.\n");
        } else {
            sb.append("\n  This star is ").append(String.format("%.4f", luminositySolar)).append("x as luminous as the Sun.\n");
        }

        return sb.toString();
    }

    private String calcAbsoluteMagnitude(Map<String, String> params) {
        double apparentMag = requireDouble(params, "apparent_magnitude");
        double distancePc = requireDouble(params, "distance_parsec");

        if (distancePc <= 0) {
            return "Error: Distance must be positive.";
        }

        double absoluteMag = apparentMag - 5.0 * Math.log10(distancePc / 10.0);

        return "Absolute Magnitude Calculation\n" +
                "══════════════════════════════\n\n" +
                "Formula: M = m - 5*log10(d/10)\n\n" +
                "Inputs:\n" +
                "  Apparent magnitude (m): " + apparentMag + "\n" +
                "  Distance (d):           " + distancePc + " parsecs" +
                " (" + String.format("%.2f", distancePc * PARSEC_IN_LY) + " light-years)\n\n" +
                "Result:\n" +
                "  Absolute magnitude (M): " + String.format("%.4f", absoluteMag) + "\n";
    }

    private String calcApparentMagnitude(Map<String, String> params) {
        double absoluteMag = requireDouble(params, "absolute_magnitude");
        double distancePc = requireDouble(params, "distance_parsec");

        if (distancePc <= 0) {
            return "Error: Distance must be positive.";
        }

        double apparentMag = absoluteMag + 5.0 * Math.log10(distancePc / 10.0);

        return "Apparent Magnitude Calculation\n" +
                "══════════════════════════════\n\n" +
                "Formula: m = M + 5*log10(d/10)\n\n" +
                "Inputs:\n" +
                "  Absolute magnitude (M): " + absoluteMag + "\n" +
                "  Distance (d):           " + distancePc + " parsecs" +
                " (" + String.format("%.2f", distancePc * PARSEC_IN_LY) + " light-years)\n\n" +
                "Result:\n" +
                "  Apparent magnitude (m): " + String.format("%.4f", apparentMag) + "\n";
    }

    private String calcStellarDistance(Map<String, String> params) {
        double parallax = requireDouble(params, "parallax_arcsec");

        if (parallax <= 0) {
            return "Error: Parallax must be positive.";
        }

        double distancePc = 1.0 / parallax;
        double distanceLy = distancePc * PARSEC_IN_LY;
        double distanceAu = distancePc * 206265.0;

        return "Stellar Distance from Parallax\n" +
                "══════════════════════════════\n\n" +
                "Formula: d = 1/p parsecs\n\n" +
                "Input:\n" +
                "  Parallax: " + parallax + " arcseconds\n\n" +
                "Results:\n" +
                "  Distance: " + String.format("%.4f", distancePc) + " parsecs\n" +
                "  Distance: " + String.format("%.4f", distanceLy) + " light-years\n" +
                "  Distance: " + sci(distanceAu) + " AU\n";
    }

    private String lookupSpectralClass(Map<String, String> params) {
        String type = params.getOrDefault("type", "all").strip().toUpperCase();

        StringBuilder sb = new StringBuilder();
        sb.append("Stellar Spectral Classification\n");
        sb.append("═══════════════════════════════\n\n");

        if ("ALL".equals(type)) {
            sb.append(String.format("%-6s %-18s %-14s %-14s %-14s%n", "Class", "Temperature (K)", "Color", "Luminosity (L_sun)", "Mass (M_sun)"));
            sb.append("─".repeat(70)).append("\n");
            for (SpectralData sd : SPECTRAL_CLASSES.values()) {
                sb.append(String.format("%-6s %-18s %-14s %-18s %-14s%n",
                        sd.type, sd.tempRange, sd.color, sd.luminosityRange, sd.massRange));
            }
            sb.append("\nExamples per class:\n");
            for (SpectralData sd : SPECTRAL_CLASSES.values()) {
                sb.append("  ").append(sd.type).append(": ").append(sd.examples).append("\n");
            }
        } else {
            SpectralData sd = SPECTRAL_CLASSES.get(type);
            if (sd == null) {
                return "Error: Unknown spectral class '" + type + "'. Available: O, B, A, F, G, K, M (or 'all')";
            }
            sb.append("Class:       ").append(sd.type).append("\n");
            sb.append("Temperature: ").append(sd.tempRange).append(" K\n");
            sb.append("Color:       ").append(sd.color).append("\n");
            sb.append("Luminosity:  ").append(sd.luminosityRange).append(" L_sun\n");
            sb.append("Mass:        ").append(sd.massRange).append(" M_sun\n");
            sb.append("Examples:    ").append(sd.examples).append("\n");
        }

        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════════
    // Tool 3: Orbital Mechanics
    // ═════════════════════════════════════════════════════════════════

    @Tool(name = "astronomy_orbital_mechanics", description = "Perform orbital mechanics calculations: orbital velocity, "
            + "escape velocity, orbital period (Kepler's 3rd law), Hohmann transfer delta-v, and gravitational force. "
            + "Params is a comma-separated list of key=value pairs relevant to the chosen calculation.")
    public String orbitalMechanics(
            @ToolParam(description = "Calculation type: orbital_velocity, escape_velocity, orbital_period, hohmann_transfer, gravitational_force") String calculation,
            @ToolParam(description = "Comma-separated key=value parameters. "
                    + "orbital_velocity: mass_kg,radius_m | "
                    + "escape_velocity: mass_kg,radius_m | "
                    + "orbital_period: semi_major_axis_m,central_mass_kg | "
                    + "hohmann_transfer: r1_m,r2_m,central_mass_kg | "
                    + "gravitational_force: m1_kg,m2_kg,distance_m") String params) {

        if (calculation == null || calculation.isBlank()) {
            return "Error: Please specify a calculation type.";
        }

        Map<String, String> paramMap = parseParams(params);
        String calc = calculation.strip().toLowerCase();

        try {
            return switch (calc) {
                case "orbital_velocity" -> calcOrbitalVelocity(paramMap);
                case "escape_velocity" -> calcEscapeVelocity(paramMap);
                case "orbital_period" -> calcOrbitalPeriod(paramMap);
                case "hohmann_transfer" -> calcHohmannTransfer(paramMap);
                case "gravitational_force" -> calcGravitationalForce(paramMap);
                default -> "Error: Unknown calculation '" + calculation + "'. "
                        + "Available: orbital_velocity, escape_velocity, orbital_period, hohmann_transfer, gravitational_force";
            };
        } catch (NumberFormatException e) {
            return "Error: Invalid numeric parameter. " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String calcOrbitalVelocity(Map<String, String> params) {
        double mass = requireDouble(params, "mass_kg");
        double radius = requireDouble(params, "radius_m");

        if (radius <= 0) return "Error: Radius must be positive.";

        double velocity = Math.sqrt(G * mass / radius);

        return "Orbital Velocity Calculation\n" +
                "═══════════════════════════\n\n" +
                "Formula: v = sqrt(G*M/r)\n\n" +
                "Inputs:\n" +
                "  Central mass (M): " + sci(mass) + " kg\n" +
                "  Orbital radius (r): " + sci(radius) + " m\n\n" +
                "Result:\n" +
                "  Orbital velocity: " + String.format("%.2f", velocity) + " m/s\n" +
                "  Orbital velocity: " + String.format("%.4f", velocity / 1000.0) + " km/s\n";
    }

    private String calcEscapeVelocity(Map<String, String> params) {
        double mass = requireDouble(params, "mass_kg");
        double radius = requireDouble(params, "radius_m");

        if (radius <= 0) return "Error: Radius must be positive.";

        double velocity = Math.sqrt(2.0 * G * mass / radius);

        return "Escape Velocity Calculation\n" +
                "══════════════════════════\n\n" +
                "Formula: v = sqrt(2*G*M/r)\n\n" +
                "Inputs:\n" +
                "  Mass (M):   " + sci(mass) + " kg\n" +
                "  Radius (r): " + sci(radius) + " m\n\n" +
                "Result:\n" +
                "  Escape velocity: " + String.format("%.2f", velocity) + " m/s\n" +
                "  Escape velocity: " + String.format("%.4f", velocity / 1000.0) + " km/s\n";
    }

    private String calcOrbitalPeriod(Map<String, String> params) {
        double semiMajorAxis = requireDouble(params, "semi_major_axis_m");
        double centralMass = requireDouble(params, "central_mass_kg");

        if (semiMajorAxis <= 0 || centralMass <= 0) return "Error: Semi-major axis and central mass must be positive.";

        // Kepler's 3rd law: T = 2*pi*sqrt(a^3 / (G*M))
        double period = 2.0 * Math.PI * Math.sqrt(Math.pow(semiMajorAxis, 3) / (G * centralMass));

        return "Orbital Period Calculation (Kepler's 3rd Law)\n" +
                "═════════════════════════════════════════════\n\n" +
                "Formula: T = 2*pi*sqrt(a^3 / (G*M))\n\n" +
                "Inputs:\n" +
                "  Semi-major axis (a): " + sci(semiMajorAxis) + " m\n" +
                "  Central mass (M):    " + sci(centralMass) + " kg\n\n" +
                "Results:\n" +
                "  Period: " + sci(period) + " seconds\n" +
                "  Period: " + String.format("%.4f", period / 3600.0) + " hours\n" +
                "  Period: " + String.format("%.6f", period / 86400.0) + " days\n" +
                "  Period: " + String.format("%.6f", period / (86400.0 * 365.25)) + " years\n";
    }

    private String calcHohmannTransfer(Map<String, String> params) {
        double r1 = requireDouble(params, "r1_m");
        double r2 = requireDouble(params, "r2_m");
        double centralMass = requireDouble(params, "central_mass_kg");

        if (r1 <= 0 || r2 <= 0 || centralMass <= 0) return "Error: All values must be positive.";

        double mu = G * centralMass;

        // Transfer orbit semi-major axis
        double aTransfer = (r1 + r2) / 2.0;

        // Velocities at departure orbit
        double v1Circular = Math.sqrt(mu / r1);
        double v1Transfer = Math.sqrt(mu * (2.0 / r1 - 1.0 / aTransfer));
        double deltaV1 = Math.abs(v1Transfer - v1Circular);

        // Velocities at arrival orbit
        double v2Circular = Math.sqrt(mu / r2);
        double v2Transfer = Math.sqrt(mu * (2.0 / r2 - 1.0 / aTransfer));
        double deltaV2 = Math.abs(v2Circular - v2Transfer);

        double totalDeltaV = deltaV1 + deltaV2;

        // Transfer time (half the transfer orbit period)
        double transferTime = Math.PI * Math.sqrt(Math.pow(aTransfer, 3) / mu);

        return "Hohmann Transfer Orbit Calculation\n" +
                "══════════════════════════════════\n\n" +
                "Inputs:\n" +
                "  Inner orbit radius (r1):  " + sci(r1) + " m\n" +
                "  Outer orbit radius (r2):  " + sci(r2) + " m\n" +
                "  Central body mass:        " + sci(centralMass) + " kg\n\n" +
                "Transfer Orbit:\n" +
                "  Semi-major axis: " + sci(aTransfer) + " m\n\n" +
                "Departure burn (at r1):\n" +
                "  Circular velocity:  " + String.format("%.2f", v1Circular) + " m/s (" + String.format("%.4f", v1Circular / 1000) + " km/s)\n" +
                "  Transfer velocity:  " + String.format("%.2f", v1Transfer) + " m/s (" + String.format("%.4f", v1Transfer / 1000) + " km/s)\n" +
                "  Delta-v1:           " + String.format("%.2f", deltaV1) + " m/s (" + String.format("%.4f", deltaV1 / 1000) + " km/s)\n\n" +
                "Arrival burn (at r2):\n" +
                "  Transfer velocity:  " + String.format("%.2f", v2Transfer) + " m/s (" + String.format("%.4f", v2Transfer / 1000) + " km/s)\n" +
                "  Circular velocity:  " + String.format("%.2f", v2Circular) + " m/s (" + String.format("%.4f", v2Circular / 1000) + " km/s)\n" +
                "  Delta-v2:           " + String.format("%.2f", deltaV2) + " m/s (" + String.format("%.4f", deltaV2 / 1000) + " km/s)\n\n" +
                "Total:\n" +
                "  Total delta-v:    " + String.format("%.2f", totalDeltaV) + " m/s (" + String.format("%.4f", totalDeltaV / 1000) + " km/s)\n" +
                "  Transfer time:    " + sci(transferTime) + " seconds\n" +
                "  Transfer time:    " + String.format("%.2f", transferTime / 86400.0) + " days\n";
    }

    private String calcGravitationalForce(Map<String, String> params) {
        double m1 = requireDouble(params, "m1_kg");
        double m2 = requireDouble(params, "m2_kg");
        double distance = requireDouble(params, "distance_m");

        if (distance <= 0) return "Error: Distance must be positive.";

        double force = G * m1 * m2 / (distance * distance);

        return "Gravitational Force Calculation\n" +
                "══════════════════════════════\n\n" +
                "Formula: F = G*m1*m2/r^2\n\n" +
                "Inputs:\n" +
                "  Mass 1 (m1):    " + sci(m1) + " kg\n" +
                "  Mass 2 (m2):    " + sci(m2) + " kg\n" +
                "  Distance (r):   " + sci(distance) + " m\n" +
                "  G:              " + sci(G) + " N*m^2/kg^2\n\n" +
                "Result:\n" +
                "  Gravitational force: " + sci(force) + " N\n";
    }

    // ═════════════════════════════════════════════════════════════════
    // Tool 4: Celestial Coordinates
    // ═════════════════════════════════════════════════════════════════

    @Tool(name = "astronomy_celestial_coordinates", description = "Convert between celestial coordinate systems: "
            + "equatorial to horizontal, RA/Dec format conversions, and angular separation between objects. "
            + "Params is a comma-separated list of key=value pairs relevant to the chosen conversion.")
    public String celestialCoordinates(
            @ToolParam(description = "Conversion type: equatorial_to_horizontal, horizontal_to_equatorial, ra_format, dec_format, angular_separation") String conversion,
            @ToolParam(description = "Comma-separated key=value parameters. "
                    + "ra_format: ra (in h:m:s or decimal degrees) | "
                    + "dec_format: dec (in d:m:s or decimal degrees) | "
                    + "equatorial_to_horizontal: ra_hours,dec_deg,latitude_deg,lst_hours | "
                    + "horizontal_to_equatorial: alt_deg,az_deg,latitude_deg,lst_hours | "
                    + "angular_separation: ra1_deg,dec1_deg,ra2_deg,dec2_deg") String params) {

        if (conversion == null || conversion.isBlank()) {
            return "Error: Please specify a conversion type.";
        }

        Map<String, String> paramMap = parseParams(params);
        String conv = conversion.strip().toLowerCase();

        try {
            return switch (conv) {
                case "ra_format" -> convertRaFormat(paramMap);
                case "dec_format" -> convertDecFormat(paramMap);
                case "equatorial_to_horizontal" -> equatorialToHorizontal(paramMap);
                case "horizontal_to_equatorial" -> horizontalToEquatorial(paramMap);
                case "angular_separation" -> angularSeparation(paramMap);
                default -> "Error: Unknown conversion '" + conversion + "'. "
                        + "Available: ra_format, dec_format, equatorial_to_horizontal, horizontal_to_equatorial, angular_separation";
            };
        } catch (NumberFormatException e) {
            return "Error: Invalid numeric parameter. " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String convertRaFormat(Map<String, String> params) {
        String ra = requireString(params, "ra");

        StringBuilder sb = new StringBuilder();
        sb.append("Right Ascension Format Conversion\n");
        sb.append("═════════════════════════════════\n\n");

        if (ra.contains(":")) {
            // h:m:s -> decimal degrees
            String[] parts = ra.split(":");
            double hours = Double.parseDouble(parts[0].trim());
            double minutes = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0;
            double seconds = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 0;

            double totalHours = hours + minutes / 60.0 + seconds / 3600.0;
            double decimalDeg = totalHours * 15.0;

            sb.append("Input:  ").append(ra).append(" (hours:minutes:seconds)\n\n");
            sb.append("Results:\n");
            sb.append("  Decimal hours:   ").append(String.format("%.6f", totalHours)).append(" h\n");
            sb.append("  Decimal degrees: ").append(String.format("%.6f", decimalDeg)).append(" deg\n");
        } else {
            // decimal degrees -> h:m:s
            double decimalDeg = Double.parseDouble(ra.trim());
            double totalHours = decimalDeg / 15.0;
            int h = (int) totalHours;
            double remMin = (totalHours - h) * 60.0;
            int m = (int) remMin;
            double s = (remMin - m) * 60.0;

            sb.append("Input:  ").append(decimalDeg).append(" degrees\n\n");
            sb.append("Results:\n");
            sb.append("  Decimal hours: ").append(String.format("%.6f", totalHours)).append(" h\n");
            sb.append("  HMS format:    ").append(String.format("%dh %dm %.2fs", h, m, s)).append("\n");
            sb.append("  HMS format:    ").append(String.format("%d:%02d:%05.2f", h, m, s)).append("\n");
        }

        return sb.toString();
    }

    private String convertDecFormat(Map<String, String> params) {
        String dec = requireString(params, "dec");

        StringBuilder sb = new StringBuilder();
        sb.append("Declination Format Conversion\n");
        sb.append("════════════════════════════\n\n");

        if (dec.contains(":")) {
            // d:m:s -> decimal degrees
            String[] parts = dec.split(":");
            double degrees = Double.parseDouble(parts[0].trim());
            double arcmin = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0;
            double arcsec = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 0;

            double sign = degrees < 0 ? -1.0 : 1.0;
            double decimalDeg = sign * (Math.abs(degrees) + arcmin / 60.0 + arcsec / 3600.0);

            sb.append("Input:  ").append(dec).append(" (degrees:arcminutes:arcseconds)\n\n");
            sb.append("Result:\n");
            sb.append("  Decimal degrees: ").append(String.format("%.6f", decimalDeg)).append(" deg\n");
        } else {
            // decimal degrees -> d:m:s
            double decimalDeg = Double.parseDouble(dec.trim());
            String sign = decimalDeg < 0 ? "-" : "+";
            double absDeg = Math.abs(decimalDeg);
            int d = (int) absDeg;
            double remArcmin = (absDeg - d) * 60.0;
            int m = (int) remArcmin;
            double s = (remArcmin - m) * 60.0;

            sb.append("Input:  ").append(decimalDeg).append(" degrees\n\n");
            sb.append("Result:\n");
            sb.append("  DMS format: ").append(String.format("%s%d° %d' %.2f\"", sign, d, m, s)).append("\n");
            sb.append("  DMS format: ").append(String.format("%s%d:%02d:%05.2f", sign, d, m, s)).append("\n");
        }

        return sb.toString();
    }

    private String equatorialToHorizontal(Map<String, String> params) {
        double raHours = requireDouble(params, "ra_hours");
        double decDeg = requireDouble(params, "dec_deg");
        double latDeg = requireDouble(params, "latitude_deg");
        double lstHours = requireDouble(params, "lst_hours");

        // Hour angle
        double haDeg = (lstHours - raHours) * 15.0;
        double haRad = Math.toRadians(haDeg);
        double decRad = Math.toRadians(decDeg);
        double latRad = Math.toRadians(latDeg);

        // Altitude
        double sinAlt = Math.sin(decRad) * Math.sin(latRad) + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        double altRad = Math.asin(sinAlt);
        double altDeg = Math.toDegrees(altRad);

        // Azimuth
        double cosAz = (Math.sin(decRad) - Math.sin(altRad) * Math.sin(latRad)) / (Math.cos(altRad) * Math.cos(latRad));
        cosAz = Math.max(-1.0, Math.min(1.0, cosAz)); // clamp
        double azDeg = Math.toDegrees(Math.acos(cosAz));
        if (Math.sin(haRad) > 0) {
            azDeg = 360.0 - azDeg;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Equatorial to Horizontal Coordinate Conversion\n");
        sb.append("══════════════════════════════════════════════\n\n");
        sb.append("Inputs:\n");
        sb.append("  Right Ascension:     ").append(String.format("%.4f", raHours)).append(" hours\n");
        sb.append("  Declination:         ").append(String.format("%.4f", decDeg)).append(" degrees\n");
        sb.append("  Observer latitude:   ").append(String.format("%.4f", latDeg)).append(" degrees\n");
        sb.append("  Local Sidereal Time: ").append(String.format("%.4f", lstHours)).append(" hours\n\n");
        sb.append("Intermediate:\n");
        sb.append("  Hour Angle: ").append(String.format("%.4f", haDeg)).append(" degrees\n\n");
        sb.append("Results:\n");
        sb.append("  Altitude: ").append(String.format("%.4f", altDeg)).append(" degrees\n");
        sb.append("  Azimuth:  ").append(String.format("%.4f", azDeg)).append(" degrees\n");

        if (altDeg < 0) {
            sb.append("\n  Note: Object is below the horizon.\n");
        }

        return sb.toString();
    }

    private String horizontalToEquatorial(Map<String, String> params) {
        double altDeg = requireDouble(params, "alt_deg");
        double azDeg = requireDouble(params, "az_deg");
        double latDeg = requireDouble(params, "latitude_deg");
        double lstHours = requireDouble(params, "lst_hours");

        double altRad = Math.toRadians(altDeg);
        double azRad = Math.toRadians(azDeg);
        double latRad = Math.toRadians(latDeg);

        // Declination
        double sinDec = Math.sin(altRad) * Math.sin(latRad) + Math.cos(altRad) * Math.cos(latRad) * Math.cos(azRad);
        double decRad = Math.asin(sinDec);
        double decDegResult = Math.toDegrees(decRad);

        // Hour angle
        double cosHA = (Math.sin(altRad) - Math.sin(decRad) * Math.sin(latRad)) / (Math.cos(decRad) * Math.cos(latRad));
        cosHA = Math.max(-1.0, Math.min(1.0, cosHA));
        double haDeg = Math.toDegrees(Math.acos(cosHA));
        if (Math.sin(azRad) > 0) {
            haDeg = 360.0 - haDeg;
        }

        double raHours = lstHours - haDeg / 15.0;
        if (raHours < 0) raHours += 24.0;
        if (raHours >= 24.0) raHours -= 24.0;

        return "Horizontal to Equatorial Coordinate Conversion\n" +
                "══════════════════════════════════════════════\n\n" +
                "Inputs:\n" +
                "  Altitude:            " + String.format("%.4f", altDeg) + " degrees\n" +
                "  Azimuth:             " + String.format("%.4f", azDeg) + " degrees\n" +
                "  Observer latitude:   " + String.format("%.4f", latDeg) + " degrees\n" +
                "  Local Sidereal Time: " + String.format("%.4f", lstHours) + " hours\n\n" +
                "Results:\n" +
                "  Right Ascension: " + String.format("%.4f", raHours) + " hours\n" +
                "  Declination:     " + String.format("%.4f", decDegResult) + " degrees\n";
    }

    private String angularSeparation(Map<String, String> params) {
        double ra1Deg = requireDouble(params, "ra1_deg");
        double dec1Deg = requireDouble(params, "dec1_deg");
        double ra2Deg = requireDouble(params, "ra2_deg");
        double dec2Deg = requireDouble(params, "dec2_deg");

        double ra1Rad = Math.toRadians(ra1Deg);
        double dec1Rad = Math.toRadians(dec1Deg);
        double ra2Rad = Math.toRadians(ra2Deg);
        double dec2Rad = Math.toRadians(dec2Deg);

        // Vincenty formula for angular distance (more numerically stable)
        double deltaRa = ra2Rad - ra1Rad;
        double num1 = Math.cos(dec2Rad) * Math.sin(deltaRa);
        double num2 = Math.cos(dec1Rad) * Math.sin(dec2Rad) - Math.sin(dec1Rad) * Math.cos(dec2Rad) * Math.cos(deltaRa);
        double numerator = Math.sqrt(num1 * num1 + num2 * num2);
        double denominator = Math.sin(dec1Rad) * Math.sin(dec2Rad) + Math.cos(dec1Rad) * Math.cos(dec2Rad) * Math.cos(deltaRa);

        double separationRad = Math.atan2(numerator, denominator);
        double separationDeg = Math.toDegrees(separationRad);
        double separationArcmin = separationDeg * 60.0;
        double separationArcsec = separationDeg * 3600.0;

        return "Angular Separation Calculation\n" +
                "═════════════════════════════\n\n" +
                "Inputs:\n" +
                "  Object 1: RA = " + String.format("%.4f", ra1Deg) + " deg, Dec = " + String.format("%.4f", dec1Deg) + " deg\n" +
                "  Object 2: RA = " + String.format("%.4f", ra2Deg) + " deg, Dec = " + String.format("%.4f", dec2Deg) + " deg\n\n" +
                "Result:\n" +
                "  Angular separation: " + String.format("%.6f", separationDeg) + " degrees\n" +
                "  Angular separation: " + String.format("%.4f", separationArcmin) + " arcminutes\n" +
                "  Angular separation: " + String.format("%.2f", separationArcsec) + " arcseconds\n";
    }

    // ═════════════════════════════════════════════════════════════════
    // Tool 5: Moon Phase
    // ═════════════════════════════════════════════════════════════════

    @Tool(name = "astronomy_moon_phase", description = "Calculate the moon phase for a given date. Returns phase name, "
            + "illumination percentage, days into the lunation cycle, and dates of next full moon and next new moon. "
            + "Uses a simplified synodic period calculation (29.53 days).")
    public String moonPhase(
            @ToolParam(description = "Date in yyyy-MM-dd format. Optional - defaults to today's date.") String date) {

        LocalDate targetDate;
        if (date == null || date.isBlank()) {
            targetDate = LocalDate.now();
        } else {
            try {
                targetDate = LocalDate.parse(date.strip(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                return "Error: Invalid date format. Please use yyyy-MM-dd (e.g. 2025-06-15).";
            }
        }

        // Days since reference new moon
        long daysSinceRef = ChronoUnit.DAYS.between(REFERENCE_NEW_MOON, targetDate);
        double daysSinceRefD = (double) daysSinceRef;

        // Current position in the synodic cycle
        double cyclePosition = ((daysSinceRefD % SYNODIC_PERIOD) + SYNODIC_PERIOD) % SYNODIC_PERIOD;

        // Phase name
        String phaseName = getPhaseName(cyclePosition);

        // Illumination (approximation using cosine)
        double phaseAngle = (cyclePosition / SYNODIC_PERIOD) * 2.0 * Math.PI;
        double illumination = (1.0 - Math.cos(phaseAngle)) / 2.0 * 100.0;

        // Days to next new moon
        double daysToNewMoon = SYNODIC_PERIOD - cyclePosition;
        if (daysToNewMoon < 0.5) daysToNewMoon += SYNODIC_PERIOD;

        // Days to next full moon
        double daysToFullMoon = (SYNODIC_PERIOD / 2.0) - cyclePosition;
        if (daysToFullMoon < 0.5) daysToFullMoon += SYNODIC_PERIOD;

        LocalDate nextNewMoon = targetDate.plusDays((long) Math.ceil(daysToNewMoon));
        LocalDate nextFullMoon = targetDate.plusDays((long) Math.ceil(daysToFullMoon));

        return "Moon Phase Calculator\n" +
                "════════════════════\n\n" +
                "Date: " + targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "\n\n" +
                "┌──────────────────────────────────────┐\n" +
                String.format("│  Phase:          %-19s│%n", phaseName) +
                String.format("│  Illumination:   %-19s│%n", String.format("%.1f%%", illumination)) +
                String.format("│  Days into cycle:%-19s│%n", String.format("%.1f / %.1f days", cyclePosition, SYNODIC_PERIOD)) +
                "├──────────────────────────────────────┤\n" +
                String.format("│  Next New Moon:  %-19s│%n", nextNewMoon.format(DateTimeFormatter.ISO_LOCAL_DATE)) +
                String.format("│  (in ~%.0f days)%s│%n", daysToNewMoon, " ".repeat(Math.max(1, 23 - String.format("%.0f", daysToNewMoon).length()))) +
                String.format("│  Next Full Moon: %-19s│%n", nextFullMoon.format(DateTimeFormatter.ISO_LOCAL_DATE)) +
                String.format("│  (in ~%.0f days)%s│%n", daysToFullMoon, " ".repeat(Math.max(1, 23 - String.format("%.0f", daysToFullMoon).length()))) +
                "└──────────────────────────────────────┘\n" +
                "\nPhase Cycle:\n" +
                "  New Moon (0d) -> Waxing Crescent -> First Quarter (7.4d)\n" +
                "  -> Waxing Gibbous -> Full Moon (14.8d) -> Waning Gibbous\n" +
                "  -> Last Quarter (22.1d) -> Waning Crescent -> New Moon (29.5d)\n";
    }

    private String getPhaseName(double cyclePosition) {
        double fraction = cyclePosition / SYNODIC_PERIOD;
        if (fraction < 0.0338) return "New Moon";
        if (fraction < 0.25)  return "Waxing Crescent";
        if (fraction < 0.2838) return "First Quarter";
        if (fraction < 0.50)  return "Waxing Gibbous";
        if (fraction < 0.5338) return "Full Moon";
        if (fraction < 0.75)  return "Waning Gibbous";
        if (fraction < 0.7838) return "Last Quarter";
        if (fraction < 0.9662) return "Waning Crescent";
        return "New Moon";
    }

    // ═════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═════════════════════════════════════════════════════════════════

    private Map<String, String> parseParams(String params) {
        Map<String, String> map = new HashMap<>();
        if (params == null || params.isBlank()) return map;

        String[] pairs = params.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                String key = trimmed.substring(0, eqIdx).trim().toLowerCase();
                String value = trimmed.substring(eqIdx + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private double requireDouble(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return Double.parseDouble(value);
    }

    private String requireString(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return value;
    }

    private static String sci(double value) {
        if (value == 0) return "0";
        return String.format("%.4e", value);
    }

    private static String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%,d", (long) value);
        }
        return String.format("%,.2f", value);
    }

    // ═════════════════════════════════════════════════════════════════
    // Data Classes
    // ═════════════════════════════════════════════════════════════════

    private static class PlanetData {
        final String name;
        final double massKg;
        final double massEarth;
        final double radiusKm;
        final double distanceAU;
        final double distanceKm;
        final double orbitalPeriodYears;
        final double rotationPeriodHours;
        final int moons;
        final double surfaceGravity;
        final String temperatureRange;
        final String atmosphere;
        final String classification;

        PlanetData(String name, double massKg, double massEarth, double radiusKm,
                   double distanceAU, double distanceKm, double orbitalPeriodYears,
                   double rotationPeriodHours, int moons, double surfaceGravity,
                   String temperatureRange, String atmosphere, String classification) {
            this.name = name;
            this.massKg = massKg;
            this.massEarth = massEarth;
            this.radiusKm = radiusKm;
            this.distanceAU = distanceAU;
            this.distanceKm = distanceKm;
            this.orbitalPeriodYears = orbitalPeriodYears;
            this.rotationPeriodHours = rotationPeriodHours;
            this.moons = moons;
            this.surfaceGravity = surfaceGravity;
            this.temperatureRange = temperatureRange;
            this.atmosphere = atmosphere;
            this.classification = classification;
        }
    }

    private static class SpectralData {
        final String type;
        final String tempRange;
        final String color;
        final String examples;
        final String luminosityRange;
        final String massRange;

        SpectralData(String type, String tempRange, String color, String examples,
                     String luminosityRange, String massRange) {
            this.type = type;
            this.tempRange = tempRange;
            this.color = color;
            this.examples = examples;
            this.luminosityRange = luminosityRange;
            this.massRange = massRange;
        }
    }
}
