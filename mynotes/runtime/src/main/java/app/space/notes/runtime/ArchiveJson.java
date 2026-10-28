package app.space.notes.runtime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONObject;
import app.space.notes.domain.Note;
import app.space.notes.domain.ArchiveManifest;

/** Untrusted archive JSON gets encoding/depth checks before Android's recursive JSON parser. */
final class ArchiveJson {
    private ArchiveJson() { }
    private static String text(byte[] bytes,int limit)throws IOException{
        if(bytes==null || bytes.length<1 || bytes.length>limit)throw new IOException("Archive JSON exceeds bounds");
        try{String value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();depth(value);return value;}
        catch(java.nio.charset.CharacterCodingException invalid){throw new IOException("Archive JSON is not valid UTF-8");}
    }
    private static void depth(String value)throws IOException{
        int depth=0;boolean quoted=false,escaped=false;
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='\"')quoted=false;}else if(c=='\"')quoted=true;else if(c=='{' || c=='['){if(++depth>64)throw new IOException("Archive JSON nesting exceeds bounds");}else if(c=='}' || c==']'){if(--depth<0)throw new IOException("Archive JSON delimiters are invalid");}}
        if(quoted || depth!=0)throw new IOException("Archive JSON is incomplete");
    }
    static List<Note> library(byte[] bytes)throws IOException{
        try{JSONObject envelope=new JSONObject(text(bytes,ArchiveManifest.MAX_LIBRARY_BYTES));Object version=envelope.opt("version");if(!(version instanceof Number) || ((Number)version).doubleValue()!=1.0)throw new IOException("Unsupported archive library version");Object payload=envelope.opt("payload");if(!(payload instanceof String))throw new IOException("Archive library payload is missing");depth((String)payload);return BackupCodec.decode(bytes);}
        catch(IOException invalid){throw invalid;}catch(Exception | StackOverflowError invalid){throw new IOException("Archive library is not a valid MyNotes backup");}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to validate this archive library; source retained");}
    }
    static JSONObject metadata(byte[] bytes,String id)throws IOException{
        ArchiveManifest.requireId(id);
        try{JSONObject value=new JSONObject(text(bytes,ArchiveManifest.MAX_METADATA_BYTES));Object version=value.opt("version");if(!(version instanceof Number) || ((Number)version).doubleValue()!=1.0 || !id.equals(value.getString("id")))throw new IOException("Recording metadata identity/version mismatch");
            for(String field:new String[]{"text","state"})if(!(value.opt(field) instanceof String))throw new IOException("Recording metadata fields are invalid");
            for(String field:new String[]{"rawText","formatting"})if(value.has(field) && !(value.opt(field) instanceof String))throw new IOException("Recording metadata fields are invalid");return value;
        }catch(IOException invalid){throw invalid;}catch(Exception | StackOverflowError invalid){throw new IOException("Recording metadata is not valid archive JSON");}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to validate recording metadata; source retained");}
    }
}
