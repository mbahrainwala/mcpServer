package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * MCP tool for making HTTP requests to test APIs.
 * Supports GET, POST, PUT, PATCH, DELETE with custom headers and body.
 */
@Service
public class HttpClientTool {

    private static final Logger log = LoggerFactory.getLogger(HttpClientTool.class);

    private final HttpClient client;
    private final McpProperties properties;

    public HttpClientTool(McpProperties properties) {
        this.properties = properties;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Tool(name = "http_request", description = "Make an HTTP request to test an API endpoint. "
            + "Supports GET, POST, PUT, PATCH, DELETE with custom headers and request body. "
            + "Returns status code, response headers, and response body. "
            + "Use this to test and debug REST APIs.")
    public String httpRequest(
            @ToolParam(description = "The full URL (e.g. 'https://api.example.com/users')") String url,
            @ToolParam(description = "HTTP method: GET, POST, PUT, PATCH, DELETE. Default: GET.", required = false) String method,
            @ToolParam(description = "Request headers as key:value pairs separated by newlines. "
                    + "Example: 'Content-Type: application/json\\nAuthorization: Bearer token123'", required = false) String headers,
            @ToolParam(description = "Request body (typically JSON for POST/PUT/PATCH). Optional.", required = false) String body) {

        String httpMethod = (method != null && !method.isBlank()) ? method.strip().toUpperCase() : "GET";

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getFetch().getTimeout()));

            // Add headers
            if (headers != null && !headers.isBlank()) {
                for (String header : headers.split("\\\\n|\\n")) {
                    String[] parts = header.split(":", 2);
                    if (parts.length == 2) {
                        requestBuilder.header(parts[0].strip(), parts[1].strip());
                    }
                }
            }

            // Set method and body
            HttpRequest.BodyPublisher bodyPublisher = (body != null && !body.isBlank())
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody();

            switch (httpMethod) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> requestBuilder.POST(bodyPublisher);
                case "PUT" -> requestBuilder.PUT(bodyPublisher);
                case "PATCH" -> requestBuilder.method("PATCH", bodyPublisher);
                case "DELETE" -> requestBuilder.DELETE();
                default -> requestBuilder.method(httpMethod, bodyPublisher);
            }

            log.debug("HTTP {} {}", httpMethod, url);

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = client.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - startTime;

            StringBuilder sb = new StringBuilder();
            sb.append("HTTP Response\n");
            sb.append("─────────────\n");
            sb.append("Request: ").append(httpMethod).append(" ").append(url).append("\n");
            sb.append("Status: ").append(response.statusCode()).append(" ").append(statusText(response.statusCode())).append("\n");
            sb.append("Time: ").append(elapsed).append("ms\n\n");

            // Response headers (selected important ones)
            sb.append("Response Headers:\n");
            response.headers().map().forEach((key, values) -> {
                if (isImportantHeader(key)) {
                    sb.append("  ").append(key).append(": ").append(String.join(", ", values)).append("\n");
                }
            });

            sb.append("\nResponse Body");
            String responseBody = response.body();
            if (responseBody != null && !responseBody.isEmpty()) {
                sb.append(" (").append(responseBody.length()).append(" chars):\n");
                if (responseBody.length() > 5000) {
                    sb.append(responseBody, 0, 5000).append("\n... [truncated at 5000 chars]");
                } else {
                    sb.append(responseBody);
                }
            } else {
                sb.append(": (empty)");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("HTTP request failed: {} {}: {}", httpMethod, url, e.getMessage());
            return "HTTP request failed\n──────────────────\n"
                    + "URL: " + url + "\n"
                    + "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private boolean isImportantHeader(String name) {
        String lower = name.toLowerCase();
        return lower.contains("content-type") || lower.contains("content-length")
                || lower.contains("cache-control") || lower.contains("location")
                || lower.contains("set-cookie") || lower.contains("authorization")
                || lower.contains("x-") || lower.contains("access-control")
                || lower.contains("etag") || lower.contains("server");
    }

    private String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "";
        };
    }
}
