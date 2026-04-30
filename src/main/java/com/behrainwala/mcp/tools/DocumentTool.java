package com.behrainwala.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentTool {

    private static final Logger log = LoggerFactory.getLogger(DocumentTool.class);
    private static final int MAX_TEXT_LENGTH = 100_000;
    private static final String BASE64_PREFIX = "base64:";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Config records ────────────────────────────────────────────────────────

    private record DocumentConfig(
            String font, int fontSize, String pageSize,
            long marginTop, long marginBottom, long marginLeft, long marginRight,
            double lineSpacing, String accentColor, String pageNumbers,
            String headerText, String footerText,
            String docTitle, String docAuthor, String docSubject) {

        static DocumentConfig defaults() {
            return new DocumentConfig("Calibri", 11, "Letter",
                    1440, 1440, 1440, 1440, 1.15, "2E74B5",
                    "bottom-center", null, null, null, null, null);
        }
    }

    private record BaseStyle(String font, int size, String color,
                              boolean bold, boolean italic, boolean underline, boolean strikethrough) {
        static BaseStyle normal(DocumentConfig c) {
            return new BaseStyle(c.font(), c.fontSize(), null, false, false, false, false);
        }
        // Headings inherit color/size/bold from the registered style definition.
        // Only the font family is set on runs so Word's style editor still works.
        static BaseStyle headingInherit(DocumentConfig c) {
            return new BaseStyle(c.font(), 0, null, false, false, false, false);
        }
        static BaseStyle quote(DocumentConfig c) {
            return new BaseStyle(c.font(), c.fontSize(), "808080", false, true, false, false);
        }
        static BaseStyle footer(DocumentConfig c) {
            return new BaseStyle(c.font(), 9, "808080", false, false, false, false);
        }
    }

    private record ParsedDocument(DocumentConfig config, String markdown) {}

    // ── Tool: doc_to_text ─────────────────────────────────────────────────────

    @Tool(name = "doc_to_text",
          description = "Extract all text and structure from a Word document (.docx or .doc). "
                  + "Returns markdown-formatted output that preserves headings, lists, tables, "
                  + "bold, and italic so the LLM can read the content without spending vision tokens. "
                  + "Accepts a local file path, a URL, or base64-encoded document content (prefixed with 'base64:').")
    public String docToText(
            @ToolParam(description = "Absolute local file path (e.g. 'C:/docs/report.docx'), "
                    + "a URL pointing to a Word document, "
                    + "or base64-encoded content (prefixed with 'base64:').") String source) {

        if (source == null || source.isBlank()) return "Error: source must not be blank.";
        try {
            String format = detectFormat(source);
            byte[] bytes = loadBytes(source);
            StringBuilder header = new StringBuilder();
            header.append("Document Text Extraction\n────────────────────────\n");
            header.append("Source : ").append(displaySource(source)).append("\n");
            header.append("Format : ").append(format).append("\n");
            String body = "DOCX".equals(format) ? extractDocx(bytes, header) : extractDoc(bytes, header);
            header.append("\n");
            if (body.length() > MAX_TEXT_LENGTH)
                body = body.substring(0, MAX_TEXT_LENGTH) + "\n\n... [truncated at " + MAX_TEXT_LENGTH + " chars]";
            return header.append(body).toString();
        } catch (Exception e) {
            log.error("doc_to_text failed for '{}': {}", source, e.getMessage(), e);
            return "Error extracting document text: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── Tool: doc_create ─────────────────────────────────────────────────────

    @Tool(name = "doc_create",
          description = "Create a professional .docx Word document from enhanced markdown content. "
                  + "Optionally starts with a YAML frontmatter block (between --- lines) to configure the document.\n\n"
                  + "FRONTMATTER (all optional, between --- lines at the top):\n"
                  + "  font: Calibri              # font family\n"
                  + "  fontSize: 11               # body font size in points\n"
                  + "  pageSize: Letter           # Letter or A4\n"
                  + "  margins: 1in               # all margins (or: top:0.75in bottom:1in left:1in right:1in)\n"
                  + "  accentColor: \"#2E74B5\"    # heading color (hex or name)\n"
                  + "  lineSpacing: 1.15          # line spacing multiplier\n"
                  + "  pageNumbers: bottom-center # false/none, or bottom/top + -center/-right/-left\n"
                  + "  header: Company Confidential\n"
                  + "  footer: Draft v1.0\n"
                  + "  title: Document Title\n"
                  + "  author: Jane Smith\n\n"
                  + "CONTENT SYNTAX:\n"
                  + "  # H1  ## H2  ### H3  #### H4       — headings with accent color\n"
                  + "  **bold**  *italic*  ***bold-italic***\n"
                  + "  __underline__   ~~strikethrough~~\n"
                  + "  [text]{color:red}  [text]{size:14}  [text]{color:#FF6600 size:12}\n"
                  + "  - bullet  or  1. numbered           — lists\n"
                  + "  | Col1 | Col2 |  and  |---|---|     — table (first row = accented header)\n"
                  + "    Cell prefix {bg:#E7F3FF} sets cell background\n"
                  + "    Cell suffix ^^N spans N columns (e.g. Title^^3 spans 3)\n"
                  + "  <<<                                 — page break\n"
                  + "  [TOC]                               — table of contents field\n"
                  + "  ::: center  (or right/left/justify) — start alignment block\n"
                  + "  :::                                 — end alignment block\n"
                  + "  > blockquote text                   — indented italic quote\n"
                  + "  ![alt](path/url/base64:){width:3in align:center}  — image\n"
                  + "  ---                                 — horizontal rule")
    public String docCreate(
            @ToolParam(description = "Absolute path where the .docx file should be saved, "
                    + "e.g. 'C:/docs/resume.docx'. The parent directory must already exist.") String outputPath,
            @ToolParam(description = "Document content: optional YAML frontmatter followed by "
                    + "enhanced markdown. See tool description for full syntax reference.") String content) {

        if (outputPath == null || outputPath.isBlank()) return "Error: outputPath must not be blank.";
        if (content == null || content.isBlank()) return "Error: content must not be blank.";

        try (XWPFDocument doc = new XWPFDocument()) {
            ParsedDocument parsed = parseFrontmatter(content);
            DocumentConfig cfg = parsed.config();
            setupDocument(doc, cfg);
            applyMarkdown(doc, parsed.markdown(), cfg);
            File outFile = new File(outputPath.strip());
            try (FileOutputStream fos = new FileOutputStream(outFile)) { doc.write(fos); }
            return "Document created successfully.\nPath   : " + outFile.getAbsolutePath()
                    + "\nSize   : " + formatSize(outFile.length()) + "\nFormat : DOCX";
        } catch (Exception e) {
            log.error("doc_create failed for '{}': {}", outputPath, e.getMessage(), e);
            return "Error creating document: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── Tool: doc_edit ────────────────────────────────────────────────────────

    @Tool(name = "doc_edit",
          description = "Edit an existing .docx Word document using a JSON array of operations.\n"
                  + "Operations:\n"
                  + "  {\"type\":\"replace_text\",\"find\":\"old\",\"replace\":\"new\"}\n"
                  + "  {\"type\":\"append_markdown\",\"content\":\"# Section\\n\\nText.\"}\n"
                  + "  {\"type\":\"set_property\",\"property\":\"title|subject|author|description\",\"value\":\"...\"}\n"
                  + "Only .docx files are supported.")
    public String docEdit(
            @ToolParam(description = "Absolute local file path to the .docx file to edit.") String source,
            @ToolParam(description = "JSON array of edit operations.") String operations,
            @ToolParam(description = "Save path. Omit to overwrite source.", required = false) String outputPath) {

        if (source == null || source.isBlank()) return "Error: source must not be blank.";
        if (operations == null || operations.isBlank()) return "Error: operations must not be blank.";

        try {
            JsonNode opsNode = MAPPER.readTree(operations);
            if (!opsNode.isArray()) return "Error: operations must be a JSON array.";
            File sourceFile = new File(source.strip());
            if (!sourceFile.exists()) return "Error: File not found: " + source;

            XWPFDocument doc;
            try (FileInputStream fis = new FileInputStream(sourceFile)) { doc = new XWPFDocument(fis); }

            int replaceCount = 0, appendCount = 0, propCount = 0;
            List<String> warnings = new ArrayList<>();

            for (JsonNode op : opsNode) {
                String type = op.path("type").asText("");
                try {
                    switch (type) {
                        case "replace_text" -> {
                            String find = op.path("find").asText();
                            String repl = op.path("replace").asText();
                            if (find.isEmpty()) { warnings.add("replace_text: 'find' is required"); break; }
                            replaceCount += replaceInDocument(doc, find, repl);
                        }
                        case "append_markdown" -> {
                            String md = op.path("content").asText();
                            if (md.isEmpty()) { warnings.add("append_markdown: 'content' is required"); break; }
                            applyMarkdown(doc, md, DocumentConfig.defaults());
                            appendCount++;
                        }
                        case "set_property" -> {
                            setDocProperty(doc, op.path("property").asText(), op.path("value").asText());
                            propCount++;
                        }
                        default -> warnings.add("Unknown operation type: '" + type + "'");
                    }
                } catch (Exception e) { warnings.add("Operation '" + type + "' failed: " + e.getMessage()); }
            }

            String savePath = (outputPath != null && !outputPath.isBlank()) ? outputPath.strip() : source.strip();
            File outFile = new File(savePath);
            try (FileOutputStream fos = new FileOutputStream(outFile)) { doc.write(fos); }
            doc.close();

            StringBuilder result = new StringBuilder("Document edited successfully.\n");
            result.append("Saved to : ").append(outFile.getAbsolutePath()).append("\n");
            result.append("Size     : ").append(formatSize(outFile.length())).append("\n");
            if (replaceCount > 0) result.append("Replaced : ").append(replaceCount).append(" text occurrence(s)\n");
            if (appendCount > 0) result.append("Appended : ").append(appendCount).append(" content block(s)\n");
            if (propCount > 0)   result.append("Updated  : ").append(propCount).append(" metadata property/ies\n");
            if (!warnings.isEmpty()) { result.append("\nWarnings:\n"); warnings.forEach(w -> result.append("  • ").append(w).append("\n")); }
            return result.toString().strip();

        } catch (Exception e) {
            log.error("doc_edit failed for '{}': {}", source, e.getMessage(), e);
            return "Error editing document: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ── Frontmatter parsing ───────────────────────────────────────────────────

    private ParsedDocument parseFrontmatter(String content) {
        String norm = content.replace("\r\n", "\n").replace("\r", "\n");
        if (!norm.startsWith("---")) return new ParsedDocument(DocumentConfig.defaults(), norm);
        int end = norm.indexOf("\n---", 3);
        if (end < 0) return new ParsedDocument(DocumentConfig.defaults(), norm);
        String yaml = norm.substring(3, end).strip();
        String markdown = norm.substring(end + 4).strip();

        Map<String, String> p = new LinkedHashMap<>();
        for (String line : yaml.split("\n")) {
            int c = line.indexOf(':');
            if (c < 0) continue;
            String k = line.substring(0, c).strip().toLowerCase();
            String v = line.substring(c + 1).strip();
            if (v.length() >= 2 && ((v.charAt(0) == '"' && v.charAt(v.length()-1) == '"')
                    || (v.charAt(0) == '\'' && v.charAt(v.length()-1) == '\'')))
                v = v.substring(1, v.length() - 1);
            p.put(k, v);
        }

        String font        = p.getOrDefault("font", "Calibri");
        int    fontSize    = parseInt(p.getOrDefault("fontsize", "11"), 11);
        String pageSize    = p.getOrDefault("pagesize", "Letter");
        double lineSpacing = parseDouble(p.getOrDefault("linespacing", "1.15"), 1.15);
        String accent      = resolveColor(p.getOrDefault("accentcolor", "#2E74B5"));
        String pageNums    = p.getOrDefault("pagenumbers", "bottom-center");
        if ("false".equalsIgnoreCase(pageNums) || "none".equalsIgnoreCase(pageNums)) pageNums = "none";
        String headerText  = emptyToNull(p.get("header"));
        String footerText  = emptyToNull(p.get("footer"));
        String title       = emptyToNull(p.get("title"));
        String author      = emptyToNull(p.get("author"));
        String subject     = emptyToNull(p.get("subject"));
        long[] margins     = parseMargins(p.getOrDefault("margins", "1in"));

        return new ParsedDocument(
                new DocumentConfig(font, fontSize, pageSize,
                        margins[0], margins[1], margins[2], margins[3],
                        lineSpacing, accent, pageNums, headerText, footerText, title, author, subject),
                markdown);
    }

    // ── Document setup ────────────────────────────────────────────────────────

    private void setupDocument(XWPFDocument doc, DocumentConfig cfg) {
        setupStyles(doc, cfg);   // must run first — TOC depends on style outlineLvl
        // Page geometry
        CTBody body = doc.getDocument().getBody();
        CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        CTPageSz pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        if ("A4".equalsIgnoreCase(cfg.pageSize())) { pgSz.setW(BigInteger.valueOf(11906)); pgSz.setH(BigInteger.valueOf(16838)); }
        else                                        { pgSz.setW(BigInteger.valueOf(12240)); pgSz.setH(BigInteger.valueOf(15840)); }
        CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pgMar.setTop(BigInteger.valueOf(cfg.marginTop())); pgMar.setBottom(BigInteger.valueOf(cfg.marginBottom()));
        pgMar.setLeft(BigInteger.valueOf(cfg.marginLeft())); pgMar.setRight(BigInteger.valueOf(cfg.marginRight()));

        // Metadata
        if (cfg.docTitle()   != null) doc.getProperties().getCoreProperties().setTitle(cfg.docTitle());
        if (cfg.docAuthor()  != null) doc.getProperties().getCoreProperties().setCreator(cfg.docAuthor());
        if (cfg.docSubject() != null) doc.getProperties().getCoreProperties().setSubjectProperty(cfg.docSubject());

        // Header / footer / page numbers
        boolean hasPageNums = !"none".equals(cfg.pageNumbers());
        boolean hasHeader   = cfg.headerText() != null;
        boolean hasFooter   = cfg.footerText() != null;
        if (!hasPageNums && !hasHeader && !hasFooter) return;

        if (hasHeader) {
            XWPFHeader hdr = doc.createHeader(HeaderFooterType.DEFAULT);
            XWPFParagraph p = firstParagraph(hdr);
            p.setAlignment(ParagraphAlignment.CENTER);
            applyRun(p.createRun(), cfg.headerText(), BaseStyle.footer(cfg));
        }

        if (hasFooter || hasPageNums) {
            XWPFFooter ftr = doc.createFooter(HeaderFooterType.DEFAULT);
            if (hasFooter) {
                XWPFParagraph p = firstParagraph(ftr);
                p.setAlignment(ParagraphAlignment.CENTER);
                applyRun(p.createRun(), cfg.footerText(), BaseStyle.footer(cfg));
            }
            if (hasPageNums) {
                String pos = cfg.pageNumbers();
                ParagraphAlignment align = pos.endsWith("right") ? ParagraphAlignment.RIGHT
                        : pos.endsWith("left") ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER;
                XWPFParagraph p = ftr.createParagraph();
                p.setAlignment(align);
                addPageNumField(p, cfg);
            }
        }
    }

    /**
     * Registers Normal, Heading1–Heading4, ListParagraph, and Quote styles with
     * explicit outlineLvl on headings. Without outlineLvl in the style definition
     * the TOC field has nothing to index when Word refreshes it (Ctrl+A → F9).
     */
    private void setupStyles(XWPFDocument doc, DocumentConfig cfg) {
        XWPFStyles styles = doc.createStyles();

        // ── Normal ────────────────────────────────────────────────────────────
        if (!styles.styleExist("Normal")) {
            CTStyle s = CTStyle.Factory.newInstance();
            s.setType(STStyleType.PARAGRAPH);
            s.setStyleId("Normal");
            s.setDefault(true);
            s.addNewName().setVal("Normal");
            CTRPr rpr = s.addNewRPr();
            CTFonts f = rpr.addNewRFonts(); f.setAscii(cfg.font()); f.setHAnsi(cfg.font());
            CTHpsMeasure sz = rpr.addNewSz(); sz.setVal(BigInteger.valueOf(cfg.fontSize() * 2));
            rpr.addNewSzCs().setVal(BigInteger.valueOf(cfg.fontSize() * 2));
            styles.addStyle(new XWPFStyle(s, styles));
        }

        // ── Headings 1–4 ─────────────────────────────────────────────────────
        // {fontSize pts, bold, italic, spacingBefore twips, spacingAfter twips}
        int[][] hCfg = {
            {20, 1, 0, 240, 120},
            {16, 1, 0, 200,  80},
            {14, 1, 0, 160,  60},
            {12, 0, 1, 120,  40},
        };
        for (int i = 0; i < hCfg.length; i++) {
            int level = i + 1;
            String id = "Heading" + level;
            if (styles.styleExist(id)) {
                // Style exists but may lack outlineLvl — patch it
                XWPFStyle existing = styles.getStyle(id);
                if (existing != null) {
                    CTStyle cs = existing.getCTStyle();
                    CTPPrGeneral ppr = cs.isSetPPr() ? cs.getPPr() : cs.addNewPPr();
                    if (!ppr.isSetOutlineLvl())
                        ppr.addNewOutlineLvl().setVal(BigInteger.valueOf(i));
                }
                continue;
            }
            CTStyle s = CTStyle.Factory.newInstance();
            s.setType(STStyleType.PARAGRAPH);
            s.setStyleId(id);
            s.addNewName().setVal("Heading " + level);
            s.addNewBasedOn().setVal("Normal");
            s.addNewNext().setVal("Normal");

            // outlineLvl is what the TOC field indexes (0 = H1, 1 = H2, …)
            CTPPrGeneral ppr = s.addNewPPr();
            ppr.addNewOutlineLvl().setVal(BigInteger.valueOf(i));
            CTSpacing sp = ppr.addNewSpacing();
            sp.setBefore(BigInteger.valueOf(hCfg[i][3]));
            sp.setAfter(BigInteger.valueOf(hCfg[i][4]));
            // keep paragraphs in the same page if possible for H1/H2
            if (level <= 2) ppr.addNewKeepNext();

            CTRPr rpr = s.addNewRPr();
            CTFonts f = rpr.addNewRFonts(); f.setAscii(cfg.font()); f.setHAnsi(cfg.font());
            CTHpsMeasure sz = rpr.addNewSz(); sz.setVal(BigInteger.valueOf(hCfg[i][0] * 2));
            rpr.addNewSzCs().setVal(BigInteger.valueOf(hCfg[i][0] * 2));
            rpr.addNewColor().setVal(cfg.accentColor());
            if (hCfg[i][1] == 1) { rpr.addNewB(); rpr.addNewBCs(); }
            if (hCfg[i][2] == 1) rpr.addNewI();

            styles.addStyle(new XWPFStyle(s, styles));
        }

        // ── ListParagraph ─────────────────────────────────────────────────────
        if (!styles.styleExist("ListParagraph")) {
            CTStyle s = CTStyle.Factory.newInstance();
            s.setType(STStyleType.PARAGRAPH);
            s.setStyleId("ListParagraph");
            s.addNewName().setVal("List Paragraph");
            s.addNewBasedOn().setVal("Normal");
            CTPPrGeneral ppr = s.addNewPPr();
            CTInd ind = ppr.addNewInd(); ind.setLeft(BigInteger.valueOf(720));
            styles.addStyle(new XWPFStyle(s, styles));
        }

        // ── Quote (block quote) ───────────────────────────────────────────────
        if (!styles.styleExist("Quote")) {
            CTStyle s = CTStyle.Factory.newInstance();
            s.setType(STStyleType.PARAGRAPH);
            s.setStyleId("Quote");
            s.addNewName().setVal("Quote");
            s.addNewBasedOn().setVal("Normal");
            CTRPr rpr = s.addNewRPr();
            rpr.addNewI(); rpr.addNewICs();
            rpr.addNewColor().setVal("808080");
            styles.addStyle(new XWPFStyle(s, styles));
        }
    }

    private XWPFParagraph firstParagraph(XWPFHeaderFooter hf) {
        List<XWPFParagraph> list = hf.getParagraphs();
        return list.isEmpty() ? hf.createParagraph() : list.get(0);
    }

    private void addPageNumField(XWPFParagraph para, DocumentConfig cfg) {
        applyRun(para.createRun(), "Page ", BaseStyle.footer(cfg));
        addField(para, " PAGE ");
        applyRun(para.createRun(), " of ", BaseStyle.footer(cfg));
        addField(para, " NUMPAGES ");
    }

    private void addField(XWPFParagraph para, String instrText) {
        XWPFRun r = para.createRun(); r.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        r = para.createRun(); r.getCTR().addNewInstrText().setStringValue(instrText);
        r = para.createRun(); r.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
    }

    // ── Markdown state machine ────────────────────────────────────────────────

    private void applyMarkdown(XWPFDocument doc, String markdown, DocumentConfig cfg) {
        String[] lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);

        BigInteger bulletNumId = null, numberedNumId = null;
        boolean lastWasList = false;
        ParagraphAlignment currentAlign = null;
        List<String> tableBuffer = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // ─── alignment block open
            if (line.startsWith("::: ")) {
                currentAlign = switch (line.substring(4).strip().toLowerCase()) {
                    case "center" -> ParagraphAlignment.CENTER;
                    case "right"  -> ParagraphAlignment.RIGHT;
                    case "justify" -> ParagraphAlignment.BOTH;
                    default       -> ParagraphAlignment.LEFT;
                };
                continue;
            }
            if (line.strip().equals(":::")) { currentAlign = null; continue; }

            // ─── table accumulation
            if (line.startsWith("|")) {
                tableBuffer.add(line);
                if (i + 1 >= lines.length || !lines[i + 1].startsWith("|")) {
                    renderTable(doc, tableBuffer, cfg, currentAlign);
                    tableBuffer.clear(); lastWasList = false;
                }
                continue;
            } else if (!tableBuffer.isEmpty()) {
                renderTable(doc, tableBuffer, cfg, currentAlign);
                tableBuffer.clear();
            }

            // ─── page break
            if (line.strip().equals("<<<")) {
                XWPFParagraph pb = doc.createParagraph(); pb.setPageBreak(true);
                lastWasList = false; continue;
            }

            // ─── TOC
            if (line.strip().equalsIgnoreCase("[toc]")) {
                insertTOC(doc, cfg); lastWasList = false; continue;
            }

            // ─── headings
            if (line.startsWith("#### ")) { bulletNumId = numberedNumId = null; lastWasList = false; addHeading(doc, line.substring(5).strip(), 4, cfg, currentAlign); continue; }
            if (line.startsWith("### "))  { bulletNumId = numberedNumId = null; lastWasList = false; addHeading(doc, line.substring(4).strip(), 3, cfg, currentAlign); continue; }
            if (line.startsWith("## "))   { bulletNumId = numberedNumId = null; lastWasList = false; addHeading(doc, line.substring(3).strip(), 2, cfg, currentAlign); continue; }
            if (line.startsWith("# "))    { bulletNumId = numberedNumId = null; lastWasList = false; addHeading(doc, line.substring(2).strip(), 1, cfg, currentAlign); continue; }

            // ─── horizontal rule
            if (line.matches("^[-*_]{3,}\\s*$")) {
                XWPFParagraph hr = doc.createParagraph();
                hr.setBorderBottom(Borders.SINGLE);
                paraSpacing(hr, cfg, 60, 60);
                lastWasList = false; continue;
            }

            // ─── block quote
            if (line.startsWith("> ")) {
                if (lastWasList) { numberedNumId = null; lastWasList = false; }
                XWPFParagraph qp = doc.createParagraph();
                if (currentAlign != null) qp.setAlignment(currentAlign);
                CTPPr ppr = getPpr(qp);
                CTInd ind = ppr.isSetInd() ? ppr.getInd() : ppr.addNewInd();
                ind.setLeft(BigInteger.valueOf(720));
                CTPBdr borders = ppr.isSetPBdr() ? ppr.getPBdr() : ppr.addNewPBdr();
                CTBorder left = borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft();
                left.setVal(STBorder.SINGLE); left.setSz(BigInteger.valueOf(12)); left.setColor("AAAAAA"); left.setSpace(BigInteger.valueOf(8));
                paraSpacing(qp, cfg, 0, 60);
                addRuns(qp, line.substring(2), BaseStyle.quote(cfg));
                continue;
            }

            // ─── image: ![alt](src) or ![alt](src){attrs}
            Matcher imgM = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)(?:\\{([^}]*)\\})?").matcher(line.strip());
            if (imgM.matches()) {
                if (lastWasList) { numberedNumId = null; lastWasList = false; }
                insertImage(doc, imgM.group(2).strip(), imgM.group(1), imgM.group(3), cfg, currentAlign);
                continue;
            }

            // ─── bullet list
            Matcher bm = Pattern.compile("^[\\-*+]\\s+(.+)").matcher(line);
            if (bm.matches()) {
                if (bulletNumId == null) bulletNumId = createBulletList(doc);
                addListItem(doc, bulletNumId, bm.group(1), cfg); lastWasList = true; continue;
            }

            // ─── numbered list
            Matcher nm = Pattern.compile("^\\d+\\.\\s+(.+)").matcher(line);
            if (nm.matches()) {
                if (numberedNumId == null) numberedNumId = createNumberedList(doc);
                addListItem(doc, numberedNumId, nm.group(1), cfg); lastWasList = true; continue;
            }

            // ─── leave list context
            if (lastWasList && !line.isBlank()) { numberedNumId = null; lastWasList = false; }

            // ─── blank line
            if (line.isBlank()) { doc.createParagraph(); continue; }

            // ─── normal paragraph
            XWPFParagraph np = doc.createParagraph();
            if (currentAlign != null) np.setAlignment(currentAlign);
            paraSpacing(np, cfg, 0, 120);
            addRuns(np, line, BaseStyle.normal(cfg));
        }
        if (!tableBuffer.isEmpty()) renderTable(doc, tableBuffer, cfg, currentAlign);
    }

    // ── Block builders ────────────────────────────────────────────────────────

    private void addHeading(XWPFDocument doc, String text, int level, DocumentConfig cfg, ParagraphAlignment align) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading" + level);
        if (align != null) p.setAlignment(align);
        // Spacing is owned by the style definition — only set line spacing for body rhythm,
        // not before/after (that would override the style and break Word's style editor).
        CTPPr ppr = getPpr(p);
        CTSpacing sp = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        sp.setLine(BigInteger.valueOf((long)(240 * cfg.lineSpacing())));
        sp.setLineRule(STLineSpacingRule.AUTO);
        // Runs inherit bold/italic/color/size from the Heading style; only font family is explicit.
        addRuns(p, text, BaseStyle.headingInherit(cfg));
    }

    private void addListItem(XWPFDocument doc, BigInteger numId, String text, DocumentConfig cfg) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("ListParagraph");
        p.setNumID(numId);
        CTPPr ppr = getPpr(p);
        CTNumPr numPr = ppr.isSetNumPr() ? ppr.getNumPr() : ppr.addNewNumPr();
        (numPr.isSetIlvl() ? numPr.getIlvl() : numPr.addNewIlvl()).setVal(BigInteger.ZERO);
        paraSpacing(p, cfg, 0, 40);
        addRuns(p, text, BaseStyle.normal(cfg));
    }

    private void insertTOC(XWPFDocument doc, DocumentConfig cfg) {
        XWPFParagraph hp = doc.createParagraph();
        hp.setStyle("Heading1");
        paraSpacing(hp, cfg, 240, 120);
        XWPFRun hr = hp.createRun();
        hr.setText("Table of Contents");
        hr.setBold(true); hr.setFontSize(16); hr.setColor(cfg.accentColor()); hr.setFontFamily(cfg.font());

        XWPFParagraph fp = doc.createParagraph();
        CTP ctp = fp.getCTP();
        CTR br = ctp.addNewR();
        br.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        CTR ir = ctp.addNewR(); ir.addNewInstrText().setStringValue("TOC \\o \"1-3\" \\h \\z \\u");
        CTR sr = ctp.addNewR(); sr.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        CTR cr = ctp.addNewR(); cr.addNewT().setStringValue("[Right-click → Update Field to generate contents]");
        CTR er = ctp.addNewR(); er.addNewFldChar().setFldCharType(STFldCharType.END);
        doc.createParagraph();
    }

    private void insertImage(XWPFDocument doc, String src, String alt, String attrsRaw,
                              DocumentConfig cfg, ParagraphAlignment align) {
        try {
            byte[] bytes  = loadBytes(src);
            int picType   = detectPictureType(src);
            int widthEmu  = (int)(5.5 * 914400);
            ParagraphAlignment imgAlign = align != null ? align : ParagraphAlignment.CENTER;

            if (attrsRaw != null) {
                for (String attr : attrsRaw.split("\\s+")) {
                    int c = attr.indexOf(':'); if (c < 0) continue;
                    String k = attr.substring(0, c).toLowerCase(), v = attr.substring(c + 1);
                    if ("width".equals(k)) widthEmu = parseEmu(v);
                    else if ("align".equals(k)) imgAlign = switch (v.toLowerCase()) {
                        case "right" -> ParagraphAlignment.RIGHT;
                        case "left"  -> ParagraphAlignment.LEFT;
                        default      -> ParagraphAlignment.CENTER;
                    };
                }
            }
            int heightEmu = widthEmu;
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi != null && bi.getWidth() > 0) heightEmu = (int)((long)widthEmu * bi.getHeight() / bi.getWidth());
            } catch (Exception ignored) {}

            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(imgAlign);
            paraSpacing(p, cfg, 60, 60);
            p.createRun().addPicture(new ByteArrayInputStream(bytes), picType,
                    alt != null && !alt.isBlank() ? alt : "image", widthEmu, heightEmu);
        } catch (Exception e) {
            log.warn("Image insertion failed for '{}': {}", src, e.getMessage());
            XWPFParagraph ep = doc.createParagraph();
            XWPFRun er = ep.createRun();
            er.setText("[Image not loaded: " + src + " — " + e.getMessage() + "]");
            er.setItalic(true); er.setColor("FF0000");
        }
    }

    // ── Table rendering ───────────────────────────────────────────────────────

    private static final Pattern CELL_BG  = Pattern.compile("\\{bg:([^}]+)}\\s*(.*)");
    private static final Pattern CELL_COL = Pattern.compile("(.*)\\^\\^(\\d+)$");

    private void renderTable(XWPFDocument doc, List<String> lines, DocumentConfig cfg, ParagraphAlignment align) {
        // Parse: skip separator rows, build data rows
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            String[] cells = splitTableCells(line);
            if (cells.length > 0 && Arrays.stream(cells).allMatch(c -> c.strip().matches("[-:| ]+"))) continue;
            rows.add(cells);
        }
        if (rows.isEmpty()) return;

        int cols = rows.stream().mapToInt(r -> r.length).max().orElse(1);
        XWPFTable table = doc.createTable(rows.size(), cols);

        // Table-wide styling
        CTTblPr tpr = table.getCTTbl().getTblPr();
        if (tpr == null) tpr = table.getCTTbl().addNewTblPr();
        CTTblWidth tw = tpr.isSetTblW() ? tpr.getTblW() : tpr.addNewTblW();
        tw.setType(STTblWidth.PCT); tw.setW(BigInteger.valueOf(5000));
        CTTblBorders tb = tpr.isSetTblBorders() ? tpr.getTblBorders() : tpr.addNewTblBorders();
        setTblBorder(tb.addNewTop(),     STBorder.SINGLE, 4, "A0A0A0");
        setTblBorder(tb.addNewBottom(),  STBorder.SINGLE, 4, "A0A0A0");
        setTblBorder(tb.addNewLeft(),    STBorder.SINGLE, 4, "A0A0A0");
        setTblBorder(tb.addNewRight(),   STBorder.SINGLE, 4, "A0A0A0");
        setTblBorder(tb.addNewInsideH(), STBorder.SINGLE, 4, "D0D0D0");
        setTblBorder(tb.addNewInsideV(), STBorder.SINGLE, 4, "D0D0D0");

        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r);
            String[] cells = rows.get(r);
            boolean isHeader = (r == 0);

            int colIdx = 0;    // logical column position (advances by colspan)
            int ctRowIdx = 0;  // physical index in the live CTRow array (not the stale Java list)

            while (colIdx < cols) {
                String raw = colIdx < cells.length ? cells[colIdx].strip() : "";

                // Parse colspan
                int colspan = 1;
                Matcher cm = CELL_COL.matcher(raw);
                if (cm.matches()) { raw = cm.group(1).strip(); colspan = Math.max(1, parseInt(cm.group(2), 1)); }

                // Parse cell background
                String bgColor = null;
                Matcher bm = CELL_BG.matcher(raw);
                if (bm.matches()) { bgColor = resolveColor(bm.group(1)); raw = bm.group(2).strip(); }

                // Access cell via CTRow directly — avoids using XWPFTableRow's stale Java list
                // (the Java list is not updated when removeTc() is called, causing XmlValueDisconnectedException)
                CTTc ctTc;
                if (ctRowIdx < row.getCtRow().sizeOfTcArray()) {
                    ctTc = row.getCtRow().getTcArray(ctRowIdx);
                } else {
                    ctTc = row.getCtRow().addNewTc();
                    ctTc.addNewP(); // CTTc requires at least one paragraph
                }
                XWPFTableCell cell = new XWPFTableCell(ctTc, row, doc);
                ctRowIdx++;

                // Apply colspan via gridSpan, then remove the extra pre-created cells
                if (colspan > 1) {
                    CTTcPr tcp = getTcPr(cell);
                    (tcp.isSetGridSpan() ? tcp.getGridSpan() : tcp.addNewGridSpan()).setVal(BigInteger.valueOf(colspan));
                    for (int s = 1; s < colspan; s++) {
                        // ctRowIdx points to the next cell to remove; don't advance it
                        if (row.getCtRow().sizeOfTcArray() > ctRowIdx)
                            row.getCtRow().removeTc(ctRowIdx);
                    }
                }

                // Header row gets accent background + white text; otherwise custom or default
                String effectiveBg = isHeader ? cfg.accentColor() : bgColor;
                if (effectiveBg != null) {
                    CTTcPr tcp = getTcPr(cell);
                    CTShd shd = tcp.isSetShd() ? tcp.getShd() : tcp.addNewShd();
                    shd.setVal(STShd.CLEAR); shd.setColor("auto"); shd.setFill(effectiveBg);
                }

                // Cell padding
                CTTcPr tcp = getTcPr(cell);
                CTTcMar mar = tcp.isSetTcMar() ? tcp.getTcMar() : tcp.addNewTcMar();
                setTcW(mar.isSetTop() ? mar.getTop() : mar.addNewTop(), 60);
                setTcW(mar.isSetBottom() ? mar.getBottom() : mar.addNewBottom(), 60);
                setTcW(mar.isSetLeft() ? mar.getLeft() : mar.addNewLeft(), 120);
                setTcW(mar.isSetRight() ? mar.getRight() : mar.addNewRight(), 120);

                // Cell paragraph + content
                XWPFParagraph cp = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
                while (!cp.getRuns().isEmpty()) cp.removeRun(0);
                if (align != null) cp.setAlignment(align);

                String textColor = isHeader ? "FFFFFF" : null;
                BaseStyle cs = new BaseStyle(cfg.font(), cfg.fontSize() - 1, textColor, isHeader, false, false, false);
                addRuns(cp, raw, cs);

                colIdx += colspan;
            }
        }
        doc.createParagraph(); // DOCX requires a paragraph after a table
    }

    private CTTcPr getTcPr(XWPFTableCell cell) {
        CTTc ctTc = cell.getCTTc();
        return ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
    }

    private void setTblBorder(CTBorder b, STBorder.Enum type, int sz, String color) {
        b.setVal(type); b.setSz(BigInteger.valueOf(sz)); b.setSpace(BigInteger.ZERO); b.setColor(color);
    }

    private void setTcW(CTTblWidth w, int dxa) {
        w.setType(STTblWidth.DXA); w.setW(BigInteger.valueOf(dxa));
    }

    private String[] splitTableCells(String line) {
        String[] parts = line.split("\\|");
        int s = (parts.length > 0 && parts[0].isBlank()) ? 1 : 0;
        int e = (parts.length > s && parts[parts.length - 1].isBlank()) ? parts.length - 1 : parts.length;
        return Arrays.copyOfRange(parts, s, e);
    }

    // ── Inline formatter ──────────────────────────────────────────────────────

    /**
     * State-machine inline parser. Handles: ***bold-italic***, **bold**, *italic*,
     * __underline__, ~~strikethrough~~, [text]{color:X size:Y} and plain text.
     */
    private void addRuns(XWPFParagraph para, String text, BaseStyle base) {
        if (text == null || text.isEmpty()) { para.createRun(); return; }
        int i = 0;
        StringBuilder plain = new StringBuilder();
        while (i < text.length()) {
            // bold-italic
            if (text.startsWith("***", i)) {
                int end = text.indexOf("***", i + 3);
                if (end >= 0) { flush(para, plain, base); emit(para, text.substring(i+3, end), base, true, true, false, false, null, -1); i = end + 3; continue; }
            }
            // bold
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end >= 0) { flush(para, plain, base); emit(para, text.substring(i+2, end), base, true, false, false, false, null, -1); i = end + 2; continue; }
            }
            // underline
            if (text.startsWith("__", i)) {
                int end = text.indexOf("__", i + 2);
                if (end >= 0) { flush(para, plain, base); emit(para, text.substring(i+2, end), base, false, false, true, false, null, -1); i = end + 2; continue; }
            }
            // strikethrough
            if (text.startsWith("~~", i)) {
                int end = text.indexOf("~~", i + 2);
                if (end >= 0) { flush(para, plain, base); emit(para, text.substring(i+2, end), base, false, false, false, true, null, -1); i = end + 2; continue; }
            }
            // italic
            if (text.charAt(i) == '*') {
                int end = text.indexOf('*', i + 1);
                if (end >= 0) { flush(para, plain, base); emit(para, text.substring(i+1, end), base, false, true, false, false, null, -1); i = end + 1; continue; }
            }
            // [text]{attrs}
            if (text.charAt(i) == '[') {
                int cb = text.indexOf(']', i + 1);
                if (cb >= 0 && cb + 1 < text.length() && text.charAt(cb + 1) == '{') {
                    int ca = text.indexOf('}', cb + 2);
                    if (ca >= 0) {
                        String styledText = text.substring(i + 1, cb);
                        Map<String, String> props = parseAttrs(text.substring(cb + 2, ca));
                        String color = props.containsKey("color") ? resolveColor(props.get("color")) : null;
                        int size = parseInt(props.getOrDefault("size", "-1"), -1);
                        flush(para, plain, base);
                        emit(para, styledText, base,
                                "true".equals(props.get("bold")) || base.bold(),
                                "true".equals(props.get("italic")) || base.italic(),
                                false, false, color, size);
                        i = ca + 1; continue;
                    }
                }
            }
            plain.append(text.charAt(i++));
        }
        flush(para, plain, base);
    }

    private void flush(XWPFParagraph para, StringBuilder buf, BaseStyle base) {
        if (buf.isEmpty()) return;
        emit(para, buf.toString(), base, false, false, false, false, null, -1);
        buf.setLength(0);
    }

    private void emit(XWPFParagraph para, String text, BaseStyle base,
                      boolean bold, boolean italic, boolean underline, boolean strikethrough,
                      String colorOverride, int sizeOverride) {
        if (text == null || text.isEmpty()) return;
        XWPFRun r = para.createRun();
        applyRun(r, text, base);
        if (bold || base.bold())             r.setBold(true);
        if (italic || base.italic())         r.setItalic(true);
        if (underline || base.underline())   r.setUnderline(UnderlinePatterns.SINGLE);
        if (strikethrough || base.strikethrough()) r.setStrikeThrough(true);
        if (colorOverride != null)           r.setColor(colorOverride);
        if (sizeOverride > 0)                r.setFontSize(sizeOverride);
    }

    private void applyRun(XWPFRun r, String text, BaseStyle base) {
        r.setText(text);
        if (base.font() != null)  r.setFontFamily(base.font());
        if (base.size() > 0)      r.setFontSize(base.size());  // 0 = inherit from style
        if (base.bold())          r.setBold(true);
        if (base.italic())        r.setItalic(true);
        if (base.underline())     r.setUnderline(UnderlinePatterns.SINGLE);
        if (base.strikethrough()) r.setStrikeThrough(true);
        if (base.color() != null) r.setColor(base.color());
    }

    // ── List numbering ────────────────────────────────────────────────────────

    private BigInteger createBulletList(XWPFDocument doc) {
        XWPFNumbering n = doc.createNumbering();
        CTAbstractNum abs = CTAbstractNum.Factory.newInstance();
        CTLvl lvl = abs.addNewLvl(); lvl.setIlvl(BigInteger.ZERO);
        lvl.addNewNumFmt().setVal(STNumberFormat.BULLET); lvl.addNewLvlText().setVal("•");
        lvl.addNewStart().setVal(BigInteger.ONE);
        CTInd ind = lvl.addNewPPr().addNewInd(); ind.setLeft(BigInteger.valueOf(720)); ind.setHanging(BigInteger.valueOf(360));
        return n.addNum(n.addAbstractNum(new XWPFAbstractNum(abs)));
    }

    private BigInteger createNumberedList(XWPFDocument doc) {
        XWPFNumbering n = doc.createNumbering();
        CTAbstractNum abs = CTAbstractNum.Factory.newInstance();
        CTLvl lvl = abs.addNewLvl(); lvl.setIlvl(BigInteger.ZERO);
        lvl.addNewNumFmt().setVal(STNumberFormat.DECIMAL); lvl.addNewLvlText().setVal("%1.");
        lvl.addNewStart().setVal(BigInteger.ONE);
        CTInd ind = lvl.addNewPPr().addNewInd(); ind.setLeft(BigInteger.valueOf(720)); ind.setHanging(BigInteger.valueOf(360));
        return n.addNum(n.addAbstractNum(new XWPFAbstractNum(abs)));
    }

    // ── Edit helpers ──────────────────────────────────────────────────────────

    private int replaceInDocument(XWPFDocument doc, String find, String replace) {
        int count = 0;
        for (XWPFParagraph p : doc.getParagraphs()) count += replaceInParagraph(p, find, replace);
        for (XWPFTable t : doc.getTables())
            for (XWPFTableRow row : t.getRows())
                for (XWPFTableCell cell : row.getTableCells())
                    for (XWPFParagraph p : cell.getParagraphs()) count += replaceInParagraph(p, find, replace);
        return count;
    }

    private int replaceInParagraph(XWPFParagraph para, String find, String replace) {
        int count = 0;
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t != null && t.contains(find)) { run.setText(t.replace(find, replace), 0); count++; }
        }
        if (count > 0) return count;
        String full = para.getText();
        if (!full.contains(find)) return 0;
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) return 0;
        runs.get(0).setText(full.replace(find, replace), 0);
        for (int i = runs.size() - 1; i > 0; i--) para.removeRun(i);
        return 1;
    }

    private void setDocProperty(XWPFDocument doc, String property, String value) {
        var core = doc.getProperties().getCoreProperties();
        switch (property.toLowerCase()) {
            case "title"             -> core.setTitle(value);
            case "subject"           -> core.setSubjectProperty(value);
            case "author", "creator" -> core.setCreator(value);
            case "description"       -> core.setDescription(value);
            default -> throw new IllegalArgumentException("Unknown property: " + property);
        }
    }

    // ── Extraction (doc_to_text) ──────────────────────────────────────────────

    private String extractDocx(byte[] bytes, StringBuilder meta) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String title = doc.getProperties().getCoreProperties().getTitle();
            String author = doc.getProperties().getCoreProperties().getCreator();
            if (title != null && !title.isBlank())  meta.append("Title  : ").append(title).append("\n");
            if (author != null && !author.isBlank()) meta.append("Author : ").append(author).append("\n");
            StringBuilder body = new StringBuilder();
            for (IBodyElement el : doc.getBodyElements()) {
                if (el instanceof XWPFParagraph p) appendExtractPara(body, p);
                else if (el instanceof XWPFTable t) appendExtractTable(body, t);
            }
            return body.toString().strip();
        }
    }

    private void appendExtractPara(StringBuilder sb, XWPFParagraph para) {
        String text = extractParaRuns(para);
        if (text.isBlank()) { sb.append("\n"); return; }
        String style = para.getStyle();
        String lc = style != null ? style.toLowerCase().replace(" ", "") : "";
        if      (lc.startsWith("heading1") || "1".equals(lc)) sb.append("# ").append(text.strip()).append("\n\n");
        else if (lc.startsWith("heading2") || "2".equals(lc)) sb.append("## ").append(text.strip()).append("\n\n");
        else if (lc.startsWith("heading3") || "3".equals(lc)) sb.append("### ").append(text.strip()).append("\n\n");
        else if (lc.startsWith("heading4") || "4".equals(lc)) sb.append("#### ").append(text.strip()).append("\n\n");
        else if (para.getNumID() != null)                      sb.append("- ").append(text.strip()).append("\n");
        else                                                   sb.append(text.strip()).append("\n\n");
    }

    private String extractParaRuns(XWPFParagraph para) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t == null || t.isEmpty()) continue;
            boolean bold = run.isBold(), italic = run.isItalic();
            if (bold && italic) sb.append("***").append(t).append("***");
            else if (bold)      sb.append("**").append(t).append("**");
            else if (italic)    sb.append("_").append(t).append("_");
            else                sb.append(t);
        }
        return sb.toString();
    }

    private void appendExtractTable(StringBuilder sb, XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return;
        int cols = rows.stream().mapToInt(r -> r.getTableCells().size()).max().orElse(0);
        if (cols == 0) return;
        String[][] cells = new String[rows.size()][cols];
        for (int r = 0; r < rows.size(); r++) {
            List<XWPFTableCell> rc = rows.get(r).getTableCells();
            for (int c = 0; c < cols; c++) cells[r][c] = c < rc.size() ? rc.get(c).getText().strip() : "";
        }
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            for (String[] row : cells) widths[c] = Math.max(widths[c], row[c] != null ? row[c].length() : 0);
            widths[c] = Math.max(widths[c], 3);
        }
        String sep = buildSep(widths);
        sb.append("\n").append(sep).append("\n");
        for (int r = 0; r < rows.size(); r++) {
            sb.append("| ");
            for (int c = 0; c < cols; c++)
                sb.append(String.format("%-" + widths[c] + "s", cells[r][c] != null ? cells[r][c] : "")).append(" | ");
            sb.append("\n");
            if (r == 0) sb.append(sep).append("\n");
        }
        sb.append(sep).append("\n\n");
    }

    private String buildSep(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) sb.append("-".repeat(w + 2)).append("+");
        return sb.toString();
    }

    private String extractDoc(byte[] bytes, StringBuilder meta) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor ex = new WordExtractor(doc)) {
            String title = doc.getSummaryInformation().getTitle();
            String author = doc.getSummaryInformation().getAuthor();
            if (title != null && !title.isBlank())  meta.append("Title  : ").append(title).append("\n");
            if (author != null && !author.isBlank()) meta.append("Author : ").append(author).append("\n");
            meta.append("Note   : .doc format — heading/list structure not preserved.\n");
            StringBuilder body = new StringBuilder();
            for (String p : ex.getParagraphText()) {
                String s = p.replaceAll("[\r]", "").strip();
                if (!s.isEmpty()) body.append(s).append("\n\n");
            }
            return body.toString().strip();
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private CTPPr getPpr(XWPFParagraph p) {
        CTP ctp = p.getCTP();
        return ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
    }

    private void paraSpacing(XWPFParagraph p, DocumentConfig cfg, int before, int after) {
        CTPPr ppr = getPpr(p);
        CTSpacing sp = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        sp.setLine(BigInteger.valueOf((long)(240 * cfg.lineSpacing())));
        sp.setLineRule(STLineSpacingRule.AUTO);
        if (before >= 0) sp.setBefore(BigInteger.valueOf(before));
        if (after  >= 0) sp.setAfter(BigInteger.valueOf(after));
    }

    private long[] parseMargins(String value) {
        if (value.contains(":")) {
            Map<String, Long> m = new HashMap<>();
            for (String part : value.split("\\s+")) {
                int c = part.indexOf(':');
                if (c > 0) m.put(part.substring(0, c).toLowerCase(), parseTwips(part.substring(c + 1)));
            }
            long def = 1440L;
            return new long[]{m.getOrDefault("top", def), m.getOrDefault("bottom", def),
                               m.getOrDefault("left", def), m.getOrDefault("right", def)};
        }
        long v = parseTwips(value);
        return new long[]{v, v, v, v};
    }

    private long parseTwips(String v) {
        if (v == null || v.isBlank()) return 1440;
        v = v.strip().toLowerCase();
        try {
            if (v.endsWith("in"))  return (long)(Double.parseDouble(v.replace("in", "")) * 1440);
            if (v.endsWith("cm"))  return (long)(Double.parseDouble(v.replace("cm", "")) * 567);
            if (v.endsWith("mm"))  return (long)(Double.parseDouble(v.replace("mm", "")) * 56.7);
            if (v.endsWith("pt"))  return (long)(Double.parseDouble(v.replace("pt", "")) * 20);
            return Long.parseLong(v);
        } catch (NumberFormatException e) { return 1440; }
    }

    private int parseEmu(String v) {
        if (v == null) return (int)(5.5 * 914400);
        v = v.strip().toLowerCase();
        try {
            if (v.endsWith("in")) return (int)(Double.parseDouble(v.replace("in","")) * 914400);
            if (v.endsWith("cm")) return (int)(Double.parseDouble(v.replace("cm","")) * 360000);
            if (v.endsWith("mm")) return (int)(Double.parseDouble(v.replace("mm","")) * 36000);
            if (v.endsWith("px")) return (int)(Double.parseDouble(v.replace("px","")) * 9525);
            return Integer.parseInt(v);
        } catch (NumberFormatException e) { return (int)(5.5 * 914400); }
    }

    private Map<String, String> parseAttrs(String attrStr) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String part : attrStr.split("\\s+")) {
            int c = part.indexOf(':');
            if (c > 0) m.put(part.substring(0, c).toLowerCase(), part.substring(c + 1));
        }
        return m;
    }

    private String resolveColor(String color) {
        if (color == null) return "000000";
        color = color.strip();
        if (color.startsWith("#")) return color.substring(1).toUpperCase();
        return switch (color.toLowerCase()) {
            case "red"          -> "FF0000";
            case "blue"         -> "0070C0";
            case "darkblue"     -> "003366";
            case "green"        -> "00B050";
            case "darkgreen"    -> "006400";
            case "orange"       -> "FF6600";
            case "purple"       -> "7030A0";
            case "black"        -> "000000";
            case "gray","grey"  -> "808080";
            case "lightgray","lightgrey" -> "D3D3D3";
            case "white"        -> "FFFFFF";
            case "yellow"       -> "FFD700";
            case "pink"         -> "FF69B4";
            case "teal"         -> "008080";
            case "brown"        -> "8B4513";
            case "gold"         -> "DAA520";
            default             -> color.length() == 6 ? color.toUpperCase() : "000000";
        };
    }

    private byte[] loadBytes(String source) throws Exception {
        String s = source.strip();
        if (s.startsWith(BASE64_PREFIX)) return Base64.getDecoder().decode(s.substring(BASE64_PREFIX.length()));
        if (s.startsWith("http://") || s.startsWith("https://"))
            try (var is = URI.create(s).toURL().openStream()) { return is.readAllBytes(); }
        return Files.readAllBytes(Path.of(s));
    }

    private String detectFormat(String source) {
        String s = source.strip().toLowerCase();
        if (s.startsWith(BASE64_PREFIX)) return "DOCX";
        int q = s.indexOf('?'); if (q > 0) s = s.substring(0, q);
        return s.endsWith(".doc") ? "DOC" : "DOCX";
    }

    private int detectPictureType(String src) {
        String s = src.toLowerCase();
        if (s.contains(".jpg") || s.contains(".jpeg")) return XWPFDocument.PICTURE_TYPE_JPEG;
        if (s.contains(".gif"))  return XWPFDocument.PICTURE_TYPE_GIF;
        if (s.contains(".bmp"))  return XWPFDocument.PICTURE_TYPE_BMP;
        if (s.contains(".wmf"))  return XWPFDocument.PICTURE_TYPE_WMF;
        if (s.contains(".emf"))  return XWPFDocument.PICTURE_TYPE_EMF;
        if (s.contains(".tif"))  return XWPFDocument.PICTURE_TYPE_TIFF;
        return XWPFDocument.PICTURE_TYPE_PNG;
    }

    private String displaySource(String s) {
        if (s != null && s.strip().startsWith(BASE64_PREFIX)) {
            int len = s.strip().length() - BASE64_PREFIX.length();
            return "[base64-encoded document, ~" + formatSize((long)(len * 0.75)) + "]";
        }
        return s;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024*1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.strip()); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.strip()); } catch (Exception e) { return def; }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
