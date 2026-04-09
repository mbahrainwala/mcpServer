package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhysicsToolTest {

    private PhysicsTool tool;

    @BeforeEach
    void setUp() {
        tool = new PhysicsTool();
    }

    @Test
    void physicsConstants_all_containsSpeedOfLight() {
        String result = tool.physicsConstants("all");
        assertThat(result).contains("2.997").containsIgnoringCase("speed of light");
    }

    @Test
    void physicsConstants_mechanics() {
        String result = tool.physicsConstants("mechanics");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("Gravity"), s -> assertThat(s).containsIgnoringCase("gravitational"));
    }

    @Test
    void physicsConstants_electromagnetism() {
        String result = tool.physicsConstants("electromagnetism");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("charge"), s -> assertThat(s).containsIgnoringCase("permittivity"));
    }

    @Test
    void physicsConstants_quantum() {
        String result = tool.physicsConstants("quantum");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("planck"), s -> assertThat(s).containsIgnoringCase("electron"));
    }

    @Test
    void physicsConstants_thermodynamics() {
        String result = tool.physicsConstants("thermodynamics");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("boltzmann"), s -> assertThat(s).containsIgnoringCase("gas"));
    }

    @Test
    void physicsConstants_unknown_query_shows_noMatch() {
        String result = tool.physicsConstants("xyzzy_not_a_constant");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("No constants matched"), s -> assertThat(s).containsIgnoringCase("Try"));
    }
}
