package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingToolTest {

    private EmbeddingTool tool;

    @BeforeEach
    void setUp() {
        tool = new EmbeddingTool();
    }

    @Test
    void cosine_identicalVectorsIsOne() {
        String result = tool.vectorSimilarity("1,2,3", "1,2,3", "cosine");
        assertThat(result).contains("1.000000");
    }

    @Test
    void cosine_orthogonalIsZero() {
        String result = tool.vectorSimilarity("1,0,0", "0,1,0", "cosine");
        assertThat(result).contains("0.000000");
    }

    @Test
    void cosine_oppositeIsNegativeOne() {
        String result = tool.vectorSimilarity("1,0,0", "-1,0,0", "cosine");
        assertThat(result).contains("-1.000000");
    }

    @Test
    void euclidean_zeroForIdentical() {
        String result = tool.vectorSimilarity("3,4", "3,4", "euclidean");
        assertThat(result).contains("0.000000");
    }

    @Test
    void euclidean_classicTriangle() {
        // (0,0) to (3,4) → distance 5
        String result = tool.vectorSimilarity("0,0", "3,4", "euclidean");
        assertThat(result).contains("5.000000");
    }

    @Test
    void manhattan() {
        String result = tool.vectorSimilarity("0,0", "3,4", "manhattan");
        assertThat(result).contains("7.000000");
    }

    @Test
    void dot_product() {
        String result = tool.vectorSimilarity("1,2,3", "4,5,6", "dot");
        // 1*4 + 2*5 + 3*6 = 32
        assertThat(result).contains("32.000000");
    }

    @Test
    void pearson_perfectCorrelation() {
        String result = tool.vectorSimilarity("1,2,3,4,5", "2,4,6,8,10", "pearson");
        assertThat(result).contains("1.000000");
    }

    @Test
    void dimensionMismatch_returnsError() {
        String result = tool.vectorSimilarity("1,2", "1,2,3", "cosine");
        assertThat(result).contains("Error").contains("differ");
    }

    @Test
    void vectorNormalize_unitLength() {
        String result = tool.vectorNormalize("3,4");
        assertThat(result).contains("Magnitude (L2): 5.000000");
        assertThat(result).contains("0.600000,0.800000");
    }

    @Test
    void vectorNormalize_zeroVectorError() {
        String result = tool.vectorNormalize("0,0,0");
        assertThat(result).startsWith("Error");
    }

    @Test
    void vectorTopK_findsClosest() {
        String query = "1,0,0";
        String candidates = "x: 1,0,0\ny: 0,1,0\nz: 0.9,0.1,0";
        String result = tool.vectorTopK(query, candidates, 2, "cosine");
        assertThat(result).contains("Top 2 matches");
        // x is identical to query — should be #1
        assertThat(result).matches("(?s).* 1\\. x .*");
    }

    @Test
    void vectorTopK_euclideanRanksSmallerFirst() {
        String result = tool.vectorTopK("0,0", "close: 1,0\nfar: 10,0", 2, "euclidean");
        assertThat(result).matches("(?s).* 1\\. close .*");
    }

    @Test
    void vectorCentroid_basicMean() {
        String result = tool.vectorCentroid("a: 1,2,3\nb: 3,4,5\nc: 5,6,7");
        // means: 3, 4, 5
        assertThat(result).contains("3.000000,4.000000,5.000000");
    }

    @Test
    void vectorCentroid_dimensionMismatchError() {
        String result = tool.vectorCentroid("a: 1,2\nb: 1,2,3");
        assertThat(result).contains("Error").contains("dimension mismatch");
    }
}
