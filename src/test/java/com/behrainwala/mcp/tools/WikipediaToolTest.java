package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WikipediaToolTest {

    private WikipediaTool tool;
    private MockedStatic<Jsoup> jsoupMock;
    private Connection mockConnection;

    @BeforeEach
    void setUp() {
        tool = new WikipediaTool(new McpProperties());
        jsoupMock = mockStatic(Jsoup.class);
        mockConnection = mock(Connection.class, RETURNS_DEEP_STUBS);
        jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
        when(mockConnection.ignoreContentType(true)).thenReturn(mockConnection);
        when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() {
        jsoupMock.close();
    }

    private void mockResponse(String bodyText) throws Exception {
        Document doc = mock(Document.class);
        Element body = mock(Element.class);
        when(doc.body()).thenReturn(body);
        when(body.text()).thenReturn(bodyText);
        when(mockConnection.get()).thenReturn(doc);
    }

    @Nested
    class InputValidation {
        @Test
        void blankTopic_returnsError() {
            String result = tool.lookup("");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
        }

        @Test
        void nullTopic_returnsError() {
            String result = tool.lookup(null);
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
        }

        @Test
        void whitespaceOnly_returnsError() {
            String result = tool.lookup("   ");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
        }
    }

    @Nested
    class HappyPath {
        @Test
        void fullResponse_withAllFields() throws Exception {
            String json = "{\"title\":\"Albert Einstein\","
                    + "\"description\":\"Theoretical physicist\","
                    + "\"extract\":\"Albert Einstein was a German-born physicist.\","
                    + "\"content_urls\":{\"desktop\":{\"page\":\"https://en.wikipedia.org/wiki/Albert_Einstein\"}}}";
            mockResponse(json);

            String result = tool.lookup("Albert Einstein");
            assertThat(result).contains("Wikipedia: Albert Einstein")
                    .contains("Theoretical physicist")
                    .contains("German-born physicist")
                    .contains("https://en.wikipedia.org/wiki/Albert_Einstein");
        }

        @Test
        void responseWithoutDescription() throws Exception {
            String json = "{\"title\":\"Test Topic\","
                    + "\"extract\":\"Some text about the topic.\","
                    + "\"content_urls\":{\"desktop\":{\"page\":\"https://en.wikipedia.org/wiki/Test\"}}}";
            mockResponse(json);

            String result = tool.lookup("Test Topic");
            assertThat(result).contains("Test Topic").contains("Some text about the topic.");
            assertThat(result).doesNotContain("Description:");
        }

        @Test
        void responseWithoutPageUrl() throws Exception {
            String json = "{\"title\":\"Topic\","
                    + "\"extract\":\"Some info.\"}";
            mockResponse(json);

            String result = tool.lookup("Topic");
            assertThat(result).contains("Some info.");
            assertThat(result).doesNotContain("Full article");
        }

        @Test
        void responseWithoutTitle_usesTopic() throws Exception {
            String json = "{\"extract\":\"Some data.\","
                    + "\"content_urls\":{\"desktop\":{\"page\":\"https://example.com\"}}}";
            mockResponse(json);

            String result = tool.lookup("My Topic");
            assertThat(result).contains("My Topic");
        }
    }

    @Nested
    class FallbackToSearch {
        @Test
        void emptyExtract_fallsToSearch() throws Exception {
            // First call returns empty extract, search also fails
            String emptyExtract = "{\"title\":\"Test\",\"extract\":\"\"}";
            String searchJson = "[\"query\",[\"Test Page\"],[\"\"],[\"https://en.wikipedia.org/wiki/Test_Page\"]]";

            // First call → empty extract, second call → search results, third call → summary
            Document doc1 = mock(Document.class);
            Element body1 = mock(Element.class);
            when(doc1.body()).thenReturn(body1);
            when(body1.text()).thenReturn(emptyExtract);

            Document doc2 = mock(Document.class);
            Element body2 = mock(Element.class);
            when(doc2.body()).thenReturn(body2);
            when(body2.text()).thenReturn(searchJson);

            Document doc3 = mock(Document.class);
            Element body3 = mock(Element.class);
            when(doc3.body()).thenReturn(body3);
            when(body3.text()).thenReturn("{\"title\":\"Test Page\",\"extract\":\"Found via search.\"}");

            when(mockConnection.get()).thenReturn(doc1, doc2, doc3);

            String result = tool.lookup("Test");
            assertThat(result).contains("Found via search.");
        }

        @Test
        void exceptionInLookup_fallsToSearch_noResults() throws Exception {
            // First call throws exception, search returns no results
            Document doc2 = mock(Document.class);
            Element body2 = mock(Element.class);
            when(doc2.body()).thenReturn(body2);
            when(body2.text()).thenReturn("[\"query\",[],[],[]]");

            when(mockConnection.get())
                    .thenThrow(new RuntimeException("Connection failed"))
                    .thenReturn(doc2);

            String result = tool.lookup("Nonexistent");
            assertThat(result).containsIgnoringCase("no wikipedia article found");
        }

        @Test
        void exceptionInSearch_returnsSearchFailed() throws Exception {
            // Both lookup and search throw exceptions
            when(mockConnection.get()).thenThrow(new RuntimeException("Network error"));

            String result = tool.lookup("Test");
            assertThat(result).containsIgnoringCase("wikipedia search failed");
        }

        @Test
        void searchReturnsResult_withTitle() throws Exception {
            // First call returns null extract, search finds result
            Document doc1 = mock(Document.class);
            Element body1 = mock(Element.class);
            when(doc1.body()).thenReturn(body1);
            when(body1.text()).thenReturn("{\"title\":\"X\"}"); // No extract

            Document doc2 = mock(Document.class);
            Element body2 = mock(Element.class);
            when(doc2.body()).thenReturn(body2);
            when(body2.text()).thenReturn("[\"query\",[\"Found\"],[\"\"],[\"url\"]]");

            Document doc3 = mock(Document.class);
            Element body3 = mock(Element.class);
            when(doc3.body()).thenReturn(body3);
            when(body3.text()).thenReturn("{\"title\":\"Found\",\"extract\":\"Found article text.\"}");

            when(mockConnection.get()).thenReturn(doc1, doc2, doc3);

            String result = tool.lookup("X");
            assertThat(result).contains("Found article text.");
        }

        @Test
        void searchReturnsResult_emptyExtract() throws Exception {
            // Search finds title but summary has no extract
            Document doc1 = mock(Document.class);
            Element body1 = mock(Element.class);
            when(doc1.body()).thenReturn(body1);
            when(body1.text()).thenReturn("{\"title\":\"X\"}");

            Document doc2 = mock(Document.class);
            Element body2 = mock(Element.class);
            when(doc2.body()).thenReturn(body2);
            when(body2.text()).thenReturn("[\"query\",[\"Result\"],[\"\"],[\"url\"]]");

            Document doc3 = mock(Document.class);
            Element body3 = mock(Element.class);
            when(doc3.body()).thenReturn(body3);
            when(body3.text()).thenReturn("{\"title\":\"Result\",\"extract\":\"\"}");

            when(mockConnection.get()).thenReturn(doc1, doc2, doc3);

            String result = tool.lookup("X");
            assertThat(result).containsIgnoringCase("no wikipedia article found");
        }
    }

    @Nested
    class JsonFieldExtraction {
        @Test
        void nestedFieldPath() throws Exception {
            // Tests extractJsonField with multiple field path elements
            String json = "{\"title\":\"Test\","
                    + "\"extract\":\"Test content.\","
                    + "\"content_urls\":{\"desktop\":{\"page\":\"https://example.com/page\"}}}";
            mockResponse(json);

            String result = tool.lookup("Test");
            assertThat(result).contains("https://example.com/page");
        }

        @Test
        void escapedCharacters() throws Exception {
            String json = "{\"title\":\"Test\","
                    + "\"extract\":\"Line 1\\nLine 2 with \\\"quotes\\\" and \\\\backslash\"}";
            mockResponse(json);

            String result = tool.lookup("Test");
            assertThat(result).contains("Line 1\nLine 2").contains("\"quotes\"");
        }

        @Test
        void nonStringValue_returnsNull() throws Exception {
            // When value after colon is not a string (e.g., number), returns null
            String json = "{\"title\":123,\"extract\":\"Some text.\"}";
            mockResponse(json);

            String result = tool.lookup("Test");
            // title is null (not a string), so falls back to topic
            assertThat(result).contains("Test").contains("Some text.");
        }

        @Test
        void missingField_returnsNull() throws Exception {
            String json = "{\"extract\":\"Content only.\"}";
            mockResponse(json);

            String result = tool.lookup("My Query");
            assertThat(result).contains("My Query").contains("Content only.");
        }
    }
}
