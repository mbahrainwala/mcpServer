package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.config.McpProperties;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
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

class DictionaryToolTest {

    private DictionaryTool tool;
    private MockedStatic<Jsoup> jsoupMock;
    private Connection mockConnection;

    @BeforeEach
    void setUp() {
        tool = new DictionaryTool(new McpProperties());
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
        void blankWord_returnsError() {
            String result = tool.define("");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
        }

        @Test
        void nullWord_returnsError() {
            String result = tool.define(null);
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
        }

        @Test
        void whitespaceOnly_returnsError() {
            String result = tool.define("   ");
            assertThat(result).containsIgnoringCase("error").containsIgnoringCase("required");
        }
    }

    @Nested
    class HappyPath {
        @Test
        void fullResponse_withPhoneticAndMeanings() throws Exception {
            String json = "[{\"word\":\"hello\",\"phonetic\":\"/həˈloʊ/\","
                    + "\"meanings\":[{\"partOfSpeech\":\"noun\","
                    + "\"definitions\":[{\"definition\":\"A greeting\",\"example\":\"He said hello\"}]},"
                    + "{\"partOfSpeech\":\"verb\","
                    + "\"definitions\":[{\"definition\":\"To greet someone\"}]}]}]";
            mockResponse(json);

            String result = tool.define("hello");
            assertThat(result).contains("hello").contains("/həˈloʊ/")
                    .contains("NOUN").contains("A greeting")
                    .contains("He said hello")
                    .contains("VERB").contains("To greet someone");
        }

        @Test
        void responseWithMultipleDefinitions() throws Exception {
            String json = "[{\"word\":\"run\","
                    + "\"meanings\":[{\"partOfSpeech\":\"verb\","
                    + "\"definitions\":[{\"definition\":\"Move fast\"},{\"definition\":\"Operate a machine\"}]}]}]";
            mockResponse(json);

            String result = tool.define("run");
            assertThat(result).contains("1.").contains("Move fast")
                    .contains("2.").contains("Operate a machine");
        }

        @Test
        void responseWithoutPhonetic() throws Exception {
            String json = "[{\"word\":\"xyz\","
                    + "\"meanings\":[{\"partOfSpeech\":\"noun\","
                    + "\"definitions\":[{\"definition\":\"A test word\"}]}]}]";
            mockResponse(json);

            String result = tool.define("xyz");
            assertThat(result).contains("xyz").contains("A test word");
            assertThat(result).doesNotContain("Pronunciation");
        }

        @Test
        void responseWithNoMeanings_fallbackExtraction() throws Exception {
            // JSON without "meanings" key → falls back to extractAllDefinitions
            String json = "[{\"word\":\"test\",\"definition\":\"A procedure\"}]";
            mockResponse(json);

            String result = tool.define("test");
            assertThat(result).contains("test");
        }

        @Test
        void responseWithEmptyPhonetic() throws Exception {
            String json = "[{\"word\":\"test\",\"phonetic\":\"\","
                    + "\"meanings\":[{\"partOfSpeech\":\"noun\","
                    + "\"definitions\":[{\"definition\":\"An examination\"}]}]}]";
            mockResponse(json);

            String result = tool.define("test");
            assertThat(result).doesNotContain("Pronunciation");
        }
    }

    @Nested
    class ErrorHandling {
        @Test
        void httpStatus404_returnsNoDefinition() throws Exception {
            when(mockConnection.get()).thenThrow(new HttpStatusException("Not Found", 404, "http://example.com"));

            String result = tool.define("zxqwv");
            assertThat(result).containsIgnoringCase("no definition found");
        }

        @Test
        void httpStatusOther_returnsFailureMessage() throws Exception {
            when(mockConnection.get()).thenThrow(new HttpStatusException("Server Error", 500, "http://example.com"));

            String result = tool.define("test");
            assertThat(result).containsIgnoringCase("dictionary lookup failed");
        }

        @Test
        void generalException_returnsFailureMessage() throws Exception {
            when(mockConnection.get()).thenThrow(new RuntimeException("Connection timeout"));

            String result = tool.define("test");
            assertThat(result).containsIgnoringCase("dictionary lookup failed").contains("Connection timeout");
        }
    }

    @Nested
    class JsonParsing {
        @Test
        void escapedQuotesInDefinition() throws Exception {
            String json = "[{\"word\":\"test\","
                    + "\"meanings\":[{\"partOfSpeech\":\"noun\","
                    + "\"definitions\":[{\"definition\":\"A \\\"quoted\\\" word\"}]}]}]";
            mockResponse(json);

            String result = tool.define("test");
            assertThat(result).contains("A \"quoted\" word");
        }

        @Test
        void exampleBetweenDefinitions() throws Exception {
            // Example comes after a definition but before the boundary
            String json = "[{\"word\":\"test\","
                    + "\"meanings\":[{\"partOfSpeech\":\"noun\","
                    + "\"definitions\":[{\"definition\":\"First def\",\"example\":\"Example one\"},"
                    + "{\"definition\":\"Second def\"}]}]}]";
            mockResponse(json);

            String result = tool.define("test");
            assertThat(result).contains("First def").contains("Example one").contains("Second def");
        }

        @Test
        void fallbackExtraction_multipleDefinitions() throws Exception {
            // No meanings key, just definitions scattered
            String json = "{\"definition\":\"First\",\"other\":1,\"definition\":\"Second\"}";
            mockResponse(json);

            String result = tool.define("word");
            assertThat(result).isNotNull();
        }

        @Test
        void wordWithWhitespace_isStripped() throws Exception {
            String json = "[{\"word\":\"hello\","
                    + "\"meanings\":[{\"partOfSpeech\":\"noun\","
                    + "\"definitions\":[{\"definition\":\"A greeting\"}]}]}]";
            mockResponse(json);

            String result = tool.define("  hello  ");
            assertThat(result).contains("hello");
        }
    }
}
