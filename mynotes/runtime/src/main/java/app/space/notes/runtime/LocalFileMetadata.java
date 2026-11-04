package app.space.notes.runtime;

import android.content.Context;
import android.net.Uri;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;

/** Original-provider metadata only; no renderer-cache path or explanatory help in file details. */
final class LocalFileMetadata {
    static void show(Context context,NotesDesign design,ExecutorService worker,Uri uri,String name,String mime,long fallbackBytes,String counts,BooleanSupplier closed){
        LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(design.dp(16),design.dp(12),design.dp(16),design.dp(12));ProgressBar progress=new ProgressBar(context);body.addView(progress,new LinearLayout.LayoutParams(design.dp(40),design.dp(40)));TextView info=design.label("Reading file details…",16,design.ink);info.setTextIsSelectable(true);body.addView(info);ScrollView scroll=new ScrollView(context);scroll.addView(body);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("File details").setView(scroll).setPositiveButton("Close",null).create();design.show(dialog);
        worker.execute(()->{String value=read(context,uri,name,mime,fallbackBytes,counts);new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(!closed.getAsBoolean() && dialog.isShowing()){progress.setVisibility(android.view.View.GONE);info.setText(value);}});});
    }
    static String read(Context context,Uri uri,String name,String mime,long fallbackBytes,String counts){
        String size=fallbackBytes<0?"Unavailable":android.text.format.Formatter.formatFileSize(context,fallbackBytes),modified="Unavailable",location=uri==null?"Unavailable":uri.toString();
        if(uri!=null)try(android.database.Cursor cursor=context.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.SIZE,android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED},null,null,null)){if(cursor!=null && cursor.moveToFirst()){int n=cursor.getColumnIndex(android.provider.OpenableColumns.SIZE),m=cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED);if(n>=0 && !cursor.isNull(n) && cursor.getLong(n)>=0)size=android.text.format.Formatter.formatFileSize(context,cursor.getLong(n));if(m>=0 && !cursor.isNull(m) && cursor.getLong(m)>0)modified=java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(cursor.getLong(m)));}}catch(RuntimeException unsupported){/* Absent provider fields are explicitly unavailable, never invented. */}
        if(uri!=null)try{if(android.provider.DocumentsContract.isDocumentUri(context,uri))location=android.provider.DocumentsContract.getDocumentId(uri)+"\nURI: "+uri;}catch(RuntimeException unsupported){}
        return "Name: "+name+"\nLocation: "+location+"\nType: "+mime+"\nSize: "+size+"\nModified: "+modified+(counts.isEmpty()?"":"\n"+counts);
    }
}
