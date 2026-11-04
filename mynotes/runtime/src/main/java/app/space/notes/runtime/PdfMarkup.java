package app.space.notes.runtime;

import android.content.Context;
import android.graphics.*;
import android.util.AtomicFile;
import com.tom_roush.pdfbox.pdmodel.*;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import java.io.*;
import java.util.*;
import org.json.*;

/** Bounded private annotation drafts and explicit PDF-copy export. Never writes the source. */
final class PdfMarkup {
    static final int MAX_STROKES=1000,MAX_POINTS=30000;
    static final class Stroke {
        final int page,color;final float width;final boolean highlight;final List<PointF> points;
        Stroke(int p,int c,float w,boolean h,List<PointF> input){page=p;color=c;width=w;highlight=h;points=new ArrayList<>();for(PointF v:input)points.add(new PointF(v.x,v.y));}
    }
    private final File draft;private final int pageCount;private final List<Stroke> strokes=new ArrayList<>();
    private final Deque<List<Stroke>> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private PdfMarkup(File draft,int pageCount){this.draft=draft;this.pageCount=pageCount;}
    static PdfMarkup open(Context context,File source,int pages)throws Exception {
        java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(source)){byte[] buffer=new byte[65536];for(int n;(n=in.read(buffer))!=-1;)hash.update(buffer,0,n);}
        StringBuilder key=new StringBuilder();for(byte b:hash.digest())key.append(String.format(Locale.ROOT,"%02x",b&255));File directory=new File(context.getFilesDir(),"pdf-annotation-drafts");if(!directory.isDirectory() && !directory.mkdirs())throw new IOException("Cannot prepare private annotation storage");
        PdfMarkup marks=new PdfMarkup(new File(directory,key+".json"),pages);AtomicFile atomic=new AtomicFile(marks.draft);
        try(InputStream input=atomic.openRead()){JSONObject root=new JSONObject(new String(LocalOpenFile.bounded(input,2*1024*1024),java.nio.charset.StandardCharsets.UTF_8));if(root.getInt("schema")!=1 || root.getInt("pages")!=pages)throw new IOException("Annotation draft format mismatch; retained without overwrite");JSONArray array=root.getJSONArray("strokes");if(array.length()>MAX_STROKES)throw new IOException("Annotation draft exceeds safety bound");int count=0;for(int i=0;i<array.length();i++){JSONObject s=array.getJSONObject(i);JSONArray points=s.getJSONArray("points");List<PointF> values=new ArrayList<>();if(points.length()<1 || (count+=points.length())>MAX_POINTS)throw new IOException("Annotation draft exceeds point bound");for(int n=0;n<points.length();n++){JSONArray xy=points.getJSONArray(n);float x=(float)xy.getDouble(0),y=(float)xy.getDouble(1);if(!Float.isFinite(x)||!Float.isFinite(y)||x<0||x>1||y<0||y>1)throw new IOException("Invalid annotation point");values.add(new PointF(x,y));}int page=s.getInt("page");float width=(float)s.getDouble("width");if(page<0||page>=pages||!Float.isFinite(width)||width<.0001f||width>.1f)throw new IOException("Invalid annotation geometry");marks.strokes.add(new Stroke(page,s.getInt("color"),width,s.getBoolean("highlight"),values));}}
        catch(FileNotFoundException fresh){}return marks;
    }
    synchronized List<Stroke> snapshot(){return new ArrayList<>(strokes);}
    private void checkpoint(){undo.addLast(new ArrayList<>(strokes));while(undo.size()>20)undo.removeFirst();redo.clear();}
    synchronized boolean add(Stroke stroke){int points=stroke.points.size();for(Stroke s:strokes)points+=s.points.size();if(strokes.size()>=MAX_STROKES||points>MAX_POINTS||stroke.points.isEmpty())return false;checkpoint();strokes.add(stroke);return true;}
    synchronized boolean erase(int page,float x,float y,float radius){for(int i=strokes.size()-1;i>=0;i--){Stroke s=strokes.get(i);if(s.page!=page)continue;for(PointF point:s.points)if(Math.hypot(point.x-x,point.y-y)<=radius){checkpoint();strokes.remove(i);return true;}}return false;}
    synchronized void undo(){if(undo.isEmpty())return;redo.addLast(new ArrayList<>(strokes));strokes.clear();strokes.addAll(undo.removeLast());}
    synchronized void redo(){if(redo.isEmpty())return;undo.addLast(new ArrayList<>(strokes));strokes.clear();strokes.addAll(redo.removeLast());}
    void persist()throws Exception {List<Stroke> copy=snapshot();JSONObject root=new JSONObject().put("schema",1).put("pages",pageCount);JSONArray array=new JSONArray();for(Stroke s:copy){JSONArray points=new JSONArray();for(PointF p:s.points)points.put(new JSONArray().put(p.x).put(p.y));array.put(new JSONObject().put("page",s.page).put("color",s.color).put("width",s.width).put("highlight",s.highlight).put("points",points));}byte[] bytes=root.put("strokes",array).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);if(bytes.length>2*1024*1024)throw new IOException("Annotation draft exceeds storage bound");AtomicFile atomic=new AtomicFile(draft);FileOutputStream stream=null;try{stream=atomic.startWrite();stream.write(bytes);atomic.finishWrite(stream);}catch(Exception error){if(stream!=null)atomic.failWrite(stream);throw error;}}
    static PointF pdfPoint(float x,float y,com.tom_roush.pdfbox.pdmodel.common.PDRectangle crop,int rotation){float u=x*((rotation%180==0)?crop.getWidth():crop.getHeight()),v=(1-y)*((rotation%180==0)?crop.getHeight():crop.getWidth());float px=u,py=v;if(rotation==90){px=crop.getWidth()-v;py=u;}else if(rotation==180){px=crop.getWidth()-u;py=crop.getHeight()-v;}else if(rotation==270){px=v;py=crop.getHeight()-u;}return new PointF(crop.getLowerLeftX()+px,crop.getLowerLeftY()+py);}
    File export(Context context,File original)throws Exception {
        List<Stroke> copy=snapshot();File destination=File.createTempFile("marked-pdf-",".pdf",context.getCacheDir());boolean done=false;
        try(PDDocument pdf=PDDocument.load(original,MemoryUsageSetting.setupTempFileOnly(128L*1024*1024).setTempDir(context.getCacheDir()))){
            Map<Integer,List<Stroke>> byPage=new TreeMap<>();for(Stroke s:copy)byPage.computeIfAbsent(s.page,k->new ArrayList<>()).add(s);
            for(Map.Entry<Integer,List<Stroke>> entry:byPage.entrySet()){PDPage page=pdf.getPage(entry.getKey());int rotation=((page.getRotation()%360)+360)%360;com.tom_roush.pdfbox.pdmodel.common.PDRectangle crop=page.getCropBox();try(PDPageContentStream stream=new PDPageContentStream(pdf,page,PDPageContentStream.AppendMode.APPEND,true,true)){for(Stroke s:entry.getValue()){stream.saveGraphicsState();PDExtendedGraphicsState state=new PDExtendedGraphicsState();state.setStrokingAlphaConstant(s.highlight?.28f:1f);stream.setGraphicsStateParameters(state);stream.setStrokingColor(Color.red(s.color),Color.green(s.color),Color.blue(s.color));stream.setLineWidth(s.width*(rotation%180==0?crop.getWidth():crop.getHeight()));stream.setLineCapStyle(1);stream.setLineJoinStyle(1);boolean first=true;for(PointF point:s.points){PointF p=pdfPoint(point.x,point.y,crop,rotation);if(first){stream.moveTo(p.x,p.y);first=false;}stream.lineTo(p.x+.001f,p.y);}stream.stroke();stream.restoreGraphicsState();}}}
            pdf.save(destination);if(destination.length()>128L*1024*1024)throw new IOException("Marked PDF exceeds export safety limit; draft retained");done=true;return destination;
        }finally{if(!done)destination.delete();}
    }
}
