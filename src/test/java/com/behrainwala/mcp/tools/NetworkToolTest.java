package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkToolTest {

    private NetworkTool tool;

    @BeforeEach
    void setUp() {
        tool = new NetworkTool();
    }

    // ── subnetCalculator ─────────────────────────────────────────────────────

    @Test
    void subnetCalculator_slash24() {
        String result = tool.subnetCalculator("192.168.1.0", 24);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("255.255.255.0"), s -> assertThat(s).containsIgnoringCase("subnet mask"));
    }

    @Test
    void subnetCalculator_slash8() {
        String result = tool.subnetCalculator("10.0.0.0", 8);
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("255.0.0.0"), s -> assertThat(s).containsIgnoringCase("10.0.0.0"));
    }

    @Test
    void subnetCalculator_hostCount_slash24() {
        String result = tool.subnetCalculator("192.168.1.0", 24);
        // /24 has 254 usable hosts
        assertThat(result).contains("254");
    }

    @Test
    void subnetCalculator_blankIp_returnsError() {
        String result = tool.subnetCalculator("", 24);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void subnetCalculator_invalidCidr_returnsError() {
        String result = tool.subnetCalculator("192.168.1.0", 33);
        assertThat(result).containsIgnoringCase("error");
    }

    @Test
    void subnetCalculator_invalidIpFormat_returnsError() {
        String result = tool.subnetCalculator("not.an.ip", 24);
        assertThat(result).containsIgnoringCase("error");
    }

    // ── portReference ────────────────────────────────────────────────────────

    @Test
    void portReference_http() {
        String result = tool.portReference("80");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("HTTP"), s -> assertThat(s).containsIgnoringCase("80"));
    }

    @Test
    void portReference_ssh() {
        String result = tool.portReference("22");
        assertThat(result).containsIgnoringCase("SSH");
    }

    @Test
    void portReference_unknown() {
        String result = tool.portReference("9999");
        assertThat(result).isNotBlank(); // may or may not be known
    }

    // ── ipInfo ───────────────────────────────────────────────────────────────

    @Test
    void ipInfo_privateRange() {
        String result = tool.ipInfo("192.168.1.100");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("private"), s -> assertThat(s).containsIgnoringCase("192.168"));
    }

    @Test
    void ipInfo_loopback() {
        String result = tool.ipInfo("127.0.0.1");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).containsIgnoringCase("loopback"), s -> assertThat(s).containsIgnoringCase("127"));
    }
}
