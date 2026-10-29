package app.space.notes.domain;

import java.util.Arrays;

/** Immutable pixel-edge geometry: rotate clockwise, mirror horizontally, then crop.
 * Preview commands never resample pixels; only an explicit export rasterizes once. */
public final class ImageEditPlan {
 public final int sourceWidth,sourceHeight,turns,left,top,right,bottom;
 public final boolean mirrored;
 public ImageEditPlan(int width,int height){this(width,height,0,false,0,0,width,height);}
 private ImageEditPlan(int width,int height,int rotation,boolean mirror,int l,int t,int r,int b){
  if(width<1 || height<1 || width>1600 || height>1600 || rotation<0 || rotation>3)throw new IllegalArgumentException("Invalid image dimensions");
  int w=rotation%2==0?width:height,h=rotation%2==0?height:width;
  if(l<0 || t<0 || r>w || b>h || l>=r || t>=b)throw new IllegalArgumentException("Invalid crop bounds");
  sourceWidth=width;sourceHeight=height;turns=rotation;mirrored=mirror;left=l;top=t;right=r;bottom=b;
 }
 public int width(){return turns%2==0?sourceWidth:sourceHeight;}
 public int height(){return turns%2==0?sourceHeight:sourceWidth;}
 public int cropWidth(){return right-left;}
 public int cropHeight(){return bottom-top;}
 public boolean changed(){return !equals(new ImageEditPlan(sourceWidth,sourceHeight));}
 public ImageEditPlan crop(int l,int t,int r,int b){return new ImageEditPlan(sourceWidth,sourceHeight,turns,mirrored,l,t,r,b);}
 public ImageEditPlan fullCrop(){return crop(0,0,width(),height());}
 public ImageEditPlan mirror(){return new ImageEditPlan(sourceWidth,sourceHeight,turns,!mirrored,width()-right,top,width()-left,bottom);}
 public ImageEditPlan rotateRight(){
  // A screen-space rotation reverses direction before an existing mirror.
  int next=(turns+(mirrored?3:1))%4;
  return new ImageEditPlan(sourceWidth,sourceHeight,next,mirrored,height()-bottom,left,height()-top,right);
 }
 public ImageEditPlan rotateLeft(){return rotateRight().rotateRight().rotateRight();}
 public ImageEditPlan aspect(int numerator,int denominator){
  if(numerator<1 || denominator<1 || numerator>100 || denominator>100)throw new IllegalArgumentException("Invalid aspect ratio");
  int w=width(),h=height();
  if((long)w*denominator>(long)h*numerator)w=Math.max(1,h*numerator/denominator);else h=Math.max(1,w*denominator/numerator);
  int x=Math.max(0,Math.min(width()-w,(left+right-w)/2)),y=Math.max(0,Math.min(height()-h,(top+bottom-h)/2));
  return crop(x,y,x+w,y+h);
 }
 /** Six affine coefficients mapping source pixel edges into the oriented image. */
 public float[] matrix(){
  float a,b,c,d,e,f;
  switch(turns){
   case 1:a=0;b=-1;c=sourceHeight;d=1;e=0;f=0;break;
   case 2:a=-1;b=0;c=sourceWidth;d=0;e=-1;f=sourceHeight;break;
   case 3:a=0;b=1;c=0;d=-1;e=0;f=sourceWidth;break;
   default:a=1;b=0;c=0;d=0;e=1;f=0;
  }
  if(mirrored){a=-a;b=-b;c=width()-c;}
  return new float[]{a,b,c,d,e,f};
 }
 public int[] state(){return new int[]{sourceWidth,sourceHeight,turns,mirrored?1:0,left,top,right,bottom};}
 public static ImageEditPlan restore(int[] state){
  if(state==null || state.length!=8 || (state[3]!=0 && state[3]!=1))throw new IllegalArgumentException("Invalid image edit state");
  return new ImageEditPlan(state[0],state[1],state[2],state[3]==1,state[4],state[5],state[6],state[7]);
 }
 @Override public boolean equals(Object other){return other instanceof ImageEditPlan && Arrays.equals(state(),((ImageEditPlan)other).state());}
 @Override public int hashCode(){return Arrays.hashCode(state());}
}
