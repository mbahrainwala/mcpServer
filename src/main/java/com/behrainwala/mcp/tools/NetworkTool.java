package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP tool for network/IP operations including subnet calculation,
 * IP address information, CIDR range listing, port reference, and subnet comparison.
 */
@Service
public class NetworkTool {

    // ──────────────────────────────────────────────
    // Common port reference data
    // ──────────────────────────────────────────────

    private static final List<PortEntry> PORT_DATABASE = new ArrayList<>();

    static {
        PORT_DATABASE.add(new PortEntry(20, "TCP", "FTP-Data", "File Transfer Protocol (data channel)"));
        PORT_DATABASE.add(new PortEntry(21, "TCP", "FTP", "File Transfer Protocol (control channel)"));
        PORT_DATABASE.add(new PortEntry(22, "TCP", "SSH", "Secure Shell"));
        PORT_DATABASE.add(new PortEntry(23, "TCP", "Telnet", "Telnet remote login"));
        PORT_DATABASE.add(new PortEntry(25, "TCP", "SMTP", "Simple Mail Transfer Protocol"));
        PORT_DATABASE.add(new PortEntry(53, "TCP/UDP", "DNS", "Domain Name System"));
        PORT_DATABASE.add(new PortEntry(67, "UDP", "DHCP-Server", "Dynamic Host Configuration Protocol (server)"));
        PORT_DATABASE.add(new PortEntry(68, "UDP", "DHCP-Client", "Dynamic Host Configuration Protocol (client)"));
        PORT_DATABASE.add(new PortEntry(69, "UDP", "TFTP", "Trivial File Transfer Protocol"));
        PORT_DATABASE.add(new PortEntry(80, "TCP", "HTTP", "Hypertext Transfer Protocol"));
        PORT_DATABASE.add(new PortEntry(110, "TCP", "POP3", "Post Office Protocol v3"));
        PORT_DATABASE.add(new PortEntry(111, "TCP/UDP", "RPC", "Remote Procedure Call"));
        PORT_DATABASE.add(new PortEntry(119, "TCP", "NNTP", "Network News Transfer Protocol"));
        PORT_DATABASE.add(new PortEntry(123, "UDP", "NTP", "Network Time Protocol"));
        PORT_DATABASE.add(new PortEntry(135, "TCP/UDP", "MS-RPC", "Microsoft Remote Procedure Call"));
        PORT_DATABASE.add(new PortEntry(137, "UDP", "NetBIOS-NS", "NetBIOS Name Service"));
        PORT_DATABASE.add(new PortEntry(138, "UDP", "NetBIOS-DGM", "NetBIOS Datagram Service"));
        PORT_DATABASE.add(new PortEntry(139, "TCP", "NetBIOS-SSN", "NetBIOS Session Service"));
        PORT_DATABASE.add(new PortEntry(143, "TCP", "IMAP", "Internet Message Access Protocol"));
        PORT_DATABASE.add(new PortEntry(161, "UDP", "SNMP", "Simple Network Management Protocol"));
        PORT_DATABASE.add(new PortEntry(162, "UDP", "SNMP-Trap", "SNMP Trap"));
        PORT_DATABASE.add(new PortEntry(389, "TCP/UDP", "LDAP", "Lightweight Directory Access Protocol"));
        PORT_DATABASE.add(new PortEntry(443, "TCP", "HTTPS", "HTTP over TLS/SSL"));
        PORT_DATABASE.add(new PortEntry(445, "TCP", "SMB", "Server Message Block / Microsoft-DS"));
        PORT_DATABASE.add(new PortEntry(465, "TCP", "SMTPS", "SMTP over SSL (deprecated, now Submission over TLS)"));
        PORT_DATABASE.add(new PortEntry(514, "UDP", "Syslog", "System Logging Protocol"));
        PORT_DATABASE.add(new PortEntry(587, "TCP", "Submission", "Email Message Submission (SMTP with STARTTLS)"));
        PORT_DATABASE.add(new PortEntry(636, "TCP", "LDAPS", "LDAP over SSL/TLS"));
        PORT_DATABASE.add(new PortEntry(993, "TCP", "IMAPS", "IMAP over SSL/TLS"));
        PORT_DATABASE.add(new PortEntry(995, "TCP", "POP3S", "POP3 over SSL/TLS"));
        PORT_DATABASE.add(new PortEntry(1080, "TCP", "SOCKS", "SOCKS Proxy"));
        PORT_DATABASE.add(new PortEntry(1433, "TCP", "MSSQL", "Microsoft SQL Server"));
        PORT_DATABASE.add(new PortEntry(1521, "TCP", "Oracle", "Oracle Database"));
        PORT_DATABASE.add(new PortEntry(1723, "TCP", "PPTP", "Point-to-Point Tunneling Protocol"));
        PORT_DATABASE.add(new PortEntry(2049, "TCP/UDP", "NFS", "Network File System"));
        PORT_DATABASE.add(new PortEntry(2181, "TCP", "ZooKeeper", "Apache ZooKeeper"));
        PORT_DATABASE.add(new PortEntry(3306, "TCP", "MySQL", "MySQL Database"));
        PORT_DATABASE.add(new PortEntry(3389, "TCP/UDP", "RDP", "Remote Desktop Protocol"));
        PORT_DATABASE.add(new PortEntry(5432, "TCP", "PostgreSQL", "PostgreSQL Database"));
        PORT_DATABASE.add(new PortEntry(5672, "TCP", "AMQP", "Advanced Message Queuing Protocol (RabbitMQ)"));
        PORT_DATABASE.add(new PortEntry(5900, "TCP", "VNC", "Virtual Network Computing"));
        PORT_DATABASE.add(new PortEntry(6379, "TCP", "Redis", "Redis In-Memory Data Store"));
        PORT_DATABASE.add(new PortEntry(6443, "TCP", "Kubernetes", "Kubernetes API Server"));
        PORT_DATABASE.add(new PortEntry(8080, "TCP", "HTTP-Alt", "HTTP Alternate (commonly used for proxies/dev servers)"));
        PORT_DATABASE.add(new PortEntry(8443, "TCP", "HTTPS-Alt", "HTTPS Alternate"));
        PORT_DATABASE.add(new PortEntry(8888, "TCP", "HTTP-Alt-2", "HTTP Alternate (Jupyter Notebook default)"));
        PORT_DATABASE.add(new PortEntry(9090, "TCP", "Prometheus", "Prometheus Metrics"));
        PORT_DATABASE.add(new PortEntry(9092, "TCP", "Kafka", "Apache Kafka Broker"));
        PORT_DATABASE.add(new PortEntry(9200, "TCP", "Elasticsearch", "Elasticsearch HTTP"));
        PORT_DATABASE.add(new PortEntry(11211, "TCP/UDP", "Memcached", "Memcached"));
        PORT_DATABASE.add(new PortEntry(27017, "TCP", "MongoDB", "MongoDB Database"));
    }

    // ──────────────────────────────────────────────
    // Tool 1: Subnet Calculator
    // ──────────────────────────────────────────────

    @Tool(name = "network_subnet_calculator", description = "Calculate subnet details from an IP address and CIDR prefix length. "
            + "Returns network address, broadcast address, first/last usable host, total hosts, usable hosts, "
            + "wildcard mask, and subnet mask in dotted decimal. Handles all CIDR values from 0 to 32.")
    public String subnetCalculator(
            @ToolParam(description = "IPv4 address in dotted decimal (e.g. '192.168.1.0')") String ip_address,
            @ToolParam(description = "CIDR prefix length (0-32)") int cidr) {

        if (ip_address == null || ip_address.isBlank()) {
            return "Error: IP address is required.";
        }
        if (cidr < 0 || cidr > 32) {
            return "Error: CIDR must be between 0 and 32.";
        }

        long ip;
        try {
            ip = ipToLong(ip_address.strip());
        } catch (IllegalArgumentException e) {
            return "Error: Invalid IP address format: " + e.getMessage();
        }

        long subnetMask = cidrToMask(cidr);
        long wildcardMask = ~subnetMask & 0xFFFFFFFFL;
        long networkAddress = ip & subnetMask;
        long broadcastAddress = networkAddress | wildcardMask;
        long totalHosts = (long) Math.pow(2, 32 - cidr);
        long usableHosts;
        String firstUsable;
        String lastUsable;

        if (cidr == 32) {
            usableHosts = 1;
            firstUsable = longToIp(networkAddress);
            lastUsable = longToIp(networkAddress);
        } else if (cidr == 31) {
            // Point-to-point link (RFC 3021)
            usableHosts = 2;
            firstUsable = longToIp(networkAddress);
            lastUsable = longToIp(broadcastAddress);
        } else if (cidr == 0) {
            usableHosts = totalHosts - 2;
            firstUsable = longToIp(networkAddress + 1);
            lastUsable = longToIp(broadcastAddress - 1);
        } else {
            usableHosts = totalHosts - 2;
            firstUsable = longToIp(networkAddress + 1);
            lastUsable = longToIp(broadcastAddress - 1);
        }

        return "Subnet Calculator Results\n" +
                "────────────────────────\n" +
                "Input IP:          " + ip_address.strip() + "/" + cidr + "\n\n" +
                "Network Address:   " + longToIp(networkAddress) + "\n" +
                "Broadcast Address: " + longToIp(broadcastAddress) + "\n" +
                "Subnet Mask:       " + longToIp(subnetMask) + "\n" +
                "Wildcard Mask:     " + longToIp(wildcardMask) + "\n\n" +
                "First Usable Host: " + firstUsable + "\n" +
                "Last Usable Host:  " + lastUsable + "\n" +
                "Total Hosts:       " + totalHosts + "\n" +
                "Usable Hosts:      " + usableHosts + "\n\n" +
                "CIDR Notation:     /" + cidr + "\n" +
                "Binary Mask:       " + longToBinaryDotted(subnetMask) + "\n";
    }

    // ──────────────────────────────────────────────
    // Tool 2: IP Address Information
    // ──────────────────────────────────────────────

    @Tool(name = "network_ip_info", description = "Get detailed information about an IPv4 address. "
            + "Returns: IP class (A/B/C/D/E), private or public, binary and hex representation, "
            + "whether it is loopback, link-local, or multicast.")
    public String ipInfo(
            @ToolParam(description = "IPv4 address in dotted decimal (e.g. '10.0.0.1')") String ip_address) {

        if (ip_address == null || ip_address.isBlank()) {
            return "Error: IP address is required.";
        }

        long ip;
        try {
            ip = ipToLong(ip_address.strip());
        } catch (IllegalArgumentException e) {
            return "Error: Invalid IP address format: " + e.getMessage();
        }

        int firstOctet = (int) ((ip >> 24) & 0xFF);
        int secondOctet = (int) ((ip >> 16) & 0xFF);

        // Determine class
        String ipClass;
        String classRange;
        if (firstOctet >= 0 && firstOctet <= 127) {
            ipClass = "A";
            classRange = "0.0.0.0 - 127.255.255.255";
        } else if (firstOctet >= 128 && firstOctet <= 191) {
            ipClass = "B";
            classRange = "128.0.0.0 - 191.255.255.255";
        } else if (firstOctet >= 192 && firstOctet <= 223) {
            ipClass = "C";
            classRange = "192.0.0.0 - 223.255.255.255";
        } else if (firstOctet >= 224 && firstOctet <= 239) {
            ipClass = "D";
            classRange = "224.0.0.0 - 239.255.255.255 (Multicast)";
        } else {
            ipClass = "E";
            classRange = "240.0.0.0 - 255.255.255.255 (Reserved/Experimental)";
        }

        // Private check (RFC 1918 + others)
        boolean isPrivate = false;
        String privateRange = "N/A";
        if (firstOctet == 10) {
            isPrivate = true;
            privateRange = "10.0.0.0/8";
        } else if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
            isPrivate = true;
            privateRange = "172.16.0.0/12";
        } else if (firstOctet == 192 && secondOctet == 168) {
            isPrivate = true;
            privateRange = "192.168.0.0/16";
        }

        // Special addresses
        boolean isLoopback = firstOctet == 127;
        boolean isLinkLocal = (firstOctet == 169 && secondOctet == 254);
        boolean isMulticast = (firstOctet >= 224 && firstOctet <= 239);
        boolean isBroadcast = (ip == 0xFFFFFFFFL);
        boolean isUnspecified = (ip == 0L);

        // Binary representation
        String binary = longToBinaryDotted(ip);

        // Hex representation
        String hex = String.format("0x%08X", ip);

        StringBuilder sb = new StringBuilder();
        sb.append("IP Address Information\n");
        sb.append("─────────────────────\n");
        sb.append("IP Address:      ").append(ip_address.strip()).append("\n");
        sb.append("Decimal Value:   ").append(ip).append("\n");
        sb.append("Hex Value:       ").append(hex).append("\n");
        sb.append("Binary:          ").append(binary).append("\n\n");

        sb.append("CLASSIFICATION\n");
        sb.append("──────────────\n");
        sb.append("IP Class:        ").append(ipClass).append("\n");
        sb.append("Class Range:     ").append(classRange).append("\n");
        sb.append("Type:            ").append(isPrivate ? "Private" : "Public").append("\n");
        if (isPrivate) {
            sb.append("Private Range:   ").append(privateRange).append("\n");
        }
        sb.append("\n");

        sb.append("SPECIAL FLAGS\n");
        sb.append("─────────────\n");
        sb.append("Is Loopback:     ").append(isLoopback ? "Yes (127.0.0.0/8)" : "No").append("\n");
        sb.append("Is Link-Local:   ").append(isLinkLocal ? "Yes (169.254.0.0/16)" : "No").append("\n");
        sb.append("Is Multicast:    ").append(isMulticast ? "Yes (224.0.0.0/4)" : "No").append("\n");
        sb.append("Is Broadcast:    ").append(isBroadcast ? "Yes (255.255.255.255)" : "No").append("\n");
        sb.append("Is Unspecified:  ").append(isUnspecified ? "Yes (0.0.0.0)" : "No").append("\n");

        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // Tool 3: CIDR Range Listing
    // ──────────────────────────────────────────────

    @Tool(name = "network_cidr_range", description = "List all IP addresses in a CIDR range. "
            + "Only supports /24 or smaller ranges (up to 256 addresses) to avoid huge output. "
            + "Marks network and broadcast addresses in the output.")
    public String cidrRange(
            @ToolParam(description = "CIDR notation (e.g. '192.168.1.0/28')") String cidr_notation) {

        if (cidr_notation == null || cidr_notation.isBlank()) {
            return "Error: CIDR notation is required.";
        }

        String trimmed = cidr_notation.strip();
        String[] parts = trimmed.split("/");
        if (parts.length != 2) {
            return "Error: Invalid CIDR notation. Expected format: x.x.x.x/n";
        }

        long ip;
        int cidr;
        try {
            ip = ipToLong(parts[0]);
            cidr = Integer.parseInt(parts[1]);
        } catch (IllegalArgumentException e) {
            return "Error: Invalid CIDR notation: " + e.getMessage();
        }

        if (cidr < 0 || cidr > 32) {
            return "Error: CIDR prefix must be between 0 and 32.";
        }
        if (cidr < 24) {
            return "Error: CIDR prefix must be /24 or larger (24-32) to limit output. "
                    + "A /" + cidr + " range contains " + (long) Math.pow(2, 32 - cidr)
                    + " addresses which is too many to list.";
        }

        long mask = cidrToMask(cidr);
        long networkAddress = ip & mask;
        long broadcastAddress = networkAddress | (~mask & 0xFFFFFFFFL);
        long totalAddresses = broadcastAddress - networkAddress + 1;

        StringBuilder sb = new StringBuilder();
        sb.append("CIDR Range: ").append(trimmed).append("\n");
        sb.append("─────────────────────────────\n");
        sb.append("Network Address:   ").append(longToIp(networkAddress)).append("\n");
        sb.append("Broadcast Address: ").append(longToIp(broadcastAddress)).append("\n");
        sb.append("Total Addresses:   ").append(totalAddresses).append("\n\n");
        sb.append("ALL ADDRESSES\n");
        sb.append("─────────────\n");

        for (long addr = networkAddress; addr <= broadcastAddress; addr++) {
            String ipStr = longToIp(addr);
            if (addr == networkAddress && cidr < 31) {
                sb.append("  ").append(ipStr).append("  [NETWORK]\n");
            } else if (addr == broadcastAddress && cidr < 31) {
                sb.append("  ").append(ipStr).append("  [BROADCAST]\n");
            } else {
                sb.append("  ").append(ipStr).append("\n");
            }
        }

        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // Tool 4: Port Reference
    // ──────────────────────────────────────────────

    @Tool(name = "network_port_reference", description = "Look up common network port information. "
            + "Search by port number or service name. Returns port number, protocol, service name, "
            + "description, and port classification (well-known, registered, or dynamic).")
    public String portReference(
            @ToolParam(description = "Port number (e.g. '443') or service name (e.g. 'SSH', 'HTTP', 'MySQL')") String query) {

        if (query == null || query.isBlank()) {
            return "Error: Search query is required. Provide a port number or service name.";
        }

        String q = query.strip();
        List<PortEntry> results = new ArrayList<>();

        // Try matching by port number
        try {
            int portNum = Integer.parseInt(q);
            for (PortEntry entry : PORT_DATABASE) {
                if (entry.port == portNum) {
                    results.add(entry);
                }
            }
        } catch (NumberFormatException e) {
            // Not a number, search by name
            String lower = q.toLowerCase();
            for (PortEntry entry : PORT_DATABASE) {
                if (entry.service.toLowerCase().contains(lower)
                        || entry.description.toLowerCase().contains(lower)
                        || entry.protocol.toLowerCase().contains(lower)) {
                    results.add(entry);
                }
            }
        }

        if (results.isEmpty()) {
            // If searching by number didn't find in database, still provide classification
            try {
                int portNum = Integer.parseInt(q);
                if (portNum >= 0 && portNum <= 65535) {
                    return "Port " + portNum + " is not in the common port database.\n\n" +
                            "Classification: " + classifyPort(portNum) + "\n" +
                            "Port Range:     " + portRangeDescription(portNum) + "\n";
                }
            } catch (NumberFormatException ignored) {
            }
            return "No results found for query: '" + q + "'\n"
                    + "Try searching by port number (e.g. '22') or service name (e.g. 'SSH').";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Port Reference Results\n");
        sb.append("─────────────────────\n");
        sb.append("Query: ").append(q).append("\n");
        sb.append("Found: ").append(results.size()).append(" result(s)\n\n");

        for (PortEntry entry : results) {
            sb.append("PORT ").append(entry.port).append("\n");
            sb.append("  Service:        ").append(entry.service).append("\n");
            sb.append("  Protocol:       ").append(entry.protocol).append("\n");
            sb.append("  Description:    ").append(entry.description).append("\n");
            sb.append("  Classification: ").append(classifyPort(entry.port)).append("\n");
            sb.append("  Range:          ").append(portRangeDescription(entry.port)).append("\n\n");
        }

        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // Tool 5: Subnet Compare
    // ──────────────────────────────────────────────

    @Tool(name = "network_subnet_compare", description = "Compare two IP addresses to determine if they are on the same subnet. "
            + "Accepts a subnet mask in either dotted decimal (e.g. '255.255.255.0') or CIDR notation (e.g. '24'). "
            + "Returns whether the IPs share the same network, each IP's network address, and the common subnet.")
    public String subnetCompare(
            @ToolParam(description = "First IPv4 address (e.g. '192.168.1.10')") String ip1,
            @ToolParam(description = "Second IPv4 address (e.g. '192.168.1.200')") String ip2,
            @ToolParam(description = "Subnet mask in dotted decimal (e.g. '255.255.255.0') or CIDR prefix length (e.g. '24')") String subnet_mask) {

        if (ip1 == null || ip1.isBlank() || ip2 == null || ip2.isBlank()) {
            return "Error: Both IP addresses are required.";
        }
        if (subnet_mask == null || subnet_mask.isBlank()) {
            return "Error: Subnet mask is required (dotted decimal or CIDR prefix).";
        }

        long ipAddr1, ipAddr2, mask;
        int cidr;
        try {
            ipAddr1 = ipToLong(ip1.strip());
        } catch (IllegalArgumentException e) {
            return "Error: Invalid first IP address: " + e.getMessage();
        }
        try {
            ipAddr2 = ipToLong(ip2.strip());
        } catch (IllegalArgumentException e) {
            return "Error: Invalid second IP address: " + e.getMessage();
        }

        String maskTrimmed = subnet_mask.strip();
        // Remove leading slash if present (e.g. "/24")
        if (maskTrimmed.startsWith("/")) {
            maskTrimmed = maskTrimmed.substring(1);
        }

        if (maskTrimmed.contains(".")) {
            // Dotted decimal mask
            try {
                mask = ipToLong(maskTrimmed);
                cidr = maskToCidr(mask);
                if (cidr == -1) {
                    return "Error: Invalid subnet mask (not a valid contiguous mask): " + maskTrimmed;
                }
            } catch (IllegalArgumentException e) {
                return "Error: Invalid subnet mask format: " + e.getMessage();
            }
        } else {
            // CIDR prefix
            try {
                cidr = Integer.parseInt(maskTrimmed);
                if (cidr < 0 || cidr > 32) {
                    return "Error: CIDR prefix must be between 0 and 32.";
                }
                mask = cidrToMask(cidr);
            } catch (NumberFormatException e) {
                return "Error: Invalid subnet mask. Use dotted decimal (255.255.255.0) or CIDR (24).";
            }
        }

        long network1 = ipAddr1 & mask;
        long network2 = ipAddr2 & mask;
        boolean sameNetwork = (network1 == network2);

        long wildcardMask = ~mask & 0xFFFFFFFFL;
        long broadcast1 = network1 | wildcardMask;
        long broadcast2 = network2 | wildcardMask;

        StringBuilder sb = new StringBuilder();
        sb.append("Subnet Comparison\n");
        sb.append("─────────────────\n\n");

        sb.append("RESULT: ").append(sameNetwork ? "SAME NETWORK" : "DIFFERENT NETWORKS").append("\n\n");

        sb.append("Subnet Mask:       ").append(longToIp(mask)).append(" (/").append(cidr).append(")\n");
        sb.append("Wildcard Mask:     ").append(longToIp(wildcardMask)).append("\n\n");

        sb.append("IP 1: ").append(ip1.strip()).append("\n");
        sb.append("  Network Address:   ").append(longToIp(network1)).append("\n");
        sb.append("  Broadcast Address: ").append(longToIp(broadcast1)).append("\n");
        if (cidr < 31) {
            sb.append("  Host Range:        ").append(longToIp(network1 + 1))
                    .append(" - ").append(longToIp(broadcast1 - 1)).append("\n");
        }
        sb.append("\n");

        sb.append("IP 2: ").append(ip2.strip()).append("\n");
        sb.append("  Network Address:   ").append(longToIp(network2)).append("\n");
        sb.append("  Broadcast Address: ").append(longToIp(broadcast2)).append("\n");
        if (cidr < 31) {
            sb.append("  Host Range:        ").append(longToIp(network2 + 1))
                    .append(" - ").append(longToIp(broadcast2 - 1)).append("\n");
        }
        sb.append("\n");

        if (sameNetwork) {
            sb.append("Both IPs belong to network ").append(longToIp(network1)).append("/").append(cidr).append("\n");
        } else {
            sb.append("IP1 is on network ").append(longToIp(network1)).append("/").append(cidr).append("\n");
            sb.append("IP2 is on network ").append(longToIp(network2)).append("/").append(cidr).append("\n");
        }

        return sb.toString();
    }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    /**
     * Parse dotted-decimal IPv4 address to a long (unsigned 32-bit).
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Expected 4 octets, got " + octets.length + " in '" + ip + "'");
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            int octet;
            try {
                octet = Integer.parseInt(octets[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid octet '" + octets[i] + "' in '" + ip + "'");
            }
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Octet out of range (0-255): " + octet + " in '" + ip + "'");
            }
            result = (result << 8) | octet;
        }
        return result;
    }

    /**
     * Convert a long (unsigned 32-bit) back to dotted-decimal IPv4 string.
     */
    private String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }

    /**
     * Convert a CIDR prefix length to a subnet mask as a long.
     */
    private long cidrToMask(int cidr) {
        if (cidr == 0) {
            return 0L;
        }
        return (0xFFFFFFFFL << (32 - cidr)) & 0xFFFFFFFFL;
    }

    /**
     * Convert a subnet mask (long) to CIDR prefix length.
     * Returns -1 if the mask is not a valid contiguous mask.
     */
    private int maskToCidr(long mask) {
        // Check contiguity: mask must be leading 1s followed by 0s
        int cidr = 0;
        boolean foundZero = false;
        for (int i = 31; i >= 0; i--) {
            boolean bit = ((mask >> i) & 1) == 1;
            if (bit) {
                if (foundZero) {
                    return -1; // Non-contiguous mask
                }
                cidr++;
            } else {
                foundZero = true;
            }
        }
        return cidr;
    }

    /**
     * Convert a long to dotted binary representation (e.g. "11000000.10101000.00000001.00000000").
     */
    private String longToBinaryDotted(long ip) {
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            int octet = (int) ((ip >> (i * 8)) & 0xFF);
            String bin = String.format("%8s", Integer.toBinaryString(octet)).replace(' ', '0');
            sb.append(bin);
            if (i > 0) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    /**
     * Classify a port number as well-known, registered, or dynamic/private.
     */
    private String classifyPort(int port) {
        if (port >= 0 && port <= 1023) {
            return "Well-Known";
        } else if (port >= 1024 && port <= 49151) {
            return "Registered";
        } else if (port >= 49152 && port <= 65535) {
            return "Dynamic/Private";
        }
        return "Invalid";
    }

    /**
     * Return the port range description.
     */
    private String portRangeDescription(int port) {
        if (port >= 0 && port <= 1023) {
            return "0-1023 (Well-Known Ports, assigned by IANA)";
        } else if (port >= 1024 && port <= 49151) {
            return "1024-49151 (Registered Ports)";
        } else if (port >= 49152 && port <= 65535) {
            return "49152-65535 (Dynamic/Private Ports)";
        }
        return "Invalid port range";
    }

    // ──────────────────────────────────────────────
    // Inner class for port entries
    // ──────────────────────────────────────────────

    private static class PortEntry {
        final int port;
        final String protocol;
        final String service;
        final String description;

        PortEntry(int port, String protocol, String service, String description) {
            this.port = port;
            this.protocol = protocol;
            this.service = service;
            this.description = description;
        }
    }
}
