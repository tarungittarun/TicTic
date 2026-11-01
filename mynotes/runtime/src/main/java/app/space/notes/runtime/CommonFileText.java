package app.space.notes.runtime;

import android.text.Html;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.*;

/** Bounded text-only views. Never evaluates formulas, macros, scripts, URLs or archive paths. */
public final class CommonFileText {
    private CommonFileText(){}
    public static boolean supported(String name){return name.toLowerCase(Locale.ROOT).matches(".*\\.(csv|md|markdown|html|htm|rtf|odt|xlsx|pptx)$");}
    public static String read(byte[] bytes,String name)throws Exception{
        String lower=name.toLowerCase(Locale.ROOT),text;
        if(lower.endsWith(".xlsx") || lower.endsWith(".pptx") || lower.endsWith(".odt"))return office(bytes,lower);
        if(bytes.length>512*1024)throw new IOException("Text preview is limited to 512 KiB");
        text=LocalOpenFile.plain(bytes);
        if(lower.endsWith(".html") || lower.endsWith(".htm"))text=Html.fromHtml(text.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>",""),Html.FROM_HTML_MODE_LEGACY).toString();
        else if(lower.endsWith(".rtf"))text=rtf(text);
        return checked(text);
    }
    private static String checked(String text)throws IOException{if(text.length()>100000)throw new IOException("Readable text exceeds 100,000 characters; source unchanged");app.space.notes.domain.RichText.requireText(text);return text;}
    private static void add(StringBuilder out,String text)throws IOException{if(out.length()+text.length()>100000)throw new IOException("Readable text exceeds 100,000 characters");out.append(text);}
    private static Map<String,byte[]> archive(byte[] bytes)throws IOException{
        if(bytes.length>8*1024*1024)throw new IOException("Office archive exceeds 8 MiB");Map<String,byte[]> result=new TreeMap<>();Set<String> names=new HashSet<>();int entries=0,total=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){for(ZipEntry e;(e=zip.getNextEntry())!=null;){String name=e.getName();if(++entries>1024 || !names.add(name) || name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/")).contains(".."))throw new IOException("Unsafe Office archive");boolean keep=name.equals("content.xml") || name.equals("xl/sharedStrings.xml") || name.matches("xl/worksheets/sheet[0-9]+\\.xml") || name.matches("ppt/slides/slide[0-9]+\\.xml");ByteArrayOutputStream out=keep?new ByteArrayOutputStream():null;int size=0;byte[] b=new byte[8192];for(int n;(n=zip.read(b))!=-1;){size+=n;total+=n;if(size>(keep?2:8)*1024*1024 || total>16*1024*1024)throw new IOException("Office decompression limit exceeded");if(keep)out.write(b,0,n);}if(keep)result.put(name,out.toByteArray());}}
        return result;
    }
    private static XmlPullParser parser(byte[] bytes)throws Exception{String text=LocalOpenFile.plain(bytes);String upper=text.toUpperCase(Locale.ROOT);if(upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY"))throw new IOException("External XML declarations are not permitted");XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);p.setInput(new StringReader(text));return p;}
    private static List<String> strings(byte[] bytes)throws Exception{List<String> result=new ArrayList<>();if(bytes==null)return result;XmlPullParser p=parser(bytes);StringBuilder current=null;int depth=0;for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("XML nesting limit");if(p.getName().equals("si")){if(result.size()>=10000)throw new IOException("Shared string count limit");current=new StringBuilder();}else if(p.getName().equals("t") && current!=null){add(current,p.nextText());depth--;}}else if(event==XmlPullParser.END_TAG){if(p.getName().equals("si") && current!=null){result.add(current.toString());current=null;}depth--;}}return result;}
    private static String office(byte[] bytes,String name)throws Exception{
        Map<String,byte[]> files=archive(bytes);StringBuilder out=new StringBuilder();
        if(name.endsWith(".xlsx")){
            List<String> shared=strings(files.get("xl/sharedStrings.xml"));List<String> sheets=new ArrayList<>();for(String path:files.keySet())if(path.startsWith("xl/worksheets/"))sheets.add(path);sortParts(sheets);if(sheets.isEmpty())throw new IOException("Spreadsheet worksheets are missing");int index=0;
            for(String path:sheets){add(out,"\nSheet "+(++index)+" — cached values only\n");spreadsheet(out,files.get(path),shared);}
        }else if(name.endsWith(".pptx")){
            List<String> slides=new ArrayList<>();for(String path:files.keySet())if(path.startsWith("ppt/slides/"))slides.add(path);sortParts(slides);if(slides.isEmpty())throw new IOException("Presentation slides are missing");int index=0;for(String path:slides){add(out,"\nSlide "+(++index)+"\n");XmlPullParser p=parser(files.get(path));int depth=0;for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("XML nesting limit");if(p.getName().equals("t")){add(out,p.nextText());depth--;}}else if(event==XmlPullParser.END_TAG){if(p.getName().equals("p"))add(out,"\n");depth--;}}}
        }else{
            byte[] content=files.get("content.xml");if(content==null)throw new IOException("OpenDocument body is missing");XmlPullParser p=parser(content);int depth=0,paragraph=0;for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("XML nesting limit");if(p.getName().equals("p") || p.getName().equals("h"))paragraph++;if(paragraph>0 && p.getName().equals("tab"))add(out,"\t");}else if(event==XmlPullParser.TEXT && paragraph>0)add(out,p.getText());else if(event==XmlPullParser.END_TAG){if(p.getName().equals("p") || p.getName().equals("h")){paragraph--;add(out,"\n");}depth--;}}
        }
        return checked(out.toString());
    }
    /** Explicit row numbers and blank columns avoid shifting sparse cached values left. */
    private static void spreadsheet(StringBuilder out,byte[] bytes,List<String> shared)throws Exception{
        XmlPullParser p=parser(bytes);int depth=0,row=0,previousRow=0,column=0,nextColumn=1;boolean inRow=false,inCell=false;String type=null,value="";
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){
            if(event==XmlPullParser.START_TAG){
                if(++depth>64)throw new IOException("XML nesting limit");String tag=p.getName();
                if(tag.equals("row")){if(inRow)throw new IOException("Nested spreadsheet row");String ref=p.getAttributeValue(null,"r");row=ref==null?previousRow+1:positive(ref,1048576);if(row<=previousRow || row>1048576)throw new IOException("Unordered or oversized spreadsheet rows");inRow=true;nextColumn=1;add(out,"Row "+row+"\t");}
                else if(tag.equals("c")){if(!inRow || inCell)throw new IOException("Invalid spreadsheet cell");String ref=p.getAttributeValue(null,"r");column=ref==null?nextColumn:column(ref,row);if(column<nextColumn || column>16384)throw new IOException("Unordered or oversized spreadsheet columns");inCell=true;type=p.getAttributeValue(null,"t");value="";}
                else if(inCell && (tag.equals("v") || tag.equals("t"))){String part=p.nextText();depth--;if(value.length()+part.length()>100000)throw new IOException("Cell text limit");value+=part;}
            }else if(event==XmlPullParser.END_TAG){
                if(p.getName().equals("c")){if(!inCell)throw new IOException("Invalid spreadsheet cell end");if("s".equals(type)){int at;try{at=Integer.parseInt(value);}catch(NumberFormatException bad){throw new IOException("Invalid shared string reference");}if(at<0 || at>=shared.size())throw new IOException("Missing shared string");value=shared.get(at);}else if("b".equals(type) && !value.isEmpty()){if(!value.equals("0") && !value.equals("1"))throw new IOException("Invalid boolean cached value");value=value.equals("1")?"true":"false";}while(nextColumn<column){add(out,"\t");nextColumn++;}add(out,value);add(out,"\t");nextColumn++;inCell=false;}
                else if(p.getName().equals("row")){if(!inRow || inCell)throw new IOException("Invalid spreadsheet row end");add(out,"\n");previousRow=row;inRow=false;}depth--;
            }
        }
    }
    private static int positive(String value,int max)throws IOException{try{if(!value.matches("[1-9][0-9]{0,6}"))throw new NumberFormatException();int number=Integer.parseInt(value);if(number>max)throw new NumberFormatException();return number;}catch(NumberFormatException bad){throw new IOException("Invalid spreadsheet row");}}
    private static int column(String reference,int row)throws IOException{if(!reference.matches("[A-Z]{1,3}[1-9][0-9]{0,6}"))throw new IOException("Invalid cell reference");int at=0,column=0;while(at<reference.length() && reference.charAt(at)>='A' && reference.charAt(at)<='Z')column=column*26+reference.charAt(at++)-'A'+1;if(column>16384 || positive(reference.substring(at),1048576)!=row)throw new IOException("Cell reference outside row or column limits");return column;}
    private static void sortParts(List<String> paths){paths.sort(Comparator.comparingInt(p->{try{return Integer.parseInt(p.replaceAll("[^0-9]",""));}catch(NumberFormatException bad){return Integer.MAX_VALUE;}}));}
    static String rtf(String source)throws IOException{
        if(!source.startsWith("{\\rtf"))throw new IOException("Not an RTF document");StringBuilder out=new StringBuilder();Deque<Boolean> stack=new ArrayDeque<>();boolean skip=false;int fallback=1,pending=0;
        for(int i=0;i<source.length();i++){char c=source.charAt(i);if(c=='{'){if(stack.size()>=64)throw new IOException("RTF nesting limit");stack.push(skip);}else if(c=='}'){if(stack.isEmpty())throw new IOException("Unbalanced RTF");skip=stack.pop();}else if(c=='\\'){
            if(++i>=source.length())throw new IOException("Truncated RTF");char next=source.charAt(i);if(next=='*'){skip=true;continue;}if(next=='\\' || next=='{' || next=='}'){if(pending>0)pending--;else if(!skip)add(out,String.valueOf(next));continue;}if(next=='\''){if(i+2>=source.length())throw new IOException("Invalid RTF escape");try{int value=Integer.parseInt(source.substring(i+1,i+3),16);if(pending>0)pending--;else if(!skip)add(out,new String(new byte[]{(byte)value},Charset.forName("windows-1252")));}catch(NumberFormatException bad){throw new IOException("Invalid RTF hex");}i+=2;continue;}
            int start=i;while(i<source.length() && Character.isLetter(source.charAt(i)))i++;String word=source.substring(start,i);int numberStart=i;if(i<source.length() && source.charAt(i)=='-')i++;while(i<source.length() && Character.isDigit(source.charAt(i)))i++;Integer number=null;if(i>numberStart){try{number=Integer.parseInt(source.substring(numberStart,i));}catch(NumberFormatException bad){throw new IOException("Invalid RTF control");}}if(i<source.length() && source.charAt(i)==' '){}else i--;
            if(Arrays.asList("fonttbl","colortbl","stylesheet","info","pict","object","fldinst").contains(word))skip=true;
            if(word.equals("bin")){if(number==null || number<0 || number>source.length()-i-1)throw new IOException("Invalid binary RTF data");i+=number;}
            else if(word.equals("uc")){if(number==null || number<0 || number>8)throw new IOException("Invalid RTF Unicode fallback");fallback=number;}
            else if(word.equals("ansicpg") && number!=null && number!=1252)throw new IOException("This RTF code page is unsupported; original unchanged");
            else if(!skip && word.equals("u")){if(number==null || number<-32768 || number>65535)throw new IOException("Invalid RTF Unicode");add(out,String.valueOf((char)(number&65535)));pending=fallback;}
            else if(!skip && (word.equals("par") || word.equals("line")))add(out,"\n");else if(!skip && word.equals("tab"))add(out,"\t");
        }else if(c!='\n' && c!='\r'){if(pending>0)pending--;else if(!skip)add(out,String.valueOf(c));}}
        if(!stack.isEmpty())throw new IOException("Unbalanced RTF");return checked(out.toString());
    }
}
