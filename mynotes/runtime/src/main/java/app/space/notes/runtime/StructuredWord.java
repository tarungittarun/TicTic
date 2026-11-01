package app.space.notes.runtime;

import android.util.Xml;
import app.space.notes.domain.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.xmlpull.v1.XmlPullParser;

/** Bounded, non-executing OOXML subset. Never opens relationships, macros or external links.
 * Unsupported layout is disclosed; unrepresentable nested/merged tables are refused.
 */
public final class StructuredWord {

  private StructuredWord() {}

  public static final String LIMITS =
    "Editable DOCX copy: paragraphs, direct bold/italic/underline/strike, heading 1/2, supported text sizes and simple rectangular tables. Word pagination, pictures, headers/footers, comments, revisions, linked targets and inherited styles are not preserved. Lists use bullet markers; review numbering. Merged/nested tables are not supported. The original stays unchanged; this is not a Word-layout editor.";

  private static boolean word(XmlPullParser p) {
    return (
      "http://schemas.openxmlformats.org/wordprocessingml/2006/main".equals(
        p.getNamespace()
      ) ||
      "http://purl.oclc.org/ooxml/wordprocessingml/main".equals(
        p.getNamespace()
      )
    );
  }

  private static String val(XmlPullParser p) {
    String value = p.getAttributeValue(p.getNamespace(), "val");
    return value == null ? "" : value;
  }

  private static boolean on(XmlPullParser p) {
    String v = val(p);
    return (
      !v.equals("0") &&
      !v.equals("false") &&
      !v.equals("off") &&
      !v.equals("none")
    );
  }

  private static final class Text {

    final StringBuilder value = new StringBuilder();
    final List<Note.Mark> marks = new ArrayList<>();

    void add(String s) {
      value.append(s);
      if (value.length() > 100000) throw new IllegalArgumentException(
        "Word text exceeds safe size"
      );
    }

    void append(RichText text) {
      int offset = value.length();
      add(text.text);
      for (Note.Mark m : text.marks)
        marks.add(
          new Note.Mark(m.type, offset + m.start, offset + m.end, m.value)
        );
    }

    RichText build() {
      return new RichText(value.toString(), marks);
    }
  }

  public static DocumentContent read(byte[] archive) throws Exception {
    LocalOpenFile.docx(archive); // Validate compressed/aggregate sizes, declarations and source text first.
    byte[] body = null;
    try (
      ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))
    ) {
      for (ZipEntry e; (e = zip.getNextEntry()) != null; ) if (
        e.getName().equals("word/document.xml")
      ) {
        body = LocalOpenFile.bounded(zip, 2 * 1024 * 1024);
        break;
      }
    }
    if (body == null) throw new IOException("Word body is missing");
    XmlPullParser p = Xml.newPullParser();
    p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    p.setInput(new StringReader(LocalOpenFile.plain(body)));
    List<DocumentContent.Block> blocks = new ArrayList<>();
    List<List<RichText>> table = null;
    List<RichText> row = null;
    Text cell = null,
      paragraph = null;
    Set<String> styles = new LinkedHashSet<>();
    int runStart = 0,
      depth = 0,
      skip = -1,
      total = 0;
    String size = "",
      heading = "";
    boolean bodySeen = false,
      bullet = false,
      header = false;
    for (
      int event = p.getEventType();
      event != XmlPullParser.END_DOCUMENT;
      event = p.next()
    ) {
      String tag = p.getName();
      boolean w = word(p);
      if (event == XmlPullParser.START_TAG) {
        if (++depth > 128) throw new IOException(
          "Word XML nesting is too deep"
        );
        if (skip >= 0) continue;
        if (
          w &&
          Arrays.asList(
            "del",
            "drawing",
            "pict",
            "txbxContent",
            "sdtPr"
          ).contains(tag)
        ) {
          skip = depth;
          continue;
        }
        if (!w) continue;
        switch (tag) {
          case "body":
            bodySeen = true;
            break;
          case "tbl":
            if (table != null) throw new IOException(
              "Nested Word tables cannot be imported faithfully; open the original in Word"
            );
            table = new ArrayList<>();
            header = false;
            break;
          case "tr":
            if (table != null) row = new ArrayList<>();
            break;
          case "tc":
            if (row != null) cell = new Text();
            break;
          case "gridSpan":
            if (!val(p).equals("1")) throw new IOException(
              "Merged Word cells are unsupported; original unchanged"
            );
            break;
          case "vMerge":
          case "hMerge":
            throw new IOException(
              "Merged Word cells are unsupported; original unchanged"
            );
          case "tblHeader":
            header = on(p);
            break;
          case "p":
            if (paragraph != null) throw new IOException(
              "Nested Word paragraphs are unsupported"
            );
            paragraph = new Text();
            heading = "";
            bullet = false;
            break;
          case "pStyle":
            if (val(p).equalsIgnoreCase("Heading1")) heading = "h1";
            else if (val(p).equalsIgnoreCase("Heading2")) heading = "h2";
            break;
          case "numPr":
            bullet = true;
            break;
          case "r":
            styles.clear();
            size = "";
            runStart = paragraph == null ? 0 : paragraph.value.length();
            break;
          case "b":
          case "i":
          case "u":
          case "strike":
            if (on(p)) styles.add(
              tag.equals("b")
                ? "bold"
                : tag.equals("i")
                  ? "italic"
                  : tag.equals("u")
                    ? "underline"
                    : "strike"
            );
            break;
          case "sz":
            try {
              int half = Integer.parseInt(val(p));
              if (half >= 16 && half <= 144 && half % 2 == 0) size =
                Integer.toString(half / 2);
            } catch (NumberFormatException ignored) {}
            break;
          case "t":
            if (paragraph != null) {
              paragraph.add(p.nextText());
              depth--;
            }
            break;
          case "tab":
            if (paragraph != null) paragraph.add("\t");
            break;
          case "br":
          case "cr":
            if (paragraph != null) paragraph.add("\n");
            break;
        }
      } else if (event == XmlPullParser.END_TAG) {
        if (skip >= 0) {
          if (depth == skip) skip = -1;
          depth--;
          continue;
        }
        if (w) switch (tag) {
          case "r":
            if (paragraph != null && paragraph.value.length() > runStart) {
              for (String style : styles)
                paragraph.marks.add(
                  new Note.Mark(style, runStart, paragraph.value.length())
                );
              if (!size.isEmpty()) paragraph.marks.add(
                new Note.Mark("size", runStart, paragraph.value.length(), size)
              );
            }
            break;
          case "p":
            if (paragraph != null) {
              if (
                !heading.isEmpty() && paragraph.value.length() > 0
              ) paragraph.marks.add(
                new Note.Mark(heading, 0, paragraph.value.length())
              );
              RichText text = paragraph.build();
              if (bullet) {
                Text prefixed = new Text();
                prefixed.add("• ");
                prefixed.append(text);
                text = prefixed.build();
              }
              total += text.text.length();
              if (total > 100000) throw new IOException(
                "Word text exceeds safe size"
              );
              if (cell != null) {
                if (cell.value.length() > 0) cell.add("\n");
                cell.append(text);
              } else blocks.add(
                DocumentContent.Block.text(UUID.randomUUID().toString(), text)
              );
              paragraph = null;
            }
            break;
          case "tc":
            if (cell != null && row != null) {
              row.add(cell.build());
              cell = null;
            }
            break;
          case "tr":
            if (row != null && table != null) {
              table.add(row);
              row = null;
              if (table.size() > TextTable.MAX_ROWS) throw new IOException(
                "Word table exceeds safe rows"
              );
            }
            break;
          case "tbl":
            if (table != null) {
              try {
                blocks.add(
                  DocumentContent.Block.table(
                    UUID.randomUUID().toString(),
                    "",
                    TextTable.rich(table, header)
                  )
                );
              } catch (IllegalArgumentException invalid) {
                throw new IOException(
                  "Word table is not a supported rectangular editable table",
                  invalid
                );
              }
              table = null;
            }
            break;
        }
        depth--;
      }
      if (
        blocks.size() > DocumentContent.MAX_BLOCKS ||
        (row != null && row.size() > TextTable.MAX_COLUMNS)
      ) throw new IOException("Word structure exceeds safe editing limits");
    }
    if (!bodySeen) throw new IOException("Word document body is missing");
    return new DocumentContent(blocks, false);
  }
}
