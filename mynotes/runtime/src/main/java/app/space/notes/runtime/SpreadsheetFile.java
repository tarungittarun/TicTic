package app.space.notes.runtime;

import android.content.Context;
import android.net.Uri;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Read-only sparse spreadsheet model; separate safety budgets from the 100k editor. */
final class SpreadsheetFile {
    static final int MAX_CELLS=250000,MAX_TEXT=8000000;
    static final class Cell {final String value,formula;Cell(String value,String formula){this.value=value;this.formula=formula;}}
    static final class Sheet {
        final String name;final TreeMap<Integer,TreeMap<Integer,Cell>> rows=new TreeMap<>();int rowCount,columnCount,cells;
        Sheet(String name){this.name=name;}
        Cell cell(int row,int column){Map<Integer,Cell> values=rows.get(row);return values==null?null:values.get(column);}
    }
    final List<Sheet> sheets=new ArrayList<>();int cells,characters;
    static SpreadsheetFile open(Context context,Uri uri,String name)throws Exception{
        LocalDeviceDocuments.requireReadable(uri);byte[] bytes;
        try(InputStream input=context.getContentResolver().openInputStream(uri)){bytes=LocalOpenFile.bounded(input,32*1024*1024);}
        return name.toLowerCase(Locale.ROOT).endsWith(".csv")?csv(LocalOpenFile.plain(bytes)):xlsx(bytes);
    }
    private void put(Sheet sheet,int row,int column,String value,String formula)throws IOException{
        if(row<1 || row>1048576 || column<1 || column>16384 || value.length()>32767 || formula.length()>32767)throw new IOException("Spreadsheet cell exceeds supported row/column/text bounds; original unchanged");
        if(++cells>MAX_CELLS || (characters+=value.length()+formula.length())>MAX_TEXT)throw new IOException("Spreadsheet viewer safety budget exceeded (250,000 cells / 8 million characters); original unchanged");
        TreeMap<Integer,Cell> values=sheet.rows.computeIfAbsent(row,k->new TreeMap<>());
        if(values.put(column,new Cell(value,formula))!=null)throw new IOException("Duplicate spreadsheet cell");
        sheet.cells++;sheet.rowCount=Math.max(sheet.rowCount,row);sheet.columnCount=Math.max(sheet.columnCount,column);
    }
    /** RFC4180-style quoted newlines and escaped quotes, not split-on-comma extraction. */
    static SpreadsheetFile csv(String text)throws IOException{
        SpreadsheetFile book=new SpreadsheetFile();Sheet sheet=new Sheet("CSV");book.sheets.add(sheet);
        int row=1,column=1;boolean quoted=false,afterQuote=false,fieldStart=true;StringBuilder value=new StringBuilder();
        for(int at=0;at<=text.length();at++){
            char c=at==text.length()?'\n':text.charAt(at);
            if(quoted){if(at==text.length())throw new IOException("Unclosed CSV quote");if(c=='"'){if(at+1<text.length() && text.charAt(at+1)=='"'){value.append('"');at++;}else{quoted=false;afterQuote=true;}}else value.append(c);}
            else if(c==',' || c=='\n' || c=='\r'){
                if(at==text.length() && fieldStart && column==1)break;
                book.put(sheet,row,column++,value.toString(),"");value.setLength(0);fieldStart=true;afterQuote=false;
                if(c!=','){row++;column=1;if(c=='\r' && at+1<text.length() && text.charAt(at+1)=='\n')at++;}
            }else if(c=='"' && fieldStart){quoted=true;fieldStart=false;}
            else{if(afterQuote || c=='"')throw new IOException("Invalid CSV quoting");value.append(c);fieldStart=false;}
            if(value.length()>32767)throw new IOException("CSV field exceeds spreadsheet cell safety limit");
        }
        return book;
    }
    private static Map<String,byte[]> archive(byte[] bytes)throws IOException{
        Map<String,byte[]> files=new LinkedHashMap<>();Set<String> names=new HashSet<>();int entries=0,total=0,retained=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){
            String name=entry.getName();if(++entries>4096 || !names.add(name) || name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/")).contains(".."))throw new IOException("Unsafe spreadsheet archive");
            boolean keep=name.equals("xl/workbook.xml") || name.equals("xl/_rels/workbook.xml.rels") || name.equals("xl/sharedStrings.xml") || name.matches("xl/worksheets/[^/]+\\.xml");
            ByteArrayOutputStream out=keep?new ByteArrayOutputStream():null;int size=0;byte[] buffer=new byte[8192];
            for(int n;(n=zip.read(buffer))!=-1;){size+=n;total+=n;if(size>16*1024*1024 || total>64*1024*1024)throw new IOException("Spreadsheet decompression safety limit");if(keep){retained+=n;if(retained>32*1024*1024)throw new IOException("Spreadsheet XML safety budget exceeded");out.write(buffer,0,n);}}
            if(keep)files.put(name,out.toByteArray());
        }}return files;
    }
    private static XmlPullParser parser(byte[] bytes)throws Exception{
        if(bytes==null)throw new IOException("Required spreadsheet part is missing");String text=LocalOpenFile.plain(bytes),upper=text.toUpperCase(Locale.ROOT);
        if(upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY"))throw new IOException("External XML declarations are not allowed");
        XmlPullParser parser=Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(new StringReader(text));return parser;
    }
    private static List<String> shared(byte[] bytes)throws Exception{
        List<String> strings=new ArrayList<>();if(bytes==null)return strings;XmlPullParser p=parser(bytes);StringBuilder current=null;int depth=0,total=0;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){
            if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Spreadsheet XML nesting limit");if(p.getName().equals("si"))current=new StringBuilder();else if(p.getName().equals("t") && current!=null){current.append(p.nextText());depth--;if(current.length()>32767)throw new IOException("Shared string exceeds cell limit");}}
            else if(event==XmlPullParser.END_TAG){if(p.getName().equals("si") && current!=null){total+=current.length();if(strings.size()>=100000 || total>MAX_TEXT)throw new IOException("Shared strings exceed viewer safety budget");strings.add(current.toString());current=null;}depth--;}
        }return strings;
    }
    static SpreadsheetFile xlsx(byte[] bytes)throws Exception{
        Map<String,byte[]> files=archive(bytes);List<String> strings=shared(files.remove("xl/sharedStrings.xml"));Map<String,String> relationships=new HashMap<>();
        XmlPullParser rel=parser(files.remove("xl/_rels/workbook.xml.rels"));int depth=0;
        for(int event=rel.getEventType();event!=XmlPullParser.END_DOCUMENT;event=rel.next()){
            if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Spreadsheet XML nesting limit");if(rel.getName().equals("Relationship") && !"External".equals(rel.getAttributeValue(null,"TargetMode"))){String target=rel.getAttributeValue(null,"Target"),id=rel.getAttributeValue(null,"Id");if(target!=null && id!=null){java.net.URI base=java.net.URI.create("https://offline.invalid/xl/workbook.xml"),resolved=base.resolve(target);if("https".equals(resolved.getScheme()) && "offline.invalid".equals(resolved.getHost()) && resolved.getPath().startsWith("/xl/worksheets/"))relationships.put(id,resolved.getPath().substring(1));}}}else if(event==XmlPullParser.END_TAG)depth--;
        }
        SpreadsheetFile book=new SpreadsheetFile();XmlPullParser p=parser(files.remove("xl/workbook.xml"));depth=0;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){
            if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Spreadsheet XML nesting limit");if(p.getName().equals("sheet")){
                if(book.sheets.size()>=128)throw new IOException("Spreadsheet sheet count exceeds viewer limit");String name=p.getAttributeValue(null,"name"),id=p.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");
                String part=relationships.get(id);if(part==null)throw new IOException("Worksheet relationship is missing or external");Sheet sheet=new Sheet((name==null?"Sheet":name)+("hidden".equals(p.getAttributeValue(null,"state")) || "veryHidden".equals(p.getAttributeValue(null,"state"))?" (hidden)":""));book.readSheet(sheet,files.get(part),strings);book.sheets.add(sheet);
            }}else if(event==XmlPullParser.END_TAG)depth--;
        }
        if(book.sheets.isEmpty())throw new IOException("No readable worksheets in this file");return book;
    }
    private void readSheet(Sheet sheet,byte[] bytes,List<String> shared)throws Exception{
        XmlPullParser p=parser(bytes);int row=0,column=0,next=1,depth=0,previousRow=0;boolean inCell=false,inRow=false;String type="",value="",formula="";
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){
            if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Spreadsheet XML nesting limit");String tag=p.getName();
                if(tag.equals("row")){if(inRow)throw new IOException("Nested spreadsheet row");String number=p.getAttributeValue(null,"r");row=number==null?previousRow+1:Integer.parseInt(number);if(row<=previousRow || row>1048576)throw new IOException("Invalid spreadsheet row order");next=1;inRow=true;}
                else if(tag.equals("c")){if(!inRow || inCell)throw new IOException("Invalid spreadsheet cell nesting");String reference=p.getAttributeValue(null,"r");column=reference==null?next:column(reference,row);if(column<next)throw new IOException("Invalid spreadsheet column order");type=p.getAttributeValue(null,"t");value="";formula="";inCell=true;}
                else if(inCell && (tag.equals("v") || tag.equals("t") || tag.equals("f"))){String part=p.nextText();depth--;if(tag.equals("f"))formula=part;else value+=part;if(value.length()>32767 || formula.length()>32767)throw new IOException("Spreadsheet cell safety limit");}
            }else if(event==XmlPullParser.END_TAG){String tag=p.getName();if(tag.equals("c")){if(!inCell)throw new IOException("Invalid cell end");if("s".equals(type)){int index=Integer.parseInt(value);if(index<0 || index>=shared.size())throw new IOException("Invalid shared string reference");value=shared.get(index);}else if("b".equals(type)){if(!value.equals("0") && !value.equals("1"))throw new IOException("Invalid boolean cache");value=value.equals("1")?"TRUE":"FALSE";}put(sheet,row,column,value,formula);next=column+1;inCell=false;}else if(tag.equals("row")){if(!inRow || inCell)throw new IOException("Invalid row end");previousRow=row;inRow=false;}depth--;}
        }
    }
    static int column(String reference,int row)throws IOException{
        if(!reference.matches("[A-Z]{1,3}[1-9][0-9]{0,6}"))throw new IOException("Invalid cell reference");int at=0,column=0;while(at<reference.length() && Character.isLetter(reference.charAt(at)))column=column*26+reference.charAt(at++)-'A'+1;
        if(column>16384 || Integer.parseInt(reference.substring(at))!=row)throw new IOException("Cell reference outside row/column bounds");return column;
    }
    static String letters(int column){StringBuilder value=new StringBuilder();while(column>0){column--;value.append((char)('A'+column%26));column/=26;}return value.reverse().toString();}
    String indexedText(){StringBuilder text=new StringBuilder();outer:for(Sheet sheet:sheets){text.append(sheet.name).append('\n');for(Map<Integer,Cell> row:sheet.rows.values())for(Cell cell:row.values()){text.append(cell.value).append('\n');if(text.length()>ViewedFiles.MAX_TEXT){text.setLength(ViewedFiles.MAX_TEXT+1);break outer;}}}return text.toString();}
}
