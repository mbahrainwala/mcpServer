package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FileSystemTool {

    private static final DateTimeFormatter TS = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @Tool(name = "list_directory",
          description = "List files and subdirectories in a folder. "
                  + "Returns name, type (file/dir), size, and last-modified for each entry. "
                  + "Use 'recursive=true' to walk subdirectories. "
                  + "Use 'filter' to match a glob pattern (e.g. '*.java', '*.txt'). "
                  + "Use 'maxDepth' to limit recursion depth (default 1 = immediate children). "
                  + "Replaces manually calling ls / dir when an LLM needs to enumerate a folder.")
    public String listDirectory(
            @ToolParam(description = "Absolute path to the directory (e.g. 'C:/projects/myapp' or '/home/user/docs')")
            String path,
            @ToolParam(description = "Optional glob filter for file names (e.g. '*.java', '*.{json,yaml}'). "
                    + "Applies to files only — directories are always shown. Default: all files.",
                    required = false)
            String filter,
            @ToolParam(description = "Maximum recursion depth. 1 = immediate children only (default). "
                    + "Use a higher number or 99 for full recursive listing.",
                    required = false)
            Integer maxDepth,
            @ToolParam(description = "Include hidden files and directories (names starting with '.'). Default: false.",
                    required = false)
            Boolean includeHidden) {

        int depth = (maxDepth == null || maxDepth < 1) ? 1 : maxDepth;
        boolean showHidden = Boolean.TRUE.equals(includeHidden);

        Path root = Path.of(path);
        if (!Files.exists(root)) return "Error: path does not exist: " + path;
        if (!Files.isDirectory(root)) return "Error: path is not a directory: " + path;

        PathMatcher matcher = filter != null && !filter.isBlank()
                ? root.getFileSystem().getPathMatcher("glob:" + filter)
                : null;

        List<String[]> rows = new ArrayList<>(); // [indent+name, type, size, modified]
        long[] counts = {0, 0}; // [dirs, files]

        try {
            Files.walkFileTree(root, Set.of(), depth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) return FileVisitResult.CONTINUE;
                    String name = dir.getFileName().toString();
                    if (!showHidden && name.startsWith(".")) return FileVisitResult.SKIP_SUBTREE;
                    int indent = root.relativize(dir).getNameCount() - 1;
                    rows.add(new String[]{
                            "  ".repeat(indent) + "[DIR] " + name,
                            "dir",
                            "-",
                            TS.format(attrs.lastModifiedTime().toInstant())
                    });
                    counts[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (!showHidden && name.startsWith(".")) return FileVisitResult.CONTINUE;
                    int indent = root.relativize(file).getNameCount() - 1;
                    // Directories sitting exactly at maxDepth are delivered to visitFile
                    // (walkFileTree won't descend into them); render them as dirs, not files.
                    if (attrs.isDirectory()) {
                        rows.add(new String[]{
                                "  ".repeat(indent) + "[DIR] " + name,
                                "dir",
                                "-",
                                TS.format(attrs.lastModifiedTime().toInstant())
                        });
                        counts[0]++;
                        return FileVisitResult.CONTINUE;
                    }
                    if (matcher != null && !matcher.matches(Path.of(name))) return FileVisitResult.CONTINUE;
                    rows.add(new String[]{
                            "  ".repeat(indent) + name,
                            "file",
                            humanSize(attrs.size()),
                            TS.format(attrs.lastModifiedTime().toInstant())
                    });
                    counts[1]++;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return "Error reading directory: " + e.getMessage();
        }

        if (rows.isEmpty()) {
            return "Directory is empty (or no files match filter): " + path;
        }

        // Column widths
        int nameW = rows.stream().mapToInt(r -> r[0].length()).max().orElse(4);
        int sizeW = rows.stream().mapToInt(r -> r[2].length()).max().orElse(4);

        StringBuilder sb = new StringBuilder();
        sb.append("Directory: ").append(root.toAbsolutePath()).append("\n");
        if (filter != null && !filter.isBlank()) sb.append("Filter: ").append(filter).append("\n");
        sb.append("Depth: ").append(depth)
          .append(" | Dirs: ").append(counts[0])
          .append(" | Files: ").append(counts[1]).append("\n");
        sb.append("─".repeat(nameW + sizeW + 22)).append("\n");
        sb.append(String.format("%-" + nameW + "s  %6s  %s%n", "NAME", "SIZE", "MODIFIED"));
        sb.append("─".repeat(nameW + sizeW + 22)).append("\n");

        for (String[] row : rows) {
            sb.append(String.format("%-" + nameW + "s  %6s  %s%n", row[0], row[2], row[3]));
        }

        return sb.toString().strip();
    }

    @Tool(name = "file_info",
          description = "Get metadata for a single file or directory: size, type, last modified, "
                  + "permissions (readable/writable/executable), and absolute path.")
    public String fileInfo(
            @ToolParam(description = "Absolute path to the file or directory") String path) {

        Path p = Path.of(path);
        if (!Files.exists(p)) return "Error: path does not exist: " + path;

        try {
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            File f = p.toFile();
            StringBuilder sb = new StringBuilder();
            sb.append("Path:     ").append(p.toAbsolutePath()).append("\n");
            sb.append("Type:     ").append(attrs.isDirectory() ? "directory" : attrs.isSymbolicLink() ? "symlink" : "file").append("\n");
            sb.append("Size:     ").append(humanSize(attrs.size())).append(" (").append(attrs.size()).append(" bytes)\n");
            sb.append("Modified: ").append(TS.format(attrs.lastModifiedTime().toInstant())).append("\n");
            sb.append("Created:  ").append(TS.format(attrs.creationTime().toInstant())).append("\n");
            sb.append("Readable: ").append(f.canRead()).append("\n");
            sb.append("Writable: ").append(f.canWrite()).append("\n");
            sb.append("Executable: ").append(f.canExecute()).append("\n");
            if (attrs.isDirectory()) {
                File[] children = f.listFiles();
                sb.append("Children: ").append(children == null ? "?" : children.length).append("\n");
            }
            return sb.toString().strip();
        } catch (IOException e) {
            return "Error reading file info: " + e.getMessage();
        }
    }

    @Tool(name = "read_text_file",
          description = "Read, open, load, or cat the raw contents of a plain-text file from disk. "
                  + "USE THIS for any text file: .txt, .log, .csv, .md, .json, .xml, .yaml, .ini, source code, etc. "
                  + "Do NOT use pdf_to_text or doc_to_text for these — those are only for binary .pdf / .docx / .doc files. "
                  + "Returns the file text, optionally limited to a character count via maxChars. "
                  + "Use maxChars=0 to read the entire file. Default maxChars=5000 keeps token usage low. "
                  + "Auto-detects UTF-8/UTF-16 BOMs and decodes leniently, so it never fails on odd encodings; "
                  + "pass an explicit charset via the encoding param (e.g. 'ISO-8859-1') if needed.")
    public String readTextFile(
            @ToolParam(description = "Absolute path to the text file (e.g. 'C:/data/notes.txt')")
            String path,
            @ToolParam(description = "Maximum characters to return. Default 5000. Use 0 for the full file.",
                    required = false)
            Integer maxChars,
            @ToolParam(description = "File encoding. Default 'UTF-8'. Use 'ISO-8859-1' for legacy files.",
                    required = false)
            String encoding) {

        Path p = Path.of(path);
        if (!Files.exists(p)) return "Error: file not found: " + path;
        if (Files.isDirectory(p)) return "Error: path is a directory, not a file: " + path;

        Charset charset;
        try {
            charset = encoding != null && !encoding.isBlank()
                    ? Charset.forName(encoding.strip())
                    : StandardCharsets.UTF_8;
        } catch (Exception e) {
            return "Error: unknown encoding '" + encoding + "'";
        }

        int limit = (maxChars == null) ? 5000 : maxChars;

        try {
            byte[] bytes = Files.readAllBytes(p);

            // Detect a byte-order mark; it overrides the requested charset and is stripped
            // from the output so the LLM never sees a stray U+FEFF.
            int bomLen = 0;
            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
                charset = StandardCharsets.UTF_8;
                bomLen = 3;
            } else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
                charset = StandardCharsets.UTF_16LE;
                bomLen = 2;
            } else if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
                charset = StandardCharsets.UTF_16BE;
                bomLen = 2;
            }

            // Lenient decode: invalid bytes become U+FFFD (replacement char) rather than
            // throwing MalformedInputException, so the tool never fails on a non-UTF-8 file.
            CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            String content = decoder.decode(
                    ByteBuffer.wrap(bytes, bomLen, bytes.length - bomLen)).toString();

            boolean truncated = limit > 0 && content.length() > limit;
            if (truncated) content = content.substring(0, limit);

            StringBuilder sb = new StringBuilder();
            sb.append("File: ").append(p.toAbsolutePath()).append("\n");
            sb.append("Size: ").append(humanSize(Files.size(p)));
            if (truncated) sb.append(" (truncated to ").append(limit).append(" chars)");
            sb.append("\n─────\n");
            sb.append(content);
            return sb.toString();
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return new DecimalFormat("0.#").format(bytes / 1024.0) + " KB";
        if (bytes < 1024L * 1024 * 1024) return new DecimalFormat("0.#").format(bytes / (1024.0 * 1024)) + " MB";
        return new DecimalFormat("0.#").format(bytes / (1024.0 * 1024 * 1024)) + " GB";
    }
}
