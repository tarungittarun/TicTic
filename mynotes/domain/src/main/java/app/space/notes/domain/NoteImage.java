package app.space.notes.domain;
import java.util.*;
/** Embedded, metadata-stripped image. Self-contained backups and folder files do not depend on URI grants. */
public final class NoteImage {
 public static final class Original {
  public final String mime,data;public final int width,height;
  public Original(String mime,int width,int height,String data){validatePixels(mime,width,height,data);this.mime=mime;this.width=width;this.height=height;this.data=data;}
  public static Original of(NoteImage image){return new Original(image.mime,image.width,image.height,image.data);}
  public NoteImage asImage(String id,String name){return new NoteImage(id,name,mime,width,height,data);}
 }
 public final String id,name,mime,data;public final int width,height;public final Original original;
 public NoteImage(String id,String name,String mime,int width,int height,String data){this(id,name,mime,width,height,data,null);}
 public NoteImage(String id,String name,String mime,int width,int height,String data,Original original){
  UUID.fromString(id);if(name==null || name.length()>200)throw new IllegalArgumentException("Invalid note image");validatePixels(mime,width,height,data);if(original!=null && original.data.equals(data) && original.mime.equals(mime) && original.width==width && original.height==height)original=null;
  this.id=id;this.name=name;this.mime=mime;this.width=width;this.height=height;this.data=data;this.original=original;
 }
 private static void validatePixels(String mime,int width,int height,String data){
  if(!("image/png".equals(mime) || "image/jpeg".equals(mime)) || width<1 || height<1 || width>1600 || height>1600 || data==null || data.length()>1400000)throw new IllegalArgumentException("Invalid note image");
  byte[] bytes;try{bytes=Base64.getDecoder().decode(data);}catch(IllegalArgumentException invalid){throw new IllegalArgumentException("Invalid image bytes",invalid);}
  if(bytes.length<4 || (mime.equals("image/png") && !(bytes[0]==(byte)137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71)) || (mime.equals("image/jpeg") && !(bytes[0]==(byte)255 && bytes[1]==(byte)216)))throw new IllegalArgumentException("Invalid image bytes");
 }
 public NoteImage withOriginal(Original value){return new NoteImage(id,name,mime,width,height,data,value);}
 public String fingerprint(){return id.length()+":"+id+name.length()+":"+name+mime+":"+width+":"+height+":"+data+(original==null?"":"original:"+original.mime+":"+original.width+":"+original.height+":"+original.data);}
}
