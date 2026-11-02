package app.space.notes.runtime;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.net.Uri;
import java.io.File;
import java.util.*;

/** Private, bounded read-history/search index. Never an authority or an editable note. */
public final class ViewedFiles extends SQLiteOpenHelper {
    public static final int MAX_FILES=32,MAX_TEXT=2*1024*1024;
    public ViewedFiles(Context context){super(context,new File(context.getFilesDir(),"viewed-search-v1.sqlite").getPath(),null,1);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE files(id INTEGER PRIMARY KEY,uri TEXT UNIQUE NOT NULL,name TEXT NOT NULL,mime TEXT NOT NULL,viewed INTEGER NOT NULL,partial INTEGER NOT NULL)");db.execSQL("CREATE VIRTUAL TABLE words USING fts4(name,body,tokenize=unicode61 'tokenchars="+marks()+"')");}
    private static String marks(){StringBuilder value=new StringBuilder();for(char c=0;c<Character.MAX_VALUE;c++){int type=Character.getType(c);if(type==Character.NON_SPACING_MARK || type==Character.COMBINING_SPACING_MARK || type==Character.ENCLOSING_MARK)value.append(c);}return value.toString();}
    @Override public void onUpgrade(SQLiteDatabase db,int old,int version){throw new IllegalStateException("Unsupported viewed-file index version; original files are unchanged");}
    public static final class Entry {public final Uri uri;public final String name,mime;public final long viewed;public final boolean partial;Entry(Cursor c){uri=Uri.parse(c.getString(0));name=c.getString(1);mime=c.getString(2);viewed=c.getLong(3);partial=c.getInt(4)!=0;}}
    public void remember(Uri uri,String name,String mime,String body)throws Exception{remember(uri,name,mime,body,false);}
    public void remember(Uri uri,String name,String mime,String body,boolean partial)throws Exception{
        LocalDeviceDocuments.requireReadable(uri);SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            long id=-1;try(Cursor c=db.rawQuery("SELECT id FROM files WHERE uri=?",new String[]{uri.toString()})){if(c.moveToFirst())id=c.getLong(0);}
            ContentValues row=new ContentValues();row.put("uri",uri.toString());row.put("name",name);row.put("mime",mime);row.put("viewed",System.currentTimeMillis());row.put("partial",partial || body.length()>MAX_TEXT?1:0);
            if(id<0)id=db.insertOrThrow("files",null,row);else{db.update("files",row,"id=?",new String[]{Long.toString(id)});db.delete("words","docid=?",new String[]{Long.toString(id)});}
            ContentValues words=new ContentValues();words.put("docid",id);words.put("name",name);words.put("body",body.substring(0,Math.min(body.length(),MAX_TEXT)));db.insertOrThrow("words",null,words);
            db.execSQL("DELETE FROM words WHERE docid IN (SELECT id FROM files ORDER BY viewed DESC,id DESC LIMIT -1 OFFSET "+MAX_FILES+")");
            db.execSQL("DELETE FROM files WHERE id IN (SELECT id FROM files ORDER BY viewed DESC,id DESC LIMIT -1 OFFSET "+MAX_FILES+")");db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public List<Entry> search(String query){
        List<Entry> result=new ArrayList<>();StringBuilder match=new StringBuilder();java.util.regex.Matcher tokens=java.util.regex.Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}]*").matcher(query.substring(0,Math.min(512,query.length())));
        while(tokens.find()){if(match.length()>0)match.append(" AND ");match.append('"').append(tokens.group()).append('*').append('"');}
        boolean literal=match.length()==0 && !query.trim().isEmpty();String like="%"+query.substring(0,Math.min(512,query.length())).replace("^","^^").replace("%","^%").replace("_","^_")+"%";
        String selection=literal?" WHERE id IN (SELECT docid FROM words WHERE name LIKE ? ESCAPE '^' OR body LIKE ? ESCAPE '^')":match.length()==0?"":" WHERE id IN (SELECT docid FROM words WHERE words MATCH ?)";
        String[] arguments=literal?new String[]{like,like}:match.length()==0?null:new String[]{match.toString()};
        try(Cursor c=getReadableDatabase().rawQuery("SELECT uri,name,mime,viewed,partial FROM files"+selection+" ORDER BY viewed DESC,id DESC LIMIT 32",arguments)){while(c.moveToNext())result.add(new Entry(c));}
        return result;
    }
    public void clearIndex(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("words",null,null);db.delete("files",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
}
