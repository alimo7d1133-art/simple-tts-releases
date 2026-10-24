/* Simple TTS Festival adapter, AGPL-3.0-or-later. Upstream notices are separate. */
package org.retro.flitetts;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.io.*;
import java.util.Locale;
import java.util.zip.*;

public final class FestivalPlugin implements EnginePlugin {
 private static final String DATA_VERSION="festival-160";
 private boolean ready;
 private static void checkVoice(int voice){if(voice<0||voice>1)throw new IllegalArgumentException("Invalid Festival voice");}
 public String id(){return "festival";}
 public String label(){return "Festival — Ziad Arabic / Kurt American English";}
 public Locale locale(){return new Locale("ar");}
 public String[] voiceIds(){return new String[]{"ara_norm_ziad_hts","ked_diphone"};}
 public String[] voiceLabels(){return new String[]{"Ziad — Arabic HTS","Kurt — American English LPC"};}
 public String sampleText(){return sampleText(0);}
 public String sampleText(int voice){checkVoice(voice);return voice==1?"This is the Kurt American English LPC Festival voice.":"مَرْحَبًا بِكُمْ. هَذِهِ تَجْرِبَةٌ لِلصَّوْتِ الْعَرَبِيِّ الْقَدِيمِ.";}
 public String languageNotice(){return "Festival: Ziad Arabic HTS and Kurt American English classic LPC/diphone. Separate voice and dictionary licenses are bundled. These are not neural voices.";}
 public String[] noticeAssets(){return new String[]{"festival/VOICE-NOTICE.txt","festival/VOICE-GPL-3.0.txt","festival/VOICE-AUTHORS.md","festival/CORPUS-CC-BY-4.0.txt","festival/FESTIVAL-COPYING.txt","festival/SPEECH-TOOLS-COPYING.txt","festival/HTS-COPYING.txt","festival/KD-COPYRIGHT.txt","festival/CMU-LEXICON-COPYRIGHT.txt","festival/POSLEX-COPYRIGHT.txt"};}
 public synchronized void initialize(Context context){
  if(ready)return;
  File root=new File(context.getFilesDir(),DATA_VERSION),marker=new File(root,"installed");
  try{
   if(!marker.isFile()){
    if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Cannot create Festival directory");
    String base=root.getCanonicalPath()+File.separator;
    try(ZipInputStream zip=new ZipInputStream(context.getAssets().open("festival/data.zip"))){
     ZipEntry entry;long total=0;
     while((entry=zip.getNextEntry())!=null){
      File out=new File(root,entry.getName());
      if(!out.getCanonicalPath().startsWith(base))throw new IOException("Invalid Festival resource path");
      if(entry.isDirectory()){if(!out.isDirectory()&&!out.mkdirs())throw new IOException("Cannot install directory");continue;}
      File parent=out.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot install Festival resources");
      try(FileOutputStream stream=new FileOutputStream(out)){byte[] b=new byte[16384];int n;while((n=zip.read(b))!=-1){total+=n;if(total>75000000)throw new IOException("Festival data too large");stream.write(b,0,n);}}
     }
    }
    try(FileOutputStream f=new FileOutputStream(marker)){f.write(1);}
   }
   if(!NativeFestival.initialize(new File(root,"lib").getAbsolutePath()))throw new IOException("Festival initialization failed");
   ready=true;
  }catch(IOException e){throw new IllegalStateException("Cannot install Festival voices",e);}
 }
 public Locale voiceLocale(int voice){checkVoice(voice);return voice==1?Locale.US:new Locale("ar");}
 public int languageAvailability(String l,String c,String v){
  if("ar".equalsIgnoreCase(l)||"ara".equalsIgnoreCase(l))return TextToSpeech.LANG_AVAILABLE;
  if("en".equalsIgnoreCase(l)||"eng".equalsIgnoreCase(l))return "US".equalsIgnoreCase(c)||"USA".equalsIgnoreCase(c)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
  return TextToSpeech.LANG_NOT_SUPPORTED;
 }
 public boolean acceptsText(String text){return acceptsText(text,0)||acceptsText(text,1);}
 public boolean acceptsText(String text,int voice){
  if(voice<0||voice>1||text==null||text.trim().isEmpty())return false;
  if(voice==0)return ArabicFrontend.accepts(text);
  for(int at=0;at<text.length();){int cp=text.codePointAt(at);at+=Character.charCount(cp);if(Character.isISOControl(cp)&&cp!='\n'&&cp!='\r'&&cp!='\t')return false;if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}
  return true;
 }
 public int sampleRate(int voice){checkVoice(voice);return voice==1?16000:48000;}
 /** Upstream Arabic LTS expects shadda before the vowel. */
 static String normalizeDiacritics(String text){return text.replaceAll("([\\u064B-\\u0650\\u0652\\u0670]+)\\u0651","ّ$1");}
 public short[] synthesize(String text,int voice,float rate,float pitch){checkVoice(voice);if(!acceptsText(text,voice))throw new IllegalArgumentException("Unsupported Festival text");return NativeFestival.render(voice==0?normalizeDiacritics(text):text,voice,rate,pitch);}
}
final class NativeFestival {
 static {System.loadLibrary("festivalretro");}
 static synchronized native boolean initialize(String directory);
 static synchronized native short[] render(String text,int voice,float rate,float pitch);
}
