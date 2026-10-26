/* Simple TTS MBROLA adapter, AGPL-3.0-or-later.
 * English analysis: Flite. Arabic: original ArabicFrontend. Voice data retain
 * their exact separate gratis-redistribution terms, not the application's AGPL. */
package org.retro.flitetts;
import android.content.Context;
import java.io.*;
import android.speech.tts.TextToSpeech;
import java.util.*;
public final class MbrolaPlugin implements EnginePlugin {
 private static final String[] IDS={"en1","ar1","ar2","us1","us2","us3"};
 private static final String[] LABELS={"EN1 — إنجليزي بريطاني","AR1 — عربي، نوفل التونسي، 1998","AR2 — عربي، SEDCO، 1998","US1 — إنجليزي أمريكي، Babel","US2 — إنجليزي أمريكي، Babel","US3 — إنجليزي أمريكي، OGI"};
 private volatile boolean ready;
 public String id(){return "mbrola";}
 public String label(){return "MBROLA — العربية والإنجليزية البريطانية والأمريكية";}
 public Locale locale(){return Locale.UK;}
 public Locale voiceLocale(int voice){return voice==1||voice==2?new Locale("ar"):voice>=3?Locale.US:Locale.UK;}
 public String[] voiceIds(){return IDS.clone();}
 public String[] voiceLabels(){return LABELS.clone();}
 public String sampleText(){return "Hello. This is MBROLA using the bundled Flite phoneme frontend.";}
 public String sampleText(int voice){return voice==1||voice==2?"مَرْحَبًا. هَذَا صَوْتٌ عَرَبِيٌّ.":voice>=3?"Hello. This is an American English MBROLA voice. Welcome to Simple TTS.":sampleText();}
 public String[] noticeAssets(){ArrayList<String> a=new ArrayList<>();a.add("mbrola/licenses/MBROLA-AGPL-3.0.txt");a.add("mbrola/licenses/MBROLA-VOICES-LICENSE.md");a.add("mbrola/licenses/EN1-README.txt");for(String id:new String[]{"AR1","AR2","US1","US2","US3"}){a.add("mbrola/licenses/"+id+"-LICENSE.txt");a.add("mbrola/licenses/"+id+"-README.txt");}a.add("mbrola/ARABIC-FRONTEND-NOTICE.txt");return a.toArray(new String[0]);}
 public String languageNotice(){return "MBROLA: AR1 وAR2 صوتان عربيان من 1998 (في التطبيق الأساسي)؛ EN1 بريطاني وUS1 وUS2 وUS3 أمريكية (حزمة mbrola-english). الإنجليزية بواجهة Flite مع تحويل فونيمات أمريكي مستقل. العربية بقواعد محلية، وأفضل مع التشكيل؛ غير المشكول والأسماء تقديرية وقد تخطئ. توزيع قواعد الأصوات مجاني فقط مع الإشعارات؛ البيع داخل منتج يحتاج إذن أصحاب الحقوق. جرّب قبل استخدامها لقارئ الشاشة.";}
 public String requiredPack(int voice){return voice==1||voice==2?null:"mbrola-english";}
 public void initialize(Context context){
  if(ready)return;synchronized(this){if(ready)return;
   try{
    File dir=new File(context.getFilesDir(),"mbrola-v120");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create MBROLA directory");
    for(String id:new String[]{"ar1","ar2"}){File out=new File(dir,id);if(!out.isFile()){
     File temp=new File(dir,id+".tmp");try(InputStream in=context.getAssets().open("mbrola/"+id);FileOutputStream f=new FileOutputStream(temp)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)f.write(b,0,n);}if(!temp.renameTo(out))throw new IOException("Cannot install "+id);
    }}
    if(!NativeMbrola.initialize(dir.getAbsolutePath(),context.getAssets()))throw new IllegalStateException("MBROLA databases failed to initialize");ready=true;
   }catch(IOException e){throw new IllegalStateException("Cannot install MBROLA databases",e);}
  }
 }
 public int languageAvailability(String lang,String country,String variant){
  if("ara".equalsIgnoreCase(lang)||"ar".equalsIgnoreCase(lang))return TextToSpeech.LANG_AVAILABLE;
  if(!"eng".equalsIgnoreCase(lang)&&!"en".equalsIgnoreCase(lang))return TextToSpeech.LANG_NOT_SUPPORTED;
  return "GBR".equalsIgnoreCase(country)||"GB".equalsIgnoreCase(country)||"USA".equalsIgnoreCase(country)||"US".equalsIgnoreCase(country)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
 }
 public boolean acceptsText(String text){return acceptsText(text,0)||acceptsText(text,1);}
 public boolean acceptsText(String text,int voice){
  if(voice==1||voice==2)return ArabicFrontend.accepts(text);
  if(text==null||text.trim().isEmpty())return false;for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}return true;
 }
 public int sampleRate(int voice){return voice==2?22050:16000;}
 public short[] synthesize(String text,int voice,float rate,float pitch){return voice==1||voice==2?NativeMbrola.renderPhones(ArabicFrontend.commands(text,rate,pitch),voice):NativeMbrola.render(text,voice,rate,pitch);}
}
final class NativeMbrola {
 static {System.loadLibrary("mbrolaretro");}
 static synchronized native boolean initialize(String filesDir,android.content.res.AssetManager assets);
 static synchronized native short[] render(String text,int voice,float rate,float pitch);
 static synchronized native short[] renderPhones(String commands,int voice);
}
