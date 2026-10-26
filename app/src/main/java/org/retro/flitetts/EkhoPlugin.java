/* Simple TTS Cantonese-only adapter, AGPL-3.0-or-later. See bundled upstream licenses. */
package org.retro.flitetts;
import android.content.Context;
import android.content.res.AssetManager;
import android.speech.tts.TextToSpeech;
import java.io.*;
import java.util.Locale;

/** Actual Ekho syllable concatenation with separately licensed CantoneseWong data. */
public final class EkhoPlugin implements EnginePlugin {
 static {System.loadLibrary("ekho");}
 private static boolean ready;
 private static final Object LOCK=new Object();
 private static void checkVoice(int voice){if(voice!=0)throw new IllegalArgumentException("Invalid Ekho voice");}
 public String requiredPack(int voice){return "ekho-cantonese";}
 public String id(){return "ekho";}
 public String label(){return "Ekho — CantoneseWong";}
 public Locale locale(){return Locale.forLanguageTag("yue-HK");}
 public String[] voiceIds(){return new String[]{"yue-HK-cantonese-wong"};}
 public String[] voiceLabels(){return new String[]{"CantoneseWong — Hong Kong Cantonese"};}
 public String sampleText(){return "你好，多謝你使用粵語離線語音合成。";}
 public String[] noticeAssets(){return new String[]{"ekho/licenses/EKHO-NOTICE.txt","ekho/licenses/EKHO-COPYING.txt","ekho/licenses/COPYING-jyutping-wong","ekho/licenses/DICTIONARY-NOTICE.txt","ekho/licenses/CC-BY-SA-3.0.txt","ekho/licenses/SONIC-LGPL-2.1.txt","ekho/licenses/LIBSNDFILE-LGPL-2.1.txt","ekho/licenses/GSM-COPYRIGHT.txt","ekho/licenses/SR-CONVERT-NOTICE.txt","ekho/licenses/UTFCPP-NOTICE.txt"};}
 public String languageNotice(){return "Ekho Cantonese only; CantoneseWong 16 kHz syllable recordings. No English, Mandarin, neural or network fallback. Core GPL-2.0-or-later; separate voice GPL-2.0; dictionary CC BY-SA 3.0.";}
 public int languageAvailability(String l,String c,String v){if(!"yue".equalsIgnoreCase(l))return TextToSpeech.LANG_NOT_SUPPORTED;return "HK".equalsIgnoreCase(c)||"HKG".equalsIgnoreCase(c)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;}
 public boolean acceptsText(String text){
  if(text==null||text.trim().isEmpty())return false;
  for(int at=0;at<text.length();){int cp=text.codePointAt(at);at+=Character.charCount(cp);if(cp>0xffff||(Character.isISOControl(cp)&&cp!='\n'&&cp!='\r'&&cp!='\t'))return false;if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.HAN)return false;}
  return true;
 }
 public int sampleRate(int voice){checkVoice(voice);return 16000;}
 public void initialize(Context context){synchronized(LOCK){if(ready)return;File root=new File(context.getFilesDir(),"ekho-160"),marker=new File(root,"installed");try{
  if(!marker.isFile()||!PackStore.complete(context,"ekho-cantonese"))throw new PackStore.PackMissingException("ekho-cantonese");
  if(!nativeInit(root.getAbsolutePath()))throw new IOException("Ekho initialization failed");ready=true;
 }catch(IOException failure){throw new IllegalStateException("Cannot install Cantonese voice",failure);}}}
 public short[] synthesize(String text,int voice,float rate,float pitch){checkVoice(voice);if(!acceptsText(text)||!Float.isFinite(rate)||!Float.isFinite(pitch))throw new IllegalArgumentException("Ekho requires supported Cantonese text and finite parameters");synchronized(LOCK){if(!ready)throw new IllegalStateException("Ekho is not initialized");short[] pcm=nativeRender(text,Math.max(.25f,Math.min(4f,rate)),Math.max(.5f,Math.min(2f,pitch)));if(pcm==null||pcm.length==0)throw new IllegalStateException("Ekho returned no speech");return pcm;}}
 private static void copyTree(AssetManager assets,String asset,File out)throws IOException{
  String[] children=assets.list(asset);
  if(children==null||children.length==0){File parent=out.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create voice directory");try(InputStream in=assets.open(asset);FileOutputStream stream=new FileOutputStream(out)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)stream.write(b,0,n);}return;}
  if(!out.isDirectory()&&!out.mkdirs())throw new IOException("Cannot create voice directory");for(String child:children)copyTree(assets,asset+"/"+child,new File(out,child));
 }
 private static native boolean nativeInit(String path);
 private static native short[] nativeRender(String text,float rate,float pitch);
}
