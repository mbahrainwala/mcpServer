package com.behrainwala.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemToolTest {

    private FileSystemTool tool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        tool = new FileSystemTool();
    }

    // ── read_text_file ────────────────────────────────────────────────────────

    @Test
    void readTextFile_returnsContents() throws Exception {
        Path file = tempDir.resolve("notes.txt");
        Files.writeString(file, "Hello, world!");

        String result = tool.readTextFile(file.toString(), null, null);

        assertThat(result)
                .contains("File:")
                .contains("notes.txt")
                .contains("Hello, world!");
    }

    @Test
    void readTextFile_truncatesToMaxChars() throws Exception {
        Path file = tempDir.resolve("big.txt");
        Files.writeString(file, "ABCDEFGHIJ".repeat(10)); // 100 chars

        String result = tool.readTextFile(file.toString(), 10, null);

        assertThat(result)
                .contains("truncated to 10 chars")
                .contains("ABCDEFGHIJ");
        // body after the divider should hold only the 10 retained chars
        String body = result.substring(result.indexOf("─────") + "─────".length()).strip();
        assertThat(body).isEqualTo("ABCDEFGHIJ");
    }

    @Test
    void readTextFile_maxCharsZeroReadsWholeFile() throws Exception {
        Path file = tempDir.resolve("full.txt");
        String content = "X".repeat(8000);
        Files.writeString(file, content);

        String result = tool.readTextFile(file.toString(), 0, null);

        assertThat(result).doesNotContain("truncated");
        assertThat(result).contains(content);
    }

    @Test
    void readTextFile_honoursEncoding() throws Exception {
        Path file = tempDir.resolve("latin.txt");
        Files.write(file, "café".getBytes(StandardCharsets.ISO_8859_1));

        String result = tool.readTextFile(file.toString(), null, "ISO-8859-1");

        assertThat(result).contains("café");
    }

    @Test
    void readTextFile_unknownEncoding_returnsError() throws Exception {
        Path file = tempDir.resolve("x.txt");
        Files.writeString(file, "data");

        String result = tool.readTextFile(file.toString(), null, "NOT-A-CHARSET");

        assertThat(result).contains("unknown encoding");
    }

    @Test
    void readTextFile_missingFile_returnsError() {
        String result = tool.readTextFile(tempDir.resolve("nope.txt").toString(), null, null);
        assertThat(result).contains("file not found");
    }

    @Test
    void readTextFile_directoryPath_returnsError() {
        String result = tool.readTextFile(tempDir.toString(), null, null);
        assertThat(result).contains("path is a directory");
    }

    // ── list_directory ────────────────────────────────────────────────────────

    @Test
    void listDirectory_listsEntries() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "1");
        Files.writeString(tempDir.resolve("b.txt"), "2");
        Files.createDirectory(tempDir.resolve("sub"));

        String result = tool.listDirectory(tempDir.toString(), null, null, null);

        assertThat(result)
                .contains("a.txt")
                .contains("b.txt")
                .contains("[DIR] sub")
                .contains("Files: 2")
                .contains("Dirs: 1");
    }

    @Test
    void listDirectory_appliesGlobFilter() throws Exception {
        Files.writeString(tempDir.resolve("keep.txt"), "1");
        Files.writeString(tempDir.resolve("skip.md"), "2");

        String result = tool.listDirectory(tempDir.toString(), "*.txt", null, null);

        assertThat(result).contains("keep.txt");
        assertThat(result).doesNotContain("skip.md");
    }

    @Test
    void listDirectory_recursesWithDepth() throws Exception {
        Path sub = Files.createDirectory(tempDir.resolve("nested"));
        Files.writeString(sub.resolve("deep.txt"), "x");

        String shallow = tool.listDirectory(tempDir.toString(), null, 1, null);
        assertThat(shallow).doesNotContain("deep.txt");

        String deep = tool.listDirectory(tempDir.toString(), null, 5, null);
        assertThat(deep).contains("deep.txt");
    }

    @Test
    void listDirectory_emptyDir_reportsEmpty() {
        String result = tool.listDirectory(tempDir.toString(), null, null, null);
        assertThat(result).contains("empty");
    }

    @Test
    void listDirectory_missingPath_returnsError() {
        String result = tool.listDirectory(tempDir.resolve("ghost").toString(), null, null, null);
        assertThat(result).contains("does not exist");
    }

    @Test
    void listDirectory_filePath_returnsError() throws Exception {
        Path file = tempDir.resolve("f.txt");
        Files.writeString(file, "x");
        String result = tool.listDirectory(file.toString(), null, null, null);
        assertThat(result).contains("not a directory");
    }

    // ── file_info ─────────────────────────────────────────────────────────────

    @Test
    void fileInfo_reportsFileMetadata() throws Exception {
        Path file = tempDir.resolve("meta.txt");
        Files.writeString(file, "twelve bytes");

        String result = tool.fileInfo(file.toString());

        assertThat(result)
                .contains("Type:     file")
                .contains("Size:")
                .contains("bytes")
                .contains("Modified:")
                .contains("Readable: true");
    }

    @Test
    void fileInfo_reportsDirectoryWithChildCount() throws Exception {
        Files.writeString(tempDir.resolve("c1.txt"), "1");
        Files.writeString(tempDir.resolve("c2.txt"), "2");

        String result = tool.fileInfo(tempDir.toString());

        assertThat(result)
                .contains("Type:     directory")
                .contains("Children: 2");
    }

    @Test
    void fileInfo_missingPath_returnsError() {
        String result = tool.fileInfo(tempDir.resolve("absent").toString());
        assertThat(result).contains("does not exist");
    }
}
