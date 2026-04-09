package com.behrainwala.mcp.tools;

import com.behrainwala.mcp.model.SearchResult;
import com.behrainwala.mcp.service.WebSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSearchToolTest {

    private WebSearchTool tool;
    private WebSearchService mockService;

    @BeforeEach
    void setUp() {
        mockService = mock(WebSearchService.class);
        tool = new WebSearchTool(mockService);
    }

    @Test
    void search_noResults_returnsNoResultsMessage() {
        when(mockService.search(anyString(), anyInt())).thenReturn(Collections.emptyList());
        String result = tool.search("something obscure", 5);
        assertThat(result).containsIgnoringCase("no results");
    }

    @Test
    void search_withResults_formatsOutput() {
        SearchResult sr = new SearchResult("Test Title", "https://example.com", "A brief snippet.");
        when(mockService.search(anyString(), anyInt())).thenReturn(List.of(sr));
        String result = tool.search("test query", 5);
        assertThat(result).contains("test query").contains("Test Title");
    }

    @Test
    void search_defaultLimit_isUsedWhenNullPassed() {
        when(mockService.search(anyString(), anyInt())).thenReturn(Collections.emptyList());
        // null maxResults should default to 5 without throwing
        String result = tool.search("query", null);
        assertThat(result).isNotNull();
    }
}
