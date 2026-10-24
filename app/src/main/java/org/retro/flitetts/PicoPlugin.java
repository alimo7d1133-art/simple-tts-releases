/* SVOX Pico plugin. AOSP SVOX sources and lingware are retained under pico/. */
package org.retro.flitetts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public final class PicoPlugin implements EnginePlugin {
 private static final String[] IDS={"en-US","en-GB","de-DE","es-ES","fr-FR","it-IT"};
 private static final String[] LABELS={"Pico English (US)","Pico English (UK)","Pico Deutsch", "Pico Español", "Pico Français", "Pico Italiano"};
 private static final Locale[] LOCALES={Locale.US,Locale.UK,Locale.GERMANY,new Locale("es","ES"),Locale.FRANCE,Locale.ITALY};
 private static volatile boolean ready;
 public String id(){return "pico";}
 public String label(){return "SVOX Pico — أصوات أندرويد الكلاسيكية";}
 public Locale locale(){return Locale.US;}
 public String[] voiceIds(){return IDS.clone();}
 public String[] voiceLabels(){return LABELS.clone();}
 public Locale voiceLocale(int voice){return LOCALES[Math.max(0,Math.min(LOCALES.length-1,voice))];}
 public String sampleText(int voice){String[] samples={"Hello. This is the SVOX Pico voice.","Hello. This is the British English Pico voice.","Hallo. Dies ist die klassische Pico Stimme.","Hola. Esta es la voz de Pico.","Bonjour. Ceci est la voix de Pico.","Ciao. Questa è la voce di Pico."};return samples[Math.max(0,Math.min(5,voice))];}
 public String sampleText(){return "Hello. This is the SVOX Pico voice.";}
 public String[] noticeAssets(){return new String[]{"PICO-NOTICE.txt"};}
 public String languageNotice(){return "Pico: الإنجليزية الأمريكية والبريطانية، والألمانية والإسبانية والفرنسية والإيطالية. لا يدعم العربية. يعمل دون إنترنت.";}
 public int languageAvailability(String lang,String country,String variant){
  for(Locale locale:LOCALES) {
   boolean language=locale.getLanguage().equalsIgnoreCase(lang)||locale.getISO3Language().equalsIgnoreCase(lang);
   boolean region=country==null||country.isEmpty()||locale.getCountry().equalsIgnoreCase(country)||locale.getISO3Country().equalsIgnoreCase(country);
   if(language&&region)return country==null||country.isEmpty()?TextToSpeech.LANG_AVAILABLE:TextToSpeech.LANG_COUNTRY_AVAILABLE;
  }
  for(Locale locale:LOCALES)if(locale.getLanguage().equalsIgnoreCase(lang)||locale.getISO3Language().equalsIgnoreCase(lang))return TextToSpeech.LANG_AVAILABLE;
  return TextToSpeech.LANG_NOT_SUPPORTED;
 }
 public boolean acceptsText(String text){if(text==null||text.indexOf('\u0000')>=0)return false;for(int at=0;at<text.length();){int cp=text.codePointAt(at);at+=Character.charCount(cp);if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}return true;}
 public int sampleRate(int voice){return 16000;}
 @Override public synchronized void initialize(Context context){
  if(!ready){ if(!PicoPlugin.nativeInit(context.getAssets(), context.getFilesDir().getAbsolutePath()))throw new IllegalStateException("Pico model initialization failed"); ready=true; }
 }
 public short[] synthesize(String text,int voice,float rate,float pitch){return nativeRender(text,voice,rate,pitch);}
 private static synchronized native boolean nativeInit(android.content.res.AssetManager assets,String filesDir);
 private static synchronized native short[] nativeRender(String text,int voice,float rate,float pitch);
 static { System.loadLibrary("pico"); }
}
