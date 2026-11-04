package app.space.notes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.Assert.*;

/** Same-signer CI instrumentation, not shipped in or installed on a user's app.
 * Public generated data only. This probes repository retention, not physical UI.
 */
@RunWith(AndroidJUnit4.class)
public class ReleaseRichMigrationProbe {
    private static final String TOKEN="public-rich-release64-to66-v1";
    private String hash(byte[] bytes)throws Exception{
        StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    private NoteImage image(String id,String name,int color)throws Exception{
        Bitmap bitmap=Bitmap.createBitmap(12,12,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color);
        ByteArrayOutputStream out=new ByteArrayOutputStream();assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));bitmap.recycle();
        return new NoteImage(id,name,"image/png",12,12,Base64.getEncoder().encodeToString(out.toByteArray()));
    }
    private List<Note> fixtures()throws Exception {
        long now=System.currentTimeMillis();
        NoteImage photo=image("11111111-1111-4111-8111-111111111111","Public image",Color.BLUE);
        NoteImage ink=image("22222222-2222-4222-8222-222222222222","Public drawing",Color.BLACK);
        List<Note.Mark> marks=Arrays.asList(new Note.Mark("bold",0,4),new Note.Mark("link",0,4,"https://example.com/public"));
        Note rich=Note.create(now).title("PublicRichLegacy",now).rich("PublicRichLegacy","Link ಕನ್ನಡ भारत 😀",marks,now)
            .properties(2,Arrays.asList("Public","Mixed script"),now).folder("PublicUpgrade",now).background(3,now).state(true,false,0,now)
            .images(Arrays.asList(photo),now).namedContent("image:"+photo.id,"Retained public image",now)
            .voiceSource("33333333-3333-4333-8333-333333333333",now).reminder(now+86400000,now).namedContent("reminder","Public reminder",now);
        Note checklist=Note.create(now).list("PublicLegacyChecklist",Arrays.asList(new Note.Item("legacy-row-a","Checked ಕನ್ನಡ",true,2),new Note.Item("legacy-row-b","Unchecked भारत",false,0)),true,now).folder("PublicUpgrade",now).state(false,true,0,now);
        TextTable table=TextTable.empty(2,2).withCell(0,0,"Header ಕನ್ನಡ").withCell(1,1,"Cell भारत 😀");
        Diagram diagram=new Diagram(Arrays.asList(new Diagram.Node("public-a","process","Start",20,20,100,60,0),new Diagram.Node("public-b","decision","Next",20,150,100,70,0)),Arrays.asList(new Diagram.Edge("public-edge","public-a","public-b","next →")));
        List<DocumentContent.Block> blocks=Arrays.asList(
            DocumentContent.Block.text("public-text",new RichText("Link ಕನ್ನಡ भारत 😀",marks)),
            DocumentContent.Block.checklist("public-list","Public checklist",Arrays.asList(new Note.Item("canonical-row","Nested checked",true,3,Arrays.asList(new Note.Mark("italic",0,6)))),true),
            DocumentContent.Block.table("public-table","Public table",table),
            DocumentContent.Block.visual("public-image","Public image",DocumentContent.Kind.IMAGE,photo),
            DocumentContent.Block.visual("public-drawing","Public drawing",DocumentContent.Kind.DRAWING,ink),
            DocumentContent.Block.diagram("public-diagram","Public diagram",diagram));
        Note doc=Note.create(now).title("PublicCanonicalDoc",now).withContent(new DocumentContent(blocks,true),now).folder("PublicUpgrade",now).properties(1,Arrays.asList("PublicDoc"),now);
        Note trash=Note.create(now).edit("PublicTrash","Retained trash fixture",now).state(false,false,now,now);
        return Arrays.asList(rich,checklist,doc,trash);
    }
    @Test public void publicRichLibraryRetainsItsOldWriterSnapshot()throws Exception {
        Bundle args=InstrumentationRegistry.getArguments();assertEquals(TOKEN,args.getString("explicitPublicFixtureOnly"));
        assertTrue("Disposable emulator only",Arrays.asList("ranchu","goldfish").contains(Build.HARDWARE));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertEquals("app.space.notes",context.getPackageName());
        String phase=args.getString("richPhase");assertTrue(Arrays.asList("create","before","after").contains(phase));
        int version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionCode;
        assertEquals(Integer.parseInt(args.getString("expectedReleaseCode")),version);
        NotesRepository repository=new NotesRepository(context);File marker=new File(context.getFilesDir(),"ci-public-rich-old-snapshot.json");
        if("create".equals(phase)){
            assertEquals(64,version);assertFalse("Never overwrite an earlier checkpoint",marker.exists());
            assertTrue("Never seed a populated library",repository.loadForStartup().isEmpty());assertTrue(repository.appOnlyStorage());
            List<Note> notes=fixtures();repository.createFolder("PublicUpgrade",Collections.emptyList());repository.save(notes);
            Files.write(marker.toPath(),DocumentCodec.encode(notes));
        }
        assertTrue(marker.isFile());byte[] expected=Files.readAllBytes(marker.toPath());assertTrue(expected.length>0&&expected.length<100000);
        List<Note> actual=repository.loadForStartup();assertEquals(4,actual.size());
        assertArrayEquals("Entire old-writer payload, including every embedded pixel, style and ID",expected,DocumentCodec.encode(actual));
        assertEquals(6,actual.get(2).content.blocks.size());assertEquals(1,actual.get(0).images.size());
        assertTrue(actual.get(1).items.get(0).checked);assertEquals(2,actual.get(1).items.get(0).depth);
        assertTrue(repository.listPrivateFolders(actual).contains("PublicUpgrade"));
        // Verify the candidate can write its own repository snapshot and cold-read it,
        // not just decode an untouched old file. Neither uninstall nor data clearing.
        if("after".equals(phase)){assertEquals(66,version);repository.save(actual);assertArrayEquals(expected,DocumentCodec.encode(new NotesRepository(context).loadForStartup()));}
        JSONObject proof=new JSONObject().put("phase",phase).put("version_code",version).put("notes",4).put("old_snapshot_sha256",hash(expected))
            .put("payload_unchanged",true).put("private_folder_retained",true).put("candidate_write_readback", "after".equals(phase))
            .put("physical_acceptance",false).put("ui_rendering_acceptance",false).put("scope","Public repository-generated legacy/rich/checklist/canonical Doc/image/drawing/table/diagram/label/folder/trash/reminder/voice-source metadata; not real recordings, SAF or exhaustive user-library acceptance");
        Bundle status=new Bundle();status.putString("stream","PUBLIC_RICH_UPGRADE_PROOF="+proof.toString()+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(2,status);
    }
}
