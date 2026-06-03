package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiTextToolTest {

    private AiTextTool tool;

    @BeforeEach
    void setUp() {
        tool = new AiTextTool();
    }

    @Test
    void countTokens_returnsEstimateForGpt() {
        String result = tool.countTokens("The quick brown fox jumps over the lazy dog.", "gpt");
        assertThat(result).contains("Token Estimate").contains("Model family : gpt");
        assertThat(result).matches("(?s).*Estimate {5}: ~\\d+ tokens.*");
    }

    @Test
    void countTokens_claudeFamilyUsesDifferentRatio() {
        String gpt = tool.countTokens("hello world this is a longer phrase", "gpt");
        String claude = tool.countTokens("hello world this is a longer phrase", "claude");
        assertThat(gpt).contains("4.0 chars/token");
        assertThat(claude).contains("3.5 chars/token");
    }

    @Test
    void chunkText_byWordsProducesChunks() {
        String text = "Alpha beta gamma delta epsilon. Zeta eta theta iota kappa. " +
                "Lambda mu nu xi omicron. Pi rho sigma tau upsilon.";
        String result = tool.chunkText(text, 5, 1, "words");
        assertThat(result).contains("---CHUNK 1---");
        assertThat(result).contains("---CHUNK 2---");
    }

    @Test
    void chunkText_overlapMustBeLessThanSize() {
        String result = tool.chunkText("some text", 5, 5, "words");
        assertThat(result).startsWith("Error");
    }

    @Test
    void chunkText_byCharacters() {
        String text = "abcdefghijklmnopqrstuvwxyz0123456789";
        String result = tool.chunkText(text, 10, 2, "characters");
        assertThat(result).contains("---CHUNK 1---");
    }

    @Test
    void extractKeywords_returnsRankedList() {
        String text = "Machine learning models learn from data. Models improve as more data " +
                "is provided. Learning from data is the core concept of machine learning.";
        String result = tool.extractKeywords(text, 5, 3);
        assertThat(result).contains("Top Keywords");
        // 'learning' or 'data' or 'models' should be among the keywords
        assertThat(result.toLowerCase()).containsAnyOf("learning", "data", "models");
    }

    @Test
    void extractKeywords_filtersStopWords() {
        String text = "The the the and and and is is is a a a";
        String result = tool.extractKeywords(text, 5, 3);
        assertThat(result).contains("No keywords found");
    }

    @Test
    void textSimilarity_cosineIdenticalIsOne() {
        String result = tool.textSimilarity("the quick brown fox", "the quick brown fox", "cosine");
        assertThat(result).contains("1.0000");
    }

    @Test
    void textSimilarity_cosineDifferentIsLow() {
        String result = tool.textSimilarity("apple banana cherry", "xenon yttrium zinc", "cosine");
        assertThat(result).contains("0.0000");
    }

    @Test
    void textSimilarity_jaccardWorks() {
        String result = tool.textSimilarity("a b c d", "a b c e", "jaccard");
        assertThat(result).contains("Score:");
        // intersection={a,b,c}, union={a,b,c,d,e} → 3/5 = 0.6
        assertThat(result).contains("0.6000");
    }

    @Test
    void textSimilarity_levenshteinMatch() {
        String result = tool.textSimilarity("kitten", "sitting", "levenshtein");
        assertThat(result).contains("Score:");
        // edit distance is 3, maxLen 7, sim = 1 - 3/7 ≈ 0.5714
        assertThat(result).matches("(?s).*Score: 0\\.5714.*");
    }

    @Test
    void textSimilarity_shingleMatch() {
        String result = tool.textSimilarity("hello world", "hello world", "shingle");
        assertThat(result).contains("1.0000");
    }

    @Test
    void detectLanguage_english() {
        String text = "The quick brown fox jumps over the lazy dog. This is a test sentence written in English.";
        String result = tool.detectLanguage(text);
        assertThat(result).contains("Best guess : English");
    }

    @Test
    void detectLanguage_spanish() {
        String text = "El gato está en la mesa y los perros corren por el jardín de la casa con un juguete.";
        String result = tool.detectLanguage(text);
        assertThat(result).contains("Best guess : Spanish");
    }

    @Test
    void detectLanguage_french() {
        String text = "Le chat est sur la table et les chiens courent dans le jardin de la maison avec un jouet.";
        String result = tool.detectLanguage(text);
        assertThat(result).contains("Best guess : French");
    }

    @Test
    void extractSentences_returnsRequestedCount() {
        String text = "First sentence about cats. Second sentence about dogs. " +
                "Cats are independent animals. Dogs are loyal companions. " +
                "Both cats and dogs make great pets. Pet ownership is rewarding.";
        String result = tool.extractSentences(text, 2);
        assertThat(result).contains("Top 2 Sentences");
    }

    @Test
    void extractSentences_returnsAllWhenFewer() {
        String text = "Short text. Two sentences here.";
        String result = tool.extractSentences(text, 5);
        // When sentence count ≤ topN, returns joined sentences without the header
        assertThat(result).contains("Short text").contains("Two sentences here");
    }
}
