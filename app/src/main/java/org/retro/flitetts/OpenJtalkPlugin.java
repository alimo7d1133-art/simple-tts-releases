/* Open JTalk 1.11 + HTS engine API 1.10; offline Japanese-only legacy HTS voice. */
package org.retro.flitetts;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
public final class OpenJtalkPlugin implements EnginePlugin {
 private static volatile boolean ready;
 public String id(){return "openjtalk";}
 public String label(){return "Open JTalk / HTS — اليابانية فقط";}
 public Locale locale(){return Locale.JAPAN;}
 public String[] voiceIds(){return new String[]{"ja-JP-nitech"};}
 public String[] voiceLabels(){return new String[]{"Nitech Japanese (male)"};}
 public String sampleText(){return "こんにちは。これはオープンジェイトークの音声です。";}
 public String[] noticeAssets(){return new String[]{"openjtalk/OPENJTALK-NOTICE.txt","openjtalk/licenses/OPENJTALK-COPYING.txt","openjtalk/licenses/HTS-COPYING.txt","openjtalk/licenses/MECAB-COPYING.txt","openjtalk/licenses/NITECH-CC-BY3-COPYING.txt","openjtalk/open_jtalk_dic_utf_8-1.11/COPYING","openjtalk/voice/COPYRIGHT"};}
 public String languageNotice(){return "Open JTalk: اليابانية فقط. لا يدعم العربية. يعمل دون اتصال بالإنترنت.";}
 public int languageAvailability(String l,String c,String v){boolean ja="ja".equalsIgnoreCase(l)||"jpn".equalsIgnoreCase(l);if(!ja)return TextToSpeech.LANG_NOT_SUPPORTED;return c==null||c.length()==0||"JP".equalsIgnoreCase(c)?(c==null||c.length()==0?TextToSpeech.LANG_AVAILABLE:TextToSpeech.LANG_COUNTRY_AVAILABLE):TextToSpeech.LANG_AVAILABLE;}
 public boolean acceptsText(String s){if(s==null||s.indexOf('\0')>=0)return false;for(int i=0;i<s.length();){int cp=s.codePointAt(i);i+=Character.charCount(cp);Character.UnicodeScript script=Character.UnicodeScript.of(cp);if(Character.isLetter(cp)&&script!=Character.UnicodeScript.HAN&&script!=Character.UnicodeScript.HIRAGANA&&script!=Character.UnicodeScript.KATAKANA&&script!=Character.UnicodeScript.LATIN&&script!=Character.UnicodeScript.COMMON)return false;}return true;}
 public int sampleRate(int v){return 48000;}
 public synchronized void initialize(Context c){if(!ready){if(!nativeInit(c.getAssets(),c.getFilesDir().getAbsolutePath()))throw new IllegalStateException("Open JTalk initialization failed");ready=true;}}
 public synchronized short[] synthesize(String t,int v,float r,float p){if(!ready)throw new IllegalStateException("Open JTalk is not initialized"); if(!acceptsText(t))throw new IllegalArgumentException("Open JTalk accepts bounded Japanese/Latin text"); return nativeRender(t,v,r,p);}
 private static native boolean nativeInit(android.content.res.AssetManager a,String files);
 private static native short[] nativeRender(String t,int v,float r,float p);
 static{System.loadLibrary("openjtalk");}
}
