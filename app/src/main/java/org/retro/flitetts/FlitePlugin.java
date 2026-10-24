/* Simple TTS Flite plugin. Upstream engine/data remain unmodified. */
package org.retro.flitetts;
import java.util.Locale;
import java.io.*;
import android.speech.tts.TextToSpeech;
import android.content.Context;

public final class FlitePlugin implements EnginePlugin {
 private static final String[] IDS={"kal","kal16","awb","rms","slt","bdl","clb","jmk","ksp","asc","aew","ahw","aup","axb","eey","fem","gka","ljm","lnh","rxr","slp","clartts"};
 private static final String[] LABELS={"KAL — صوت رجالي آلي قديم، 8 كيلوهرتز","KAL16 — صوت رجالي آلي قديم، 16 كيلوهرتز","AWB — صوت رجالي بلكنة اسكتلندية","RMS — صوت رجالي أمريكي","SLT — صوت نسائي أمريكي","BDL — صوت رجالي أمريكي","CLB — صوت نسائي أمريكي","JMK — صوت رجالي بإنجليزية كندية","KSP — صوت رجالي بإنجليزية هندية","ASC — صوت عربي حقيقي (Arabic Speech Corpus)","AEW — صوت رجالي أمريكي (CMU ARCTIC)","AHW — صوت رجالي ألماني-اللكنة (CMU ARCTIC)","AUP — صوت رجالي هندي-اللكنة (CMU ARCTIC)","AXB — صوت نسائي هندي-اللكنة (CMU ARCTIC)","EEY — صوت نسائي أمريكي (CMU ARCTIC)","FEM — صوت رجالي ألماني-اللكنة (CMU ARCTIC)","GKA — صوت رجالي هندي-اللكنة (CMU ARCTIC)","LJM — صوت نسائي أمريكي (CMU ARCTIC)","LNH — صوت نسائي أمريكي (CMU ARCTIC)","RXR — صوت رجالي إسرائيلي-اللكنة (CMU ARCTIC)","SLP — صوت نسائي هندي-اللكنة (CMU ARCTIC)","ClArTTS — صوت عربي فصيح مدرَّب على ClArTTS (CC BY 4.0)"};
 private static final String[] EXTRA={"cmu_us_bdl.flitevox","cmu_us_clb.flitevox","cmu_us_jmk.flitevox","cmu_us_ksp.flitevox","kacst_ar_asc.flitevox","cmu_us_aew.flitevox","cmu_us_ahw.flitevox","cmu_us_aup.flitevox","cmu_us_axb.flitevox","cmu_us_eey.flitevox","cmu_us_fem.flitevox","cmu_us_gka.flitevox","cmu_us_ljm.flitevox","cmu_us_lnh.flitevox","cmu_us_rxr.flitevox","cmu_us_slp.flitevox","clartts_ar.flitevox"};
 private static final long CLARTTS_SIZE=3184192L;
 private static final long[] SIZES={10283514L,12538083L,10401617L,11765452L,4534171L,12640161L,6949326L,5701990L,7260569L,6092398L,6193447L,6902785L,5688553L,11903497L,7645602L,7280743L,CLARTTS_SIZE};
 private static boolean isArabic(int voice){return voice==9||voice==21;}
 private volatile Context context;
 private File directory;
 public String id(){return "flite";}
 public String label(){return "Flite — محرك كلاسيكي مفتوح المصدر";}
 public Locale locale(){return Locale.US;}
 public String[] voiceIds(){return IDS.clone();}
 public String[] voiceLabels(){return LABELS.clone();}
 public String sampleText(){return "Hello. This is the classic Flite voice inside Simple TTS.";}
 public String sampleText(int voice){return isArabic(voice)?"مَرْحَبًا بِكُمْ. هَذِهِ تَجْرِبَةٌ لِلصَّوْتِ الْعَرَبِيِّ الْقَدِيمِ.":sampleText();}
 public String[] noticeAssets(){return new String[]{"FLITE-COPYING.txt","FLITE-ACKNOWLEDGEMENTS.txt","SOURCE-NOTICES.txt","flite-extra/EXTRA-VOICES-NOTICE.txt","asc/asc-MIT-LICENSE.txt","asc/asc-CORPUS-CC-BY-4.txt","clartts/CLARTTS-NOTICE.txt"};}
 public String languageNotice(){return "Flite: عشرون صوتًا إنجليزيًا محليًا وصوتان عربيان (ASC وClArTTS). أضيف في هذا الإصدار متحدثو CMU ARCTIC: AXB وEEY وFEM وGKA وLJM وLNH وRXR وSLP. تُستخرج النماذج الإضافية عند اختيارها لأول مرة.";}
 public void initialize(Context c){
  if(context!=null)return;
  synchronized(this){if(context!=null)return;
   File dir=new File(c.getFilesDir(),"flite-extra");
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Cannot create private Flite model directory");
   directory=dir;Context app=c.getApplicationContext();context=app==null?c:app;
  }
 }
 private void ensureVoice(int voice){
  if(voice<0||voice>=IDS.length)throw new IllegalArgumentException("Unknown Flite voice");
  if(voice<5)return;
  synchronized(this){
   if(context==null)throw new IllegalStateException("Flite must be initialized with an Android context");
   int i=voice-5;File out=new File(directory,EXTRA[i]);
   if(!out.isFile()||out.length()!=SIZES[i]){
    File tmp=new File(directory,EXTRA[i]+".tmp");
    try(InputStream in=context.getAssets().open((i==4?"asc/":"flite-extra/")+EXTRA[i]);FileOutputStream fos=new FileOutputStream(tmp)){
     byte[] b=new byte[32768];int n;long copied=0;while((n=in.read(b))!=-1){fos.write(b,0,n);copied+=n;}
     fos.getFD().sync();if(copied!=SIZES[i])throw new IOException("Incomplete Flite model "+EXTRA[i]);
     if(!tmp.renameTo(out))throw new IOException("Cannot commit Flite model "+EXTRA[i]);
    }catch(IOException failure){tmp.delete();throw new IllegalStateException("Cannot extract Flite model "+EXTRA[i],failure);}
   }
   if(!NativeBridge.loadExtraVoices(directory.getAbsolutePath()))throw new IllegalStateException("Flite model path not registered");
  }
 }
 public Locale voiceLocale(int voice){if(isArabic(voice))return new Locale("ar","SA");if(voice==2)return Locale.UK;if(voice==7)return Locale.CANADA;if(voice==8||voice==12||voice==13||voice==16||voice==20)return new Locale("en","IN");return Locale.US;}
 public int languageAvailability(String lang,String country,String variant){
  if("ara".equalsIgnoreCase(lang)||"ar".equalsIgnoreCase(lang))return "SAU".equalsIgnoreCase(country)||"SA".equalsIgnoreCase(country)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
  if(!"eng".equalsIgnoreCase(lang)&&!"en".equalsIgnoreCase(lang))return TextToSpeech.LANG_NOT_SUPPORTED;
  return "USA".equalsIgnoreCase(country)||"US".equalsIgnoreCase(country)||"GBR".equalsIgnoreCase(country)||"GB".equalsIgnoreCase(country)||"CAN".equalsIgnoreCase(country)||"CA".equalsIgnoreCase(country)||"IND".equalsIgnoreCase(country)||"IN".equalsIgnoreCase(country)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
 }
 public boolean acceptsText(String text){return acceptsText(text,0)||acceptsText(text,9)||acceptsText(text,21);}
 public boolean acceptsText(String text,int voice){if(isArabic(voice))return ArabicFrontend.accepts(text);if(text==null||text.trim().isEmpty())return false;for(int at=0;at<text.length();){int cp=text.codePointAt(at);at+=Character.charCount(cp);if(Character.isISOControl(cp)&&cp!='\n'&&cp!='\r'&&cp!='\t')return false;if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}return true;}
 public int sampleRate(int voice){ensureVoice(voice);int hz=NativeBridge.sampleRate(voice);if(hz<=0)throw new IllegalStateException("Flite model failed to load");return hz;}
 public short[] synthesize(String text,int voice,float rate,float pitch){ensureVoice(voice);return NativeBridge.render(text,voice,rate,pitch);}
}
