/* Simple TTS Flite+HTS adapter. Upstream engines and voice data keep their own licences. */
package org.retro.flitetts;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.io.*;
import java.util.Locale;
public final class FliteHtsPlugin implements EnginePlugin {
 private static final String VOICE_FILE="cmu_us_arctic_slt.htsvoice";
 private static final long VOICE_SIZE=1515783L;
 private static volatile boolean ready;
 public String id(){return "flitehts";}
 public String label(){return "Flite + HTS — صوت إحصائي HMM أمريكي";}
 public Locale locale(){return Locale.US;}
 public String[] voiceIds(){return new String[]{"cmu-us-arctic-slt-hts"};}
 public String[] voiceLabels(){return new String[]{"SLT HTS — صوت نسائي أمريكي إحصائي HMM، 48 كيلوهرتز"};}
 public String sampleText(){return "Hello. This is the statistical HMM voice of Flite and H T S inside Simple TTS.";}
 public String[] noticeAssets(){return new String[]{"flitehts/FLITEHTS-NOTICE.txt","flitehts/licenses/FLITE-HTS-ENGINE-COPYING.txt","flitehts/licenses/FLITE-HTS-ENGINE-README.txt","flitehts/licenses/HTS-VOICE-SLT-CC-BY-3.0-README.txt","flitehts/licenses/CMU-ARCTIC-COPYING.txt","openjtalk/licenses/HTS-COPYING.txt","FLITE-COPYING.txt"};}
 public String languageNotice(){return "Flite + HTS: صوت أمريكي نسائي واحد بتقنية HMM الإحصائية (ليس شبكة عصبية)، يعمل دون اتصال. الإنجليزية فقط.";}
 public int languageAvailability(String l,String c,String v){
  if(!"eng".equalsIgnoreCase(l)&&!"en".equalsIgnoreCase(l))return TextToSpeech.LANG_NOT_SUPPORTED;
  return "USA".equalsIgnoreCase(c)||"US".equalsIgnoreCase(c)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
 }
 public boolean acceptsText(String text){
  if(text==null||text.trim().isEmpty()||text.length()>4096)return false;
  for(int at=0;at<text.length();){int cp=text.codePointAt(at);at+=Character.charCount(cp);
   if(Character.isISOControl(cp)&&cp!='\n'&&cp!='\r'&&cp!='\t')return false;
   if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}
  return true;
 }
 public synchronized void initialize(Context c){
  if(ready)return;
  File dir=new File(c.getFilesDir(),"flitehts");
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Cannot create Flite+HTS directory");
  File out=new File(dir,VOICE_FILE);
  if(!out.isFile()||out.length()!=VOICE_SIZE){
   File tmp=new File(dir,VOICE_FILE+".tmp");
   try(InputStream in=c.getAssets().open("flitehts/"+VOICE_FILE);FileOutputStream fos=new FileOutputStream(tmp)){
    byte[] b=new byte[32768];int n;long copied=0;while((n=in.read(b))!=-1){fos.write(b,0,n);copied+=n;}
    fos.getFD().sync();if(copied!=VOICE_SIZE)throw new IOException("Incomplete HTS voice");
    if(!tmp.renameTo(out))throw new IOException("Cannot commit HTS voice");
   }catch(IOException e){tmp.delete();throw new IllegalStateException("Cannot extract Flite+HTS voice",e);}
  }
  if(!nativeInit(out.getAbsolutePath()))throw new IllegalStateException("Flite+HTS initialization failed");
  ready=true;
 }
 public int sampleRate(int voice){if(!ready)return 48000;int hz=nativeSampleRate();return hz>0?hz:48000;}
 public synchronized short[] synthesize(String text,int voice,float rate,float pitch){
  if(!ready)throw new IllegalStateException("Flite+HTS is not initialized");
  if(voice!=0)throw new IllegalArgumentException("Invalid Flite+HTS voice");
  if(!acceptsText(text))throw new IllegalArgumentException("Flite+HTS accepts English text only");
  return nativeRender(text,rate,pitch);
 }
 private static native boolean nativeInit(String path);
 private static native int nativeSampleRate();
 private static native short[] nativeRender(String text,float rate,float pitch);
 static{System.loadLibrary("flitehts");}
}
