package app.space.core.design;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import app.space.core.data.AppTheme;

/**
 * A soft, modern gel-glass surface drawn from light, tint and depth rather than a
 * hard outline.
 *
 * <p>Moved into {@code :core-design} in step 1c-2b (SUITE-PLAN.md §6.1) with the rest
 * of the glass system. It deliberately stays <b>package-private</b>: only
 * {@link AppSurfaceStyle} constructs it, and callers receive it typed as
 * {@code Drawable}. Keeping the constructor closed is what guarantees every glass
 * surface goes through the opacity floor enforced below, which is the documented
 * legibility guarantee (README.md: glass stays behind text, critical actions remain
 * solid). A public constructor would let a caller bypass it.
 */
final class AppGlassDrawable extends Drawable {
    private final Context context;
    private final int base, highlight, cornerPx, opacity, finish, depth, preset, opacityFloor;
    private final boolean dark;
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
    private final Paint bloom=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
    private final Paint reflection=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
    private final Paint rim=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
    private final Paint lower=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
    private final Paint bubble=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);
    private int drawableAlpha=255;
    private ColorFilter colorFilter;

    AppGlassDrawable(Context context,int base,int radiusDp,int opacity,int finish,int depth,int preset,int opacityFloor){
        this.context=context.getApplicationContext();
        this.base=base;
        this.highlight=GlassPreset.glow(preset);
        this.dark=AppTheme.dark(context);
        this.cornerPx=dp(radiusDp*UiKit.cornerFactor(UiKit.cornerStyle(context)));
        this.opacityFloor=Math.max(82,opacityFloor);
        this.opacity=clamp(Math.max(82,opacityFloor),100,Math.max(Math.max(82,opacityFloor),Math.max(0,Math.min(100,opacity))));
        this.finish=Math.max(0,Math.min(100,finish));
        this.depth=Math.max(0,Math.min(100,depth));
        this.preset=GlassPreset.clamp(preset);
        fill.setStyle(Paint.Style.FILL);
        bloom.setStyle(Paint.Style.FILL);
        reflection.setStyle(Paint.Style.FILL);
        rim.setStyle(Paint.Style.STROKE);
        lower.setStyle(Paint.Style.STROKE);
    }

    private int dp(float value){return (int)(value*context.getResources().getDisplayMetrics().density+.5f);}
    private static int clamp(int min,int max,int value){return Math.max(min,Math.min(max,value));}
    private int mix(int first,int second,float amount){float f=Math.max(0f,Math.min(1f,amount));return Color.rgb(Math.round(Color.red(first)+(Color.red(second)-Color.red(first))*f),Math.round(Color.green(first)+(Color.green(second)-Color.green(first))*f),Math.round(Color.blue(first)+(Color.blue(second)-Color.blue(first))*f));}
    private int alpha(int color,float amount){int a=Math.round(clamp(0,255,Math.round(amount))*drawableAlpha/255f);return (color&0x00ffffff)|(a<<24);}
    private int scaledAlpha(float amount){return Math.round(clamp(0,255,Math.round(amount))*drawableAlpha/255f);}

    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();
        if(bounds.isEmpty())return;
        RectF rect=new RectF(bounds.left,bounds.top,bounds.right,bounds.bottom);
        float radius=Math.min(cornerPx,Math.min(rect.width(),rect.height())*.5f);
        float gloss=finish/100f,relief=depth/100f;
        float characterGlow=preset==2?1.18f:preset==3?.76f:preset==1?.84f:1f;
        float characterDepth=preset==2?1.12f:preset==3?.82f:preset==4?.92f:1f;
        int pearl=dark?0xffe8fff8:Color.WHITE;
        int shadow=dark?0xff08120f:0xff657a72;
        int tinted=mix(base,highlight,preset==2?.13f:preset==3?.09f:.06f);
        int surfaceAlpha=scaledAlpha(255f*opacity/100f);

        Path silhouette=new Path();
        silhouette.addRoundRect(rect,radius,radius,Path.Direction.CW);
        canvas.save();
        canvas.clipPath(silhouette);

        int top=alpha(mix(tinted,pearl,.09f+.13f*gloss*characterGlow),surfaceAlpha+scaledAlpha(9));
        int middle=alpha(tinted,surfaceAlpha);
        int bottom=alpha(mix(tinted,shadow,.07f+.14f*relief*characterDepth),Math.max(scaledAlpha(142f*opacityFloor/100f),surfaceAlpha-scaledAlpha(16)));
        fill.setShader(new LinearGradient(rect.left,rect.top,rect.left,rect.bottom,new int[]{top,middle,bottom},new float[]{0f,.56f,1f},Shader.TileMode.CLAMP));
        fill.setColorFilter(colorFilter);
        canvas.drawRoundRect(rect,radius,radius,fill);

        float glowRadius=Math.max(rect.width(),rect.height())*(.52f+.08f*characterGlow);
        int bloomAlpha=scaledAlpha((preset==4?24f:30f)+gloss*24f);
        bloom.setShader(new RadialGradient(rect.left+rect.width()*(preset==3?.22f:.78f),rect.top+rect.height()*.05f,glowRadius,new int[]{alpha(mix(highlight,pearl,.28f),bloomAlpha),alpha(highlight,scaledAlpha(bloomAlpha*.32f)),alpha(highlight,0)},new float[]{0f,.44f,1f},Shader.TileMode.CLAMP));
        bloom.setColorFilter(colorFilter);
        canvas.drawRect(rect,bloom);
        float beadRadius=Math.max(dp(7),Math.min(rect.width(),rect.height())*.12f);
        bubble.setShader(new RadialGradient(rect.left+rect.width()*.2f,rect.top+rect.height()*.19f,beadRadius,new int[]{alpha(pearl,scaledAlpha(42f+gloss*26f)),alpha(highlight,scaledAlpha(16f+gloss*12f)),alpha(highlight,0)},new float[]{0f,.42f,1f},Shader.TileMode.CLAMP));
        bubble.setColorFilter(colorFilter);
        canvas.drawCircle(rect.left+rect.width()*.2f,rect.top+rect.height()*.19f,beadRadius,bubble);

        // A broad, feathered reflection gives the surface a fresh gel sheen without a hard stripe.
        float bandHeight=Math.max(dp(12),rect.height()*(.19f+.045f*gloss));
        Path softBand=new Path();
        softBand.moveTo(rect.left+radius*.6f,rect.top-dp(2));
        softBand.cubicTo(rect.left+rect.width()*.24f,rect.top-dp(3),rect.left+rect.width()*.71f,rect.top+bandHeight*.02f,rect.right-radius*.55f,rect.top+bandHeight*.24f);
        softBand.cubicTo(rect.right-radius*.27f,rect.top+bandHeight*.32f,rect.right-radius*.2f,rect.top+bandHeight*.55f,rect.right-radius*.28f,rect.top+bandHeight*.68f);
        softBand.cubicTo(rect.left+rect.width()*.67f,rect.top+bandHeight*.43f,rect.left+rect.width()*.27f,rect.top+bandHeight*.34f,rect.left+radius*.6f,rect.top+bandHeight*.3f);
        softBand.close();
        int reflectionAlpha=scaledAlpha((10f+gloss*34f)*(preset==0||preset==2?1.10f:1f));
        reflection.setShader(new LinearGradient(rect.left,rect.top,rect.left,rect.top+bandHeight,new int[]{alpha(pearl,reflectionAlpha),alpha(mix(pearl,highlight,.13f),scaledAlpha(reflectionAlpha*.48f)),alpha(pearl,0)},new float[]{0f,.52f,1f},Shader.TileMode.CLAMP));
        reflection.setColorFilter(colorFilter);
        canvas.drawPath(softBand,reflection);
        canvas.restore();

        RectF edge=new RectF(rect.left+dp(.6f),rect.top+dp(.6f),rect.right-dp(.6f),rect.bottom-dp(.6f));
        float edgeRadius=Math.max(0,radius-dp(.6f));
        int upperRim=scaledAlpha(15f+gloss*24f+relief*8f);
        int lowerRim=scaledAlpha(12f+gloss*9f+relief*(preset==2?25f:13f));
        rim.setStrokeWidth(dp(.85f));
        rim.setShader(new LinearGradient(0,rect.top,0,rect.bottom,new int[]{alpha(pearl,upperRim),alpha(mix(pearl,highlight,.2f),scaledAlpha(16f+gloss*9f)),alpha(highlight,lowerRim)},new float[]{0f,.48f,1f},Shader.TileMode.CLAMP));
        rim.setColorFilter(colorFilter);
        canvas.drawRoundRect(edge,edgeRadius,edgeRadius,rim);

        if(depth>12){
            lower.setStrokeWidth(dp(.8f));
            lower.setColor(alpha(pearl,scaledAlpha((depth-12)*.13f)));
            lower.setColorFilter(colorFilter);
            Path contour=new Path();
            float y=rect.bottom-radius*.62f;
            contour.moveTo(rect.left+radius,y);
            contour.quadTo(rect.centerX(),rect.bottom-dp(1),rect.right-radius,y);
            canvas.drawPath(contour,lower);
        }
    }

    @Override public void setAlpha(int alpha){drawableAlpha=clamp(0,255,alpha);invalidateSelf();}
    @Override public int getAlpha(){return drawableAlpha;}
    @Override public void setColorFilter(ColorFilter filter){colorFilter=filter;invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
