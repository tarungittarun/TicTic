package app.space.notes.runtime;
import app.space.notes.domain.SafeLinks;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
/** Incremental URI annotations for OUR freshly generated, unencrypted classic-xref Android PDFs only.
 * Never an importer/general PDF parser. Unsupported producer layouts fail before destination copying.
 * Traverses the actual page tree (not object order); stream bytes are never searched for page objects.
 */
public final class PdfLinks {
    private PdfLinks(){}
    public static final class Link {
        public final int page;public final float left,top,right,bottom;public final String uri;
        public Link(int page,float left,float top,float right,float bottom,String uri){if(page<1 || page>200 || !Float.isFinite(left) || !Float.isFinite(top) || !Float.isFinite(right) || !Float.isFinite(bottom) || left<0 || top<0 || right>595 || bottom>842 || right<=left || bottom<=top)throw new IllegalArgumentException("Invalid PDF link bounds");this.page=page;this.left=left;this.top=top;this.right=right;this.bottom=bottom;try{this.uri=new java.net.URI(SafeLinks.requireWeb(uri)).toASCIIString();}catch(java.net.URISyntaxException invalid){throw new IllegalArgumentException("Invalid PDF URL",invalid);}}
    }
    private static String match(String text,String pattern)throws IOException{Matcher value=Pattern.compile(pattern).matcher(text);if(!value.find())throw new IOException("Unsupported generated PDF structure; notes unchanged");return value.group(1);}
    private static int number(String value)throws IOException{try{int n=Integer.parseInt(value);if(n<0 || n>1000000)throw new IOException("PDF structure limit");return n;}catch(NumberFormatException invalid){throw new IOException("Invalid generated PDF reference",invalid);}}
    private static String dictionary(RandomAccessFile file,Map<Integer,Long> offsets,int id)throws IOException{
        Long offset=offsets.get(id);if(offset==null)throw new IOException("Missing generated PDF object");file.seek(offset);StringBuilder out=new StringBuilder();for(int count=0;count<65536;count++){int next=file.read();if(next<0)break;out.append((char)next);if(out.length()>=6 && out.substring(out.length()-6).equals("endobj")){String raw=out.toString();if(!raw.matches("(?s)^"+id+"\\s+0\\s+obj\\b.*") || raw.contains("stream"))throw new IOException("Unexpected PDF page-tree object");int start=raw.indexOf("<<"),end=raw.lastIndexOf(">>");if(start<0 || end<start)throw new IOException("Invalid generated PDF dictionary");return raw.substring(start,end+2);}}throw new IOException("Generated PDF dictionary exceeds limit");
    }
    private static void pages(RandomAccessFile file,Map<Integer,Long> offsets,int id,List<Integer> result,Set<Integer> seen,int depth)throws IOException{
        if(depth>16 || !seen.add(id) || seen.size()>1000)throw new IOException("Invalid PDF page tree");String node=dictionary(file,offsets,id);if(Pattern.compile("/Type\\s*/Page\\b").matcher(node).find()){if(result.size()>=200)throw new IOException("PDF page limit");result.add(id);return;}
        if(!Pattern.compile("/Type\\s*/Pages\\b").matcher(node).find())throw new IOException("Invalid PDF page tree node");String kids=match(node,"/Kids\\s*\\[([^]]*)\\]");Matcher child=Pattern.compile("(\\d+)\\s+0\\s+R").matcher(kids);int children=0;while(child.find()){children++;pages(file,offsets,number(child.group(1)),result,seen,depth+1);}if(children==0)throw new IOException("Empty PDF page tree");
    }
    private static String decimal(float value){return String.format(Locale.ROOT,"%.3f",value);}
    private static void ascii(OutputStream out,String value)throws IOException{out.write(value.getBytes(StandardCharsets.US_ASCII));}
    /** Appends a bounded incremental revision to the private temporary file, not to a user's source. */
    public static void annotate(File temporary,List<Link> links)throws IOException{
        if(links.isEmpty())return;if(links.size()>8192)throw new IOException("Too many PDF link rectangles; split the export");
        Map<Integer,Long> offsets=new HashMap<>();Map<Integer,String> updates=new TreeMap<>();int root,size;long previous;String info="";
        try(RandomAccessFile input=new RandomAccessFile(temporary,"r")){
            long length=input.length();if(length<16)throw new IOException("Incomplete generated PDF");input.seek(Math.max(0,length-8192));byte[] tail=new byte[(int)Math.min(length,8192)];input.readFully(tail);String ending=new String(tail,StandardCharsets.ISO_8859_1);try{previous=Long.parseLong(match(ending,"startxref\\s+(\\d+)\\s+%%EOF\\s*$"));}catch(NumberFormatException invalid){throw new IOException("Invalid PDF xref",invalid);}if(previous<0 || previous>=length)throw new IOException("Invalid PDF xref offset");input.seek(previous);if(!"xref".equals(input.readLine()))throw new IOException("Unsupported generated PDF xref; no destination written");
            String line;while((line=input.readLine())!=null && !line.trim().equals("trailer")){if(line.trim().isEmpty())continue;String[] section=line.trim().split("\\s+");if(section.length!=2)throw new IOException("Invalid PDF xref section");int first=number(section[0]),count=number(section[1]);if(count>100000 || first+count>100000 || offsets.size()+count>100000)throw new IOException("PDF object safety limit");for(int i=0;i<count;i++){String entry=input.readLine();if(entry==null || entry.length()>80)throw new IOException("Invalid PDF xref entry");String[] parts=entry.trim().split("\\s+");if(parts.length!=3)throw new IOException("Invalid PDF xref entry");if(parts[2].equals("n")){long at;try{at=Long.parseLong(parts[0]);}catch(NumberFormatException bad){throw new IOException("Invalid PDF object offset",bad);}if(!parts[1].equals("00000") || at<0 || at>=previous)throw new IOException("Unsupported PDF object generation");offsets.put(first+i,at);}}}
            if(line==null)throw new IOException("Missing PDF trailer");StringBuilder trailer=new StringBuilder();while((line=input.readLine())!=null && !line.trim().equals("startxref")){trailer.append(line).append('\n');if(trailer.length()>8192)throw new IOException("PDF trailer limit");}String t=trailer.toString();if(t.contains("/Encrypt") || t.contains("/Prev"))throw new IOException("Only fresh, unencrypted Android PDFs are supported");root=number(match(t,"/Root\\s+(\\d+)\\s+0\\s+R"));size=number(match(t,"/Size\\s+(\\d+)"));if(size<=Collections.max(offsets.keySet()))throw new IOException("Invalid PDF object count");Matcher metadata=Pattern.compile("/Info\\s+(\\d+)\\s+0\\s+R").matcher(t);if(metadata.find())info=" /Info "+metadata.group(1)+" 0 R";
            int tree=number(match(dictionary(input,offsets,root),"/Pages\\s+(\\d+)\\s+0\\s+R"));List<Integer> ordered=new ArrayList<>();pages(input,offsets,tree,ordered,new HashSet<>(),0);Map<Integer,StringBuilder> references=new HashMap<>();int next=size;
            long uriBytes=0;for(Link link:links){uriBytes+=link.uri.length();if(uriBytes>1024*1024)throw new IOException("PDF link metadata exceeds1MiB; split the export");if(link.page>ordered.size())throw new IOException("PDF link page missing");int page=ordered.get(link.page-1);references.computeIfAbsent(page,key->new StringBuilder()).append(next).append(" 0 R ");StringBuilder hex=new StringBuilder();for(byte value:link.uri.getBytes(StandardCharsets.US_ASCII)){int b=value&255;hex.append("0123456789abcdef".charAt(b>>>4)).append("0123456789abcdef".charAt(b&15));}updates.put(next++,"<< /Type /Annot /Subtype /Link /Rect ["+decimal(link.left)+" "+decimal(842-link.bottom)+" "+decimal(link.right)+" "+decimal(842-link.top)+"] /Border [0 0 0] /A << /S /URI /URI <"+hex+"> >> >>");}
            for(Map.Entry<Integer,StringBuilder> entry:references.entrySet()){String page=dictionary(input,offsets,entry.getKey());if(page.contains("/Annots"))throw new IOException("Unexpected preexisting PDF annotations");updates.put(entry.getKey(),page.substring(0,page.length()-2)+" /Annots ["+entry.getValue()+"] >>");}size=next;
        }
        try(FileOutputStream out=new FileOutputStream(temporary,true)){
            ascii(out,"\n");Map<Integer,Long> revised=new TreeMap<>();for(Map.Entry<Integer,String> entry:updates.entrySet()){revised.put(entry.getKey(),out.getChannel().position());ascii(out,entry.getKey()+" 0 obj\n"+entry.getValue()+"\nendobj\n");}
            long xref=out.getChannel().position();ascii(out,"xref\n");for(Map.Entry<Integer,Long> entry:revised.entrySet())ascii(out,entry.getKey()+" 1\n"+String.format(Locale.ROOT,"%010d 00000 n \n",entry.getValue()));ascii(out,"trailer\n<< /Size "+size+" /Root "+root+" 0 R /Prev "+previous+info+" >>\nstartxref\n"+xref+"\n%%EOF\n");out.flush();
        }
    }
}
