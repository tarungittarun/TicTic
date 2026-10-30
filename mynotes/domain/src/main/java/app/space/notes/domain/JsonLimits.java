package app.space.notes.domain;
import java.io.IOException;
/** Non-recursive preflight before a recursive JSON parser. Not a replacement for schema validation. */
public final class JsonLimits {
    public static final int MAX_DEPTH=32,MAX_STRUCTURES=100000;
    private JsonLimits(){}
    public static String text(byte[] bytes)throws IOException{
        require(bytes);return java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    }
    public static void require(byte[] bytes)throws IOException{
        if(bytes==null)throw new IOException("Missing document"); // Byte limits belong to the owning import/storage boundary.
        byte[] stack=new byte[MAX_DEPTH];int depth=0,structures=0;boolean quoted=false,escaped=false;
        for(byte value:bytes){
            if(quoted){if(escaped)escaped=false;else if(value=='\\')escaped=true;else if(value=='"')quoted=false;continue;}
            if(value=='"'){quoted=true;continue;}
            if(value=='{' || value=='['){if(depth==MAX_DEPTH || ++structures>MAX_STRUCTURES)throw new IOException("Document structure exceeds safety limit");stack[depth++]=value;}
            else if(value=='}' || value==']'){if(depth==0 || stack[--depth]!=(value=='}'?'{':'['))throw new IOException("Unbalanced document structure");}
        }
        if(quoted || depth!=0)throw new IOException("Incomplete document structure");
    }
}
