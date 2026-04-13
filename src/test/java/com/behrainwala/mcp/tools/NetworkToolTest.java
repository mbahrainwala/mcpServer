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
        assertThat(result)
                .contains("255.255.255.0")      // subnet mask
                .contains("0.0.0.255")           // wildcard
                .contains("192.168.1.0")         // network
                .contains("192.168.1.255")        // broadcast
                .contains("192.168.1.1")          // first usable
                .contains("192.168.1.254")         // last usable
                .contains("Usable Hosts:      254")
                .contains("Total Hosts:       256");
    }

    @Test
    void subnetCalculator_slash32_singleHost() {
        String result = tool.subnetCalculator("10.0.0.1", 32);
        assertThat(result)
                .contains("255.255.255.255")
                .contains("Usable Hosts:      1")
                .contains("Total Hosts:       1")
                .contains("First Usable Host: 10.0.0.1")
                .contains("Last Usable Host:  10.0.0.1");
    }

    @Test
    void subnetCalculator_slash31_pointToPoint() {
        String result = tool.subnetCalculator("10.0.0.0", 31);
        assertThat(result)
                .contains("Usable Hosts:      2")
                .contains("Total Hosts:       2")
                .contains("First Usable Host: 10.0.0.0")
                .contains("Last Usable Host:  10.0.0.1");
    }

    @Test
    void subnetCalculator_slash0_entireInternet() {
        String result = tool.subnetCalculator("0.0.0.0", 0);
        assertThat(result)
                .contains("0.0.0.0")
                .contains("Subnet Mask:       0.0.0.0")
                .contains("Wildcard Mask:     255.255.255.255")
                .contains("Binary Mask:");
    }

    @Test
    void subnetCalculator_slash8() {
        String result = tool.subnetCalculator("10.0.0.0", 8);
        assertThat(result)
                .contains("255.0.0.0")
                .contains("10.0.0.0")
                .contains("10.255.255.255");
    }

    @Test
    void subnetCalculator_nullIp_returnsError() {
        String result = tool.subnetCalculator(null, 24);
        assertThat(result).isEqualTo("Error: IP address is required.");
    }

    @Test
    void subnetCalculator_blankIp_returnsError() {
        String result = tool.subnetCalculator("   ", 24);
        assertThat(result).isEqualTo("Error: IP address is required.");
    }

    @Test
    void subnetCalculator_cidrNegative_returnsError() {
        String result = tool.subnetCalculator("10.0.0.0", -1);
        assertThat(result).isEqualTo("Error: CIDR must be between 0 and 32.");
    }

    @Test
    void subnetCalculator_cidr33_returnsError() {
        String result = tool.subnetCalculator("10.0.0.0", 33);
        assertThat(result).isEqualTo("Error: CIDR must be between 0 and 32.");
    }

    @Test
    void subnetCalculator_invalidIpFormat_returnsError() {
        String result = tool.subnetCalculator("not.an.ip", 24);
        assertThat(result).startsWith("Error: Invalid IP address format:");
    }

    @Test
    void subnetCalculator_ipWithTooFewOctets_returnsError() {
        String result = tool.subnetCalculator("192.168.1", 24);
        assertThat(result).startsWith("Error: Invalid IP address format:");
    }

    @Test
    void subnetCalculator_octetOutOfRange_returnsError() {
        String result = tool.subnetCalculator("256.0.0.0", 24);
        assertThat(result).startsWith("Error: Invalid IP address format:");
    }

    @Test
    void subnetCalculator_ipWithLeadingTrailingSpaces_works() {
        String result = tool.subnetCalculator("  192.168.1.0  ", 24);
        assertThat(result).contains("192.168.1.0/24").contains("Subnet Calculator Results");
    }

    // ── ipInfo ───────────────────────────────────────────────────────────────

    @Test
    void ipInfo_classA_private_10() {
        String result = tool.ipInfo("10.0.0.1");
        assertThat(result)
                .contains("Class:        A")
                .contains("0.0.0.0 - 127.255.255.255")
                .contains("Type:            Private")
                .contains("10.0.0.0/8")
                .contains("Is Loopback:     No");
    }

    @Test
    void ipInfo_classB_private_172() {
        String result = tool.ipInfo("172.16.0.1");
        assertThat(result)
                .contains("Class:        B")
                .contains("128.0.0.0 - 191.255.255.255")
                .contains("Type:            Private")
                .contains("172.16.0.0/12");
    }

    @Test
    void ipInfo_classC_private_192() {
        String result = tool.ipInfo("192.168.1.100");
        assertThat(result)
                .contains("Class:        C")
                .contains("192.0.0.0 - 223.255.255.255")
                .contains("Type:            Private")
                .contains("192.168.0.0/16");
    }

    @Test
    void ipInfo_classD_multicast() {
        String result = tool.ipInfo("224.0.0.1");
        assertThat(result)
                .contains("Class:        D")
                .contains("Multicast")
                .contains("Is Multicast:    Yes")
                .contains("Type:            Public");
    }

    @Test
    void ipInfo_classE_reserved() {
        String result = tool.ipInfo("240.0.0.1");
        assertThat(result)
                .contains("Class:        E")
                .contains("Reserved/Experimental");
    }

    @Test
    void ipInfo_loopback() {
        String result = tool.ipInfo("127.0.0.1");
        assertThat(result)
                .contains("Is Loopback:     Yes (127.0.0.0/8)")
                .contains("Class:        A");
    }

    @Test
    void ipInfo_linkLocal() {
        String result = tool.ipInfo("169.254.1.1");
        assertThat(result).contains("Is Link-Local:   Yes (169.254.0.0/16)");
    }

    @Test
    void ipInfo_broadcast_255() {
        String result = tool.ipInfo("255.255.255.255");
        assertThat(result).contains("Is Broadcast:    Yes (255.255.255.255)");
    }

    @Test
    void ipInfo_unspecified_0() {
        String result = tool.ipInfo("0.0.0.0");
        assertThat(result).contains("Is Unspecified:  Yes (0.0.0.0)");
    }

    @Test
    void ipInfo_publicIp_notPrivate() {
        String result = tool.ipInfo("8.8.8.8");
        assertThat(result)
                .contains("Type:            Public")
                .doesNotContain("Private Range:");
    }

    @Test
    void ipInfo_nullIp_returnsError() {
        String result = tool.ipInfo(null);
        assertThat(result).isEqualTo("Error: IP address is required.");
    }

    @Test
    void ipInfo_blankIp_returnsError() {
        String result = tool.ipInfo("  ");
        assertThat(result).isEqualTo("Error: IP address is required.");
    }

    @Test
    void ipInfo_invalidIp_returnsError() {
        String result = tool.ipInfo("abc");
        assertThat(result).startsWith("Error: Invalid IP address format:");
    }

    @Test
    void ipInfo_containsHexAndBinaryRepresentations() {
        String result = tool.ipInfo("192.168.1.1");
        assertThat(result).contains("0x").contains("Binary:");
    }

    @Test
    void ipInfo_classB_publicRange() {
        String result = tool.ipInfo("172.15.0.1");
        assertThat(result)
                .contains("Class:        B")
                .contains("Type:            Public");
    }

    @Test
    void ipInfo_172_31_private() {
        String result = tool.ipInfo("172.31.0.1");
        assertThat(result).contains("Type:            Private");
    }

    @Test
    void ipInfo_172_32_notPrivate() {
        String result = tool.ipInfo("172.32.0.1");
        assertThat(result).contains("Type:            Public");
    }

    // ── cidrRange ────────────────────────────────────────────────────────────

    @Test
    void cidrRange_slash30() {
        String result = tool.cidrRange("192.168.1.0/30");
        assertThat(result)
                .contains("192.168.1.0  [NETWORK]")
                .contains("192.168.1.3  [BROADCAST]")
                .contains("192.168.1.1")
                .contains("192.168.1.2")
                .contains("Total Addresses:   4");
    }

    @Test
    void cidrRange_slash32() {
        String result = tool.cidrRange("10.0.0.1/32");
        assertThat(result).contains("10.0.0.1").contains("Total Addresses:   1");
    }

    @Test
    void cidrRange_slash31_noNetworkBroadcastTag() {
        String result = tool.cidrRange("10.0.0.0/31");
        assertThat(result)
                .contains("10.0.0.0")
                .contains("10.0.0.1")
                .doesNotContain("[NETWORK]")
                .doesNotContain("[BROADCAST]");
    }

    @Test
    void cidrRange_null_returnsError() {
        String result = tool.cidrRange(null);
        assertThat(result).isEqualTo("Error: CIDR notation is required.");
    }

    @Test
    void cidrRange_blank_returnsError() {
        String result = tool.cidrRange("   ");
        assertThat(result).isEqualTo("Error: CIDR notation is required.");
    }

    @Test
    void cidrRange_noSlash_returnsError() {
        String result = tool.cidrRange("192.168.1.0");
        assertThat(result).contains("Expected format: x.x.x.x/n");
    }

    @Test
    void cidrRange_invalidIp_returnsError() {
        String result = tool.cidrRange("abc/24");
        assertThat(result).startsWith("Error: Invalid CIDR notation:");
    }

    @Test
    void cidrRange_cidrOutOfRange_returnsError() {
        String result = tool.cidrRange("10.0.0.0/33");
        assertThat(result).contains("CIDR prefix must be between 0 and 32");
    }

    @Test
    void cidrRange_cidrTooLarge_returnsError() {
        String result = tool.cidrRange("10.0.0.0/16");
        assertThat(result).contains("too many to list");
    }

    @Test
    void cidrRange_cidr24_fullRange() {
        String result = tool.cidrRange("192.168.1.0/24");
        assertThat(result)
                .contains("Total Addresses:   256")
                .contains("[NETWORK]")
                .contains("[BROADCAST]");
    }

    // ── portReference ────────────────────────────────────────────────────────

    @Test
    void portReference_byPortNumber_http() {
        String result = tool.portReference("80");
        assertThat(result).contains("HTTP").contains("Well-Known");
    }

    @Test
    void portReference_byPortNumber_ssh() {
        String result = tool.portReference("22");
        assertThat(result).contains("SSH").contains("Secure Shell");
    }

    @Test
    void portReference_byName_ssh() {
        String result = tool.portReference("ssh");
        assertThat(result).contains("SSH").contains("22");
    }

    @Test
    void portReference_byName_caseInsensitive() {
        String result = tool.portReference("MySql");
        assertThat(result).contains("MySQL").contains("3306");
    }

    @Test
    void portReference_unknownPortNumber_showsClassification() {
        String result = tool.portReference("9999");
        assertThat(result).contains("not in the common port database").contains("Registered");
    }

    @Test
    void portReference_dynamicPort() {
        String result = tool.portReference("50000");
        assertThat(result).contains("Dynamic/Private");
    }

    @Test
    void portReference_unknownServiceName() {
        String result = tool.portReference("xyzzy_unknown_service");
        assertThat(result).contains("No results found for query:");
    }

    @Test
    void portReference_null_returnsError() {
        String result = tool.portReference(null);
        assertThat(result).contains("Error: Search query is required.");
    }

    @Test
    void portReference_blank_returnsError() {
        String result = tool.portReference("   ");
        assertThat(result).contains("Error: Search query is required.");
    }

    @Test
    void portReference_registeredPort() {
        String result = tool.portReference("3306");
        assertThat(result).contains("Registered");
    }

    @Test
    void portReference_wellKnownPortRange() {
        String result = tool.portReference("443");
        assertThat(result).contains("0-1023 (Well-Known Ports, assigned by IANA)");
    }

    @Test
    void portReference_searchByDescription() {
        String result = tool.portReference("Database");
        assertThat(result).contains("result(s)");
    }

    // ── subnetCompare ────────────────────────────────────────────────────────

    @Test
    void subnetCompare_sameNetwork_cidrNotation() {
        String result = tool.subnetCompare("192.168.1.10", "192.168.1.200", "24");
        assertThat(result)
                .contains("SAME NETWORK")
                .contains("Both IPs belong to network");
    }

    @Test
    void subnetCompare_differentNetworks_cidr() {
        String result = tool.subnetCompare("192.168.1.10", "192.168.2.10", "24");
        assertThat(result)
                .contains("DIFFERENT NETWORKS")
                .contains("IP1 is on network")
                .contains("IP2 is on network");
    }

    @Test
    void subnetCompare_dottedDecimalMask() {
        String result = tool.subnetCompare("192.168.1.10", "192.168.1.200", "255.255.255.0");
        assertThat(result).contains("SAME NETWORK");
    }

    @Test
    void subnetCompare_maskWithLeadingSlash() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "/24");
        assertThat(result).contains("SAME NETWORK");
    }

    @Test
    void subnetCompare_nullIp1_returnsError() {
        String result = tool.subnetCompare(null, "10.0.0.1", "24");
        assertThat(result).contains("Error: Both IP addresses are required.");
    }

    @Test
    void subnetCompare_blankIp2_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "  ", "24");
        assertThat(result).contains("Error: Both IP addresses are required.");
    }

    @Test
    void subnetCompare_nullMask_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", null);
        assertThat(result).contains("Error: Subnet mask is required");
    }

    @Test
    void subnetCompare_blankMask_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "  ");
        assertThat(result).contains("Error: Subnet mask is required");
    }

    @Test
    void subnetCompare_invalidIp1_returnsError() {
        String result = tool.subnetCompare("xyz", "10.0.0.1", "24");
        assertThat(result).contains("Error: Invalid first IP address:");
    }

    @Test
    void subnetCompare_invalidIp2_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "xyz", "24");
        assertThat(result).contains("Error: Invalid second IP address:");
    }

    @Test
    void subnetCompare_invalidDottedMask_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "255.0.255.0");
        assertThat(result).contains("Error: Invalid subnet mask (not a valid contiguous mask)");
    }

    @Test
    void subnetCompare_invalidDottedMaskFormat_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "abc.def.ghi.jkl");
        assertThat(result).contains("Error: Invalid subnet mask format:");
    }

    @Test
    void subnetCompare_cidrOutOfRange_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "33");
        assertThat(result).contains("Error: CIDR prefix must be between 0 and 32.");
    }

    @Test
    void subnetCompare_invalidMaskNotNumericNotDotted_returnsError() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "abc");
        assertThat(result).contains("Error: Invalid subnet mask.");
    }

    @Test
    void subnetCompare_slash31_noHostRange() {
        String result = tool.subnetCompare("10.0.0.0", "10.0.0.1", "31");
        assertThat(result)
                .contains("SAME NETWORK")
                .doesNotContain("Host Range:");
    }

    @Test
    void subnetCompare_slash24_hasHostRange() {
        String result = tool.subnetCompare("10.0.0.1", "10.0.0.2", "24");
        assertThat(result).contains("Host Range:");
    }
}
