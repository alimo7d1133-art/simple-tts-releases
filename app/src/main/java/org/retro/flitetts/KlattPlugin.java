/* Standalone rhdunn Klatt 1980/1990 runtime, with Flite frontend. GPL-3+; see KLATT-COPYING.txt. */
package org.retro.flitetts;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
public final class KlattPlugin implements EnginePlugin {
 private static final String[] IDS={"impulsive","natural"};
 private static final String[] LABELS={"إثارة نبضية — Klatt", "إثارة حنجرية نموذجية — Klatt"};
 public String id(){return "klatt";}
 public String label(){return "Klatt 1980/1990 — Flite frontend";}
 public Locale locale(){return Locale.US;}
 public String[] voiceIds(){return IDS.clone();}
 public String[] voiceLabels(){return LABELS.clone();}
 public String sampleText(){return "Hello. This is the standalone Klatt formant synthesizer.";}
 public String[] noticeAssets(){return new String[]{"KLATT-COPYING.txt","KLATT-SOURCE-NOTICE.txt"};}
 public String languageNotice(){return "Klatt 1980/1990: نواة فورمانت مستقلة بترخيص GPL-3+، وواجهة Flite للإنجليزية فقط. خياران مختلفان لمصدر الإثارة، وليسا محركين منفصلين. جدول الفورمانت التقريبي جديد وقد تختلف جودة النطق عن المحركات القديمة التجارية. لا يدعم العربية.";}
 public int languageAvailability(String l,String c,String v){if(!"en".equalsIgnoreCase(l)&&!"eng".equalsIgnoreCase(l))return TextToSpeech.LANG_NOT_SUPPORTED;return ("US".equalsIgnoreCase(c)||"USA".equalsIgnoreCase(c))?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;}
 public boolean acceptsText(String t){if(t==null||t.trim().isEmpty())return false;for(int i=0;i<t.length();){int cp=t.codePointAt(i);i+=Character.charCount(cp);if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}return true;}
 public int sampleRate(int voice){check(voice);return NativeKlatt.sampleRate(voice);}
 public short[] synthesize(String text,int voice,float rate,float pitch){check(voice);if(!acceptsText(text))throw new IllegalArgumentException("Klatt accepts non-empty English Latin text");return NativeKlatt.render(text,voice,rate,pitch);}
 private static void check(int v){if(v<0||v>=IDS.length)throw new IllegalArgumentException("Unknown Klatt source");}
}
final class NativeKlatt { static {System.loadLibrary("klattretro");} static synchronized native int sampleRate(int voice); static synchronized native short[] render(String text,int voice,float rate,float pitch); private NativeKlatt(){} }
