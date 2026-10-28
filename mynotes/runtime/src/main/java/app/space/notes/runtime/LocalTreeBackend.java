package app.space.notes.runtime;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

/** Local ExternalStorageProvider only. No cloud/document-network provider is accepted. */
public final class LocalTreeBackend implements GenerationProtocol.BlobBackend {
    private final ContentResolver resolver;
    private final Uri tree, directory;
    public LocalTreeBackend(ContentResolver resolver,Uri tree) throws IOException {
        if(!"content".equals(tree.getScheme()) || !"com.android.externalstorage.documents".equals(tree.getAuthority()))
            throw new IOException("Choose Internal storage or SD card using the local Android storage provider. Cloud folders are not supported.");
        this.resolver=resolver; this.tree=tree;
        try { directory=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)); }
        catch(IllegalArgumentException e) { throw new IOException("Invalid local folder",e); }
    }
    @Override public Map<String,byte[]> readAll() throws Exception {
        Map<String,byte[]> out=new HashMap<>();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(directory));
        try(Cursor c=resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)) {
            if(c==null)throw new IOException("Folder unavailable");
            while(c.moveToNext()) {
                String name=c.getString(1);
                if(name==null || !name.startsWith("mynotes-gen-") || !name.endsWith(".json"))continue;
                if(out.containsKey(name))throw new IOException("Duplicate folder generation name");
                Uri uri=DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));
                try(InputStream in=resolver.openInputStream(uri)) {
                    if(in==null)throw new IOException("Cannot read folder generation");
                    out.put(name,readBytes(in));
                }
            }
        }
        return out;
    }
    private Uri mediaDirectory(boolean create)throws Exception{
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(directory));Uri found=null;
        try(Cursor c=resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(c==null)throw new IOException("Folder unavailable");while(c.moveToNext())if(".mynotes-media-v1".equals(c.getString(1))){if(found!=null || !DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2)))throw new IOException("Media directory name collision");found=DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));}}
        if(found==null && create)found=DocumentsContract.createDocument(resolver,directory,DocumentsContract.Document.MIME_TYPE_DIR,".mynotes-media-v1");if(found==null)throw new IOException("Media directory unavailable");return found;
    }
    private Uri blob(String hash,boolean create)throws Exception{
        if(!hash.matches("[0-9a-f]{64}"))throw new IOException("Invalid media hash");Uri parent=mediaDirectory(create),found=null;Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(parent));
        try(Cursor c=resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(c==null)throw new IOException("Media directory unavailable");while(c.moveToNext())if((hash+".bin").equals(c.getString(1))){if(found!=null)throw new IOException("Duplicate media file");found=DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));}}
        return found;
    }
    @Override public byte[] readBlob(String hash)throws Exception{Uri file=blob(hash,false);if(file==null)throw new IOException("Referenced media is missing");try(InputStream in=resolver.openInputStream(file)){if(in==null)throw new IOException("Media unavailable");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>1024*1024)throw new IOException("Media file is too large");out.write(buffer,0,count);}return out.toByteArray();}}
    @Override public void createBlob(String hash,byte[] bytes)throws Exception{
        if(!GenerationProtocol.digest(bytes).equals(hash))throw new IOException("Media hash mismatch");Uri existing=blob(hash,true);if(existing!=null){if(!Arrays.equals(bytes,readBlob(hash)))throw new IOException("Existing media is damaged; refusing overwrite");return;}
        Uri parent=mediaDirectory(true);Uri temp=DocumentsContract.createDocument(resolver,parent,"application/octet-stream","pending-"+UUID.randomUUID()+".part");if(temp==null)throw new IOException("Cannot create media");
        try(OutputStream out=resolver.openOutputStream(temp,"w")){if(out==null)throw new IOException("Cannot write media");out.write(bytes);out.flush();}try(InputStream in=resolver.openInputStream(temp)){if(in==null || !Arrays.equals(bytes,readBytes(in)))throw new IOException("Media readback mismatch");}
        Uri renamed=DocumentsContract.renameDocument(resolver,temp,hash+".bin");if(renamed==null || !Arrays.equals(bytes,readBlob(hash)))throw new IOException("Media rename verification failed");
    }
    static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream(); byte[] chunk=new byte[8192]; int read;
        while((read=in.read(chunk))!=-1)buffer.write(chunk,0,read);
        return buffer.toByteArray();
    }
    @Override public void create(String name,byte[] bytes) throws Exception {
        Uri file=DocumentsContract.createDocument(resolver,directory,"application/json",name);
        if(file==null)throw new IOException("Could not create generation");
        try(OutputStream stream=resolver.openOutputStream(file,"w")) {
            if(stream==null)throw new IOException("Folder is not writable");
            stream.write(bytes); stream.flush();
        }
        // The protocol reopens and verifies; provider close/flush is not a power-loss guarantee.
    }
}
