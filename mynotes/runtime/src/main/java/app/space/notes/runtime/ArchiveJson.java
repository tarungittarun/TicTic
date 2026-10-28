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
    /** Ignore operational state and UUID when comparing substantive transcript content. */
    static String metadataContentHash(byte[] bytes,String id)throws IOException{
        try{JSONObject value=metadata(bytes,id);value.remove("id");value.remove("state");value.put("version",1);if(!value.has("rawText"))value.put("rawText",value.getString("text"));if(!value.has("formatting"))value.put("formatting","");return GenerationProtocol.digest(canonical(value).getBytes(StandardCharsets.UTF_8));}
        catch(IOException invalid){throw invalid;}catch(Exception | StackOverflowError invalid){throw new IOException("Cannot compare recording metadata safely");}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to compare recording metadata; originals retained");}
    }
    private static String canonical(Object value)throws Exception{
        if(value==null || value==JSONObject.NULL)return "null";if(value instanceof String)return JSONObject.quote((String)value);if(value instanceof Boolean)return value.toString();if(value instanceof Number)return JSONObject.numberToString((Number)value);
        if(value instanceof JSONObject){JSONObject object=(JSONObject)value;java.util.TreeSet<String> keys=new java.util.TreeSet<>();java.util.Iterator<String> it=object.keys();while(it.hasNext())keys.add(it.next());StringBuilder out=new StringBuilder("{");boolean comma=false;for(String key:keys){if(comma)out.append(',');comma=true;out.append(JSONObject.quote(key)).append(':').append(canonical(object.get(key)));}return out.append('}').toString();}
        if(value instanceof org.json.JSONArray){org.json.JSONArray array=(org.json.JSONArray)value;StringBuilder out=new StringBuilder("[");for(int i=0;i<array.length();i++){if(i>0)out.append(',');out.append(canonical(array.get(i)));}return out.append(']').toString();}throw new IOException("Unsupported recording metadata value");
    }

}
