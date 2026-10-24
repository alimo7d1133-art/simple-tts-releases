/* Real rsynth formant-synthesis plugin. Upstream notices are in RSYNTH assets. */
package org.retro.flitetts;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
public final class RsynthPlugin implements EnginePlugin {
 private static final String[] IDS={"default"};
 private static final String[] LABELS={"rsynth default — صوت فورمانت إنجليزي اصطناعي"};
 public String id(){return "rsynth";}
 public String label(){return "rsynth — مُركّب فورمانت مفتوح المصدر";}
 public Locale locale(){return Locale.US;}
 public String[] voiceIds(){return IDS.clone();}
 public String[] voiceLabels(){return LABELS.clone();}
 public String sampleText(){return "Hello. This is the rsynth formant voice.";}
 public String[] noticeAssets(){return new String[]{"RSYNTH-COPYING.txt","RSYNTH-NOTICES.txt","RSYNTH-GPL-2.0.txt","RSYNTH-LGPL-2.0.txt"};}
 public String languageNotice(){return "rsynth: الإنجليزية فقط؛ تركيب فورمانت محلي دون بيانات صوت مسجلة.";}
 public int languageAvailability(String lang,String country,String variant){
  if(!"eng".equalsIgnoreCase(lang)&&!"en".equalsIgnoreCase(lang))return TextToSpeech.LANG_NOT_SUPPORTED;
  return "USA".equalsIgnoreCase(country)||"US".equalsIgnoreCase(country)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
 }
 public boolean acceptsText(String text){
  for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;} return true;
 }
 public int sampleRate(int voice){return RsynthNative.sampleRate(voice);}
 public short[] synthesize(String text,int voice,float rate,float pitch){return RsynthNative.render(text,voice,rate,pitch);}
}
final class RsynthNative {
 static { System.loadLibrary("rsynthretro"); }
 static native short[] render(String text,int voice,float rate,float pitch);
 static native int sampleRate(int voice);
 private RsynthNative(){}
}
