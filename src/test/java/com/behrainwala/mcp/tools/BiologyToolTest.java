package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiologyToolTest {

    private BiologyTool tool;

    @BeforeEach
    void setUp() {
        tool = new BiologyTool();
    }

    @Test
    void dnaOperations_complement_DNA() {
        // Complement of ATGC is TACG
        String result = tool.dnaOperations("complement", "ATGC");
        assertThat(result).contains("TACG");
    }

    @Test
    void dnaOperations_reverseComplement() {
        // Reverse complement of ATGC: complement=TACG, reversed=GCAT
        String result = tool.dnaOperations("reverse_complement", "ATGC");
        assertThat(result).contains("GCAT");
    }

    @Test
    void dnaOperations_transcribe() {
        // Transcription: T→U
        String result = tool.dnaOperations("transcribe", "ATGC");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("UACG"), s -> assertThat(s).contains("AUGC"));
    }

    @Test
    void dnaOperations_gcContent() {
        // GC content of ATGC = 50%
        String result = tool.dnaOperations("gc_content", "ATGC");
        assertThat(result).contains("50");
    }

    @Test
    void dnaOperations_invalidSequence_returnsError() {
        String result = tool.dnaOperations("complement", "ATGXQ");
        assertThat(result).containsIgnoringCase("error").containsIgnoringCase("invalid");
    }

    @Test
    void dnaOperations_translate_startCodon() {
        // ATG = Met (start codon)
        String result = tool.dnaOperations("translate", "ATGAAATAG");
        assertThat(result).satisfiesAnyOf(s -> assertThat(s).contains("Met"), s -> assertThat(s).containsIgnoringCase("protein"));
    }
}
