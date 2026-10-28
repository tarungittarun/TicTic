package app.space.notes.domain;
import java.util.*;
/** Embedded, metadata-stripped image. Self-contained backups and folder files do not depend on URI grants. */
public final class NoteImage {
 public final String id,name,mime,data;public final int width,height;
 public NoteImage(String id,String name,String mime,int width,int height,String data){
  UUID.fromString(id);if(name==null || name.length()>200 || !("image/png".equals(mime) || "image/jpeg".equals(mime)) || width<1 || height<1 || width>1600 || height>1600 || data==null || data.length()>1400000)throw new IllegalArgumentException("Invalid note image");
  byte[] bytes=Base64.getDecoder().decode(data);if(bytes.length<4 || (mime.equals("image/png") && !(bytes[0]==(byte)137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71)) || (mime.equals("image/jpeg") && !(bytes[0]==(byte)255 && bytes[1]==(byte)216)))throw new IllegalArgumentException("Invalid image bytes");
  this.id=id;this.name=name;this.mime=mime;this.width=width;this.height=height;this.data=data;
 }
 public String fingerprint(){return id.length()+":"+id+name.length()+":"+name+mime+":"+width+":"+height+":"+data;}
}
