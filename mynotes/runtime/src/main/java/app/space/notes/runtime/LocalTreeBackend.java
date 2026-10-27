package app.space.notes.runtime;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

/** Local ExternalStorageProvider only. No cloud/document-network provider is accepted. */
public final class LocalTreeBackend implements GenerationProtocol.Backend {
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
