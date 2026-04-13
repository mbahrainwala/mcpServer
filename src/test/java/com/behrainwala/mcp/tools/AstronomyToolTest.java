package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AstronomyToolTest {

    private AstronomyTool tool;

    @BeforeEach
    void setUp() {
        tool = new AstronomyTool();
    }

    // ── Planet Info ──

    @Nested
    class PlanetInfoTests {
        @Test
        void earth() {
            String result = tool.planetInfo("Earth");
            assertThat(result).contains("Earth").contains("Terrestrial").contains("9.81");
        }

        @Test
        void jupiter_gasGiant() {
            String result = tool.planetInfo("Jupiter");
            assertThat(result).contains("Jupiter").contains("Gas Giant").contains("95");
        }

        @Test
        void pluto_dwarfPlanet() {
            String result = tool.planetInfo("Pluto");
            assertThat(result).contains("Pluto").contains("Dwarf Planet");
        }

        @Test
        void mercury() {
            String result = tool.planetInfo("Mercury");
            assertThat(result).contains("Mercury").contains("Terrestrial");
        }

        @Test
        void venus() {
            String result = tool.planetInfo("Venus");
            assertThat(result).contains("Venus").contains("462");
        }

        @Test
        void mars() {
            String result = tool.planetInfo("Mars");
            assertThat(result).contains("Mars").contains("3.72");
        }

        @Test
        void saturn() {
            String result = tool.planetInfo("Saturn");
            assertThat(result).contains("Saturn").contains("Gas Giant");
        }

        @Test
        void uranus_iceGiant() {
            String result = tool.planetInfo("Uranus");
            assertThat(result).contains("Uranus").contains("Ice Giant");
        }

        @Test
        void neptune_iceGiant() {
            String result = tool.planetInfo("Neptune");
            assertThat(result).contains("Neptune").contains("Ice Giant");
        }

        @Test
        void caseInsensitive() {
            String result = tool.planetInfo("  earth  ");
            assertThat(result).contains("Earth");
        }

        @Test
        void unknownPlanet() {
            String result = tool.planetInfo("Vulcan");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("unknown planet");
        }

        @Test
        void nullPlanet() {
            String result = tool.planetInfo(null);
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void blankPlanet() {
            String result = tool.planetInfo("   ");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void longAtmosphere_wraps() {
            String result = tool.planetInfo("Jupiter");
            assertThat(result).contains("Atmosphere");
        }
    }

    // ── Star Properties ──

    @Nested
    class StarPropertiesTests {
        @Test
        void luminosity_moreThanSun() {
            String result = tool.starProperties("luminosity", "radius_solar=1.71, temperature_k=9940");
            assertThat(result).contains("Luminosity").contains("more luminous");
        }

        @Test
        void luminosity_lessThanSun() {
            String result = tool.starProperties("luminosity", "radius_solar=0.15, temperature_k=3000");
            assertThat(result).contains("Luminosity").contains("as luminous");
        }

        @Test
        void absoluteMagnitude() {
            String result = tool.starProperties("absolute_magnitude", "apparent_magnitude=1.0, distance_parsec=10");
            assertThat(result).contains("Absolute Magnitude");
        }

        @Test
        void absoluteMagnitude_negativeDistance() {
            String result = tool.starProperties("absolute_magnitude", "apparent_magnitude=1.0, distance_parsec=-5");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void apparentMagnitude() {
            String result = tool.starProperties("apparent_magnitude", "absolute_magnitude=4.83, distance_parsec=10");
            assertThat(result).contains("Apparent Magnitude");
        }

        @Test
        void apparentMagnitude_negativeDistance() {
            String result = tool.starProperties("apparent_magnitude", "absolute_magnitude=4.83, distance_parsec=-1");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void distance_fromParallax() {
            String result = tool.starProperties("distance", "parallax_arcsec=0.772");
            assertThat(result).contains("1.295").contains("parsec");
        }

        @Test
        void distance_negativeParallax() {
            String result = tool.starProperties("distance", "parallax_arcsec=-0.5");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void spectralClass_specific_G() {
            String result = tool.starProperties("spectral_class", "type=G");
            assertThat(result).contains("G").contains("Yellow").contains("Sun");
        }

        @Test
        void spectralClass_all() {
            String result = tool.starProperties("spectral_class", "type=all");
            assertThat(result).contains("O").contains("B").contains("A").contains("F")
                    .contains("G").contains("K").contains("M");
        }

        @Test
        void spectralClass_unknown() {
            String result = tool.starProperties("spectral_class", "type=X");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("unknown spectral class");
        }

        @Test
        void spectralClass_O() {
            String result = tool.starProperties("spectral_class", "type=O");
            assertThat(result).contains("Blue").contains("30,000");
        }

        @Test
        void spectralClass_M() {
            String result = tool.starProperties("spectral_class", "type=M");
            assertThat(result).contains("Red").contains("Betelgeuse");
        }

        @Test
        void unknownCalculation() {
            String result = tool.starProperties("unknown_calc", "param=1");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("unknown calculation");
        }

        @Test
        void nullCalculation() {
            String result = tool.starProperties(null, "param=1");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void blankCalculation() {
            String result = tool.starProperties("   ", "param=1");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void missingParameter() {
            String result = tool.starProperties("luminosity", "radius_solar=1.0");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("missing");
        }

        @Test
        void invalidNumericParameter() {
            String result = tool.starProperties("luminosity", "radius_solar=abc, temperature_k=5000");
            assertThat(result).containsIgnoringCase("error");
        }
    }

    // ── Orbital Mechanics ──

    @Nested
    class OrbitalMechanicsTests {
        @Test
        void orbitalVelocity() {
            String result = tool.orbitalMechanics("orbital_velocity",
                    "mass_kg=1.989e30, radius_m=1.496e11");
            assertThat(result).contains("Orbital Velocity").contains("29.");
        }

        @Test
        void orbitalVelocity_negativeRadius() {
            String result = tool.orbitalMechanics("orbital_velocity", "mass_kg=1e30, radius_m=-100");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void escapeVelocity() {
            String result = tool.orbitalMechanics("escape_velocity",
                    "mass_kg=5.972e24, radius_m=6.371e6");
            assertThat(result).contains("Escape Velocity").contains("11.");
        }

        @Test
        void escapeVelocity_negativeRadius() {
            String result = tool.orbitalMechanics("escape_velocity", "mass_kg=1e24, radius_m=-100");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void orbitalPeriod() {
            String result = tool.orbitalMechanics("orbital_period",
                    "semi_major_axis_m=1.496e11, central_mass_kg=1.989e30");
            assertThat(result).contains("Kepler").contains("year");
        }

        @Test
        void orbitalPeriod_nonPositive() {
            String result = tool.orbitalMechanics("orbital_period",
                    "semi_major_axis_m=-1, central_mass_kg=1e30");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void hohmannTransfer() {
            String result = tool.orbitalMechanics("hohmann_transfer",
                    "r1_m=6.571e6, r2_m=4.22e7, central_mass_kg=5.972e24");
            assertThat(result).contains("Hohmann Transfer").contains("Delta-v");
        }

        @Test
        void hohmannTransfer_nonPositive() {
            String result = tool.orbitalMechanics("hohmann_transfer",
                    "r1_m=-1, r2_m=4.22e7, central_mass_kg=5.972e24");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void gravitationalForce() {
            String result = tool.orbitalMechanics("gravitational_force",
                    "m1_kg=5.972e24, m2_kg=7.342e22, distance_m=3.844e8");
            assertThat(result).contains("Gravitational Force");
        }

        @Test
        void gravitationalForce_negativeDistance() {
            String result = tool.orbitalMechanics("gravitational_force",
                    "m1_kg=1e24, m2_kg=1e22, distance_m=-1");
            assertThat(result).containsIgnoringCase("error").contains("positive");
        }

        @Test
        void unknownCalculation() {
            String result = tool.orbitalMechanics("unknown", "mass_kg=1e30");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("unknown calculation");
        }

        @Test
        void nullCalculation() {
            String result = tool.orbitalMechanics(null, "mass_kg=1e30");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void missingParameter() {
            String result = tool.orbitalMechanics("orbital_velocity", "mass_kg=1e30");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("missing");
        }

        @Test
        void invalidNumericParameter() {
            String result = tool.orbitalMechanics("orbital_velocity", "mass_kg=abc, radius_m=1e6");
            assertThat(result).containsIgnoringCase("error");
        }
    }

    // ── Celestial Coordinates ──

    @Nested
    class CelestialCoordinatesTests {
        @Test
        void raFormat_hmsToDecimal() {
            String result = tool.celestialCoordinates("ra_format", "ra=6:45:09");
            assertThat(result).contains("Right Ascension").contains("101.");
        }

        @Test
        void raFormat_decimalToHms() {
            String result = tool.celestialCoordinates("ra_format", "ra=101.2875");
            assertThat(result).contains("6h");
        }

        @Test
        void raFormat_hmOnly() {
            String result = tool.celestialCoordinates("ra_format", "ra=6:45");
            assertThat(result).contains("Right Ascension");
        }

        @Test
        void decFormat_dmsToDecimal() {
            String result = tool.celestialCoordinates("dec_format", "dec=-16:42:58");
            assertThat(result).contains("Declination").contains("-16.");
        }

        @Test
        void decFormat_decimalToDms() {
            String result = tool.celestialCoordinates("dec_format", "dec=-16.716");
            assertThat(result).contains("Declination").contains("-");
        }

        @Test
        void decFormat_positive() {
            String result = tool.celestialCoordinates("dec_format", "dec=45.5");
            assertThat(result).contains("+");
        }

        @Test
        void decFormat_dmOnly() {
            String result = tool.celestialCoordinates("dec_format", "dec=45:30");
            assertThat(result).contains("Declination");
        }

        @Test
        void equatorialToHorizontal() {
            String result = tool.celestialCoordinates("equatorial_to_horizontal",
                    "ra_hours=6.75, dec_deg=-16.72, latitude_deg=40.0, lst_hours=12.0");
            assertThat(result).contains("Altitude").contains("Azimuth").contains("Hour Angle");
        }

        @Test
        void equatorialToHorizontal_belowHorizon() {
            String result = tool.celestialCoordinates("equatorial_to_horizontal",
                    "ra_hours=0.0, dec_deg=-80.0, latitude_deg=45.0, lst_hours=0.0");
            assertThat(result).contains("below the horizon");
        }

        @Test
        void equatorialToHorizontal_sinHaPositive() {
            String result = tool.celestialCoordinates("equatorial_to_horizontal",
                    "ra_hours=0.0, dec_deg=30.0, latitude_deg=45.0, lst_hours=6.0");
            assertThat(result).contains("Altitude").contains("Azimuth");
        }

        @Test
        void horizontalToEquatorial() {
            String result = tool.celestialCoordinates("horizontal_to_equatorial",
                    "alt_deg=30.0, az_deg=180.0, latitude_deg=40.0, lst_hours=12.0");
            assertThat(result).contains("Right Ascension").contains("Declination");
        }

        @Test
        void horizontalToEquatorial_sinAzPositive() {
            String result = tool.celestialCoordinates("horizontal_to_equatorial",
                    "alt_deg=45.0, az_deg=90.0, latitude_deg=45.0, lst_hours=12.0");
            assertThat(result).contains("Right Ascension");
        }

        @Test
        void horizontalToEquatorial_raWrapNegative() {
            String result = tool.celestialCoordinates("horizontal_to_equatorial",
                    "alt_deg=45.0, az_deg=270.0, latitude_deg=45.0, lst_hours=0.5");
            assertThat(result).contains("Right Ascension");
        }

        @Test
        void angularSeparation_zero() {
            String result = tool.celestialCoordinates("angular_separation",
                    "ra1_deg=100.0, dec1_deg=20.0, ra2_deg=100.0, dec2_deg=20.0");
            assertThat(result).contains("0.000000");
        }

        @Test
        void angularSeparation_ninety() {
            String result = tool.celestialCoordinates("angular_separation",
                    "ra1_deg=0.0, dec1_deg=0.0, ra2_deg=90.0, dec2_deg=0.0");
            assertThat(result).contains("90.0000");
        }

        @Test
        void unknownConversion() {
            String result = tool.celestialCoordinates("unknown", "ra=1.0");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("unknown conversion");
        }

        @Test
        void nullConversion() {
            String result = tool.celestialCoordinates(null, "ra=1.0");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void blankConversion() {
            String result = tool.celestialCoordinates("   ", "ra=1.0");
            assertThat(result).containsIgnoringCase("error");
        }

        @Test
        void missingParameter() {
            String result = tool.celestialCoordinates("ra_format", "");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("missing");
        }

        @Test
        void invalidNumericParameter() {
            String result = tool.celestialCoordinates("equatorial_to_horizontal",
                    "ra_hours=abc, dec_deg=0, latitude_deg=0, lst_hours=0");
            assertThat(result).containsIgnoringCase("error");
        }
    }

    // ── Moon Phase ──

    @Nested
    class MoonPhaseTests {
        @Test
        void specificDate() {
            String result = tool.moonPhase("2025-01-13");
            assertThat(result).contains("Moon Phase").contains("2025-01-13");
        }

        @Test
        void nullDate_usesToday() {
            String result = tool.moonPhase(null);
            assertThat(result).contains("Moon Phase");
        }

        @Test
        void blankDate_usesToday() {
            String result = tool.moonPhase("   ");
            assertThat(result).contains("Moon Phase");
        }

        @Test
        void invalidDateFormat() {
            String result = tool.moonPhase("not-a-date");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("invalid date");
        }

        @Test
        void newMoon_referenceDate() {
            String result = tool.moonPhase("2000-01-06");
            assertThat(result).contains("New Moon");
        }

        @Test
        void dateBeforeReference() {
            String result = tool.moonPhase("1999-12-01");
            assertThat(result).contains("Moon Phase");
        }

        @Test
        void farFutureDate() {
            String result = tool.moonPhase("2050-06-15");
            assertThat(result).contains("Moon Phase");
        }

        @Test
        void allPhaseNames() {
            String r1 = tool.moonPhase("2000-01-06");
            assertThat(r1).contains("New Moon");

            String r2 = tool.moonPhase("2000-01-10");
            assertThat(r2).containsAnyOf("Waxing Crescent", "Waxing");

            String r3 = tool.moonPhase("2000-01-14");
            assertThat(r3).containsAnyOf("First Quarter", "Waxing");

            String r5 = tool.moonPhase("2000-01-21");
            assertThat(r5).containsAnyOf("Full Moon", "Waxing Gibbous", "Waning Gibbous");

            String r7 = tool.moonPhase("2000-01-28");
            assertThat(r7).containsAnyOf("Last Quarter", "Waning");

            String r8 = tool.moonPhase("2000-02-02");
            assertThat(r8).containsAnyOf("Waning Crescent", "Waning", "New Moon");
        }
    }

    // ── Helper Methods ──

    @Nested
    class HelperTests {
        @Test
        void parseParams_emptyString() {
            String result = tool.starProperties("spectral_class", "");
            assertThat(result).contains("Spectral Classification");
        }

        @Test
        void parseParams_nullParams() {
            String result = tool.starProperties("spectral_class", null);
            assertThat(result).contains("Spectral Classification");
        }

        @Test
        void parseParams_noEqualsSign() {
            String result = tool.starProperties("spectral_class", "type");
            assertThat(result).contains("Spectral Classification");
        }

        @Test
        void sci_zeroValue() {
            String result = tool.orbitalMechanics("gravitational_force",
                    "m1_kg=0, m2_kg=1e24, distance_m=1e8");
            assertThat(result).contains("0");
        }

        @Test
        void formatNumber_integer() {
            String result = tool.planetInfo("Earth");
            assertThat(result).contains("6,371");
        }
    }
}
