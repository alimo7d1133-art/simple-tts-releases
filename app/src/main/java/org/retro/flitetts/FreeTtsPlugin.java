/* Old TTS FreeTTS adapter, AGPL-3.0-or-later; upstream BSD-style terms separate.
 * Modified 2026: direct CMU runtime, Android PCM sink and serialized requests.
 * Upstream emits BIG-endian 16-bit PCM even for the 8 kHz KAL database. */
package org.retro.flitetts;
import android.speech.tts.TextToSpeech;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory;
import com.sun.speech.freetts.audio.AudioPlayer;
import java.util.Locale;
import javax.sound.sampled.AudioFormat;
public final class FreeTtsPlugin implements EnginePlugin {
 private static final String[] IDS={"kal","kal16"};
 private static final String[] LABELS={"CMU KAL — FreeTTS، جودة 8 كيلوهرتز","CMU KAL16 — FreeTTS، جودة 16 كيلوهرتز"};
 private static final Object LOCK=new Object();
 public String id(){return "freetts";}public String label(){return "FreeTTS — محرك CMU diphone بلغة Java";}
 public Locale locale(){return Locale.US;}public String[] voiceIds(){return IDS.clone();}public String[] voiceLabels(){return LABELS.clone();}
 public String sampleText(){return "Hello. This is the FreeTTS CMU KAL diphone voice.";}
 public String languageNotice(){return "FreeTTS: الإنجليزية الأمريكية فقط. نواة Java أصلية من Sun وCMU مبنية تاريخيًا على Flite، وليست تحويلًا إلى MBROLA. KAL وKAL16 تسجيل واحد بجودتين؛ ليستا شخصيتين صوتيتين جديدتين. يعمل محليًا.";}
 public String[] noticeAssets(){return new String[]{"freetts/FREETTS-SOURCE-NOTICE.txt","freetts/FREETTS-LICENSE-TERMS.txt","freetts/CMU-KAL-README.txt"};}
 public int languageAvailability(String l,String c,String v){if(!"en".equalsIgnoreCase(l)&&!"eng".equalsIgnoreCase(l))return TextToSpeech.LANG_NOT_SUPPORTED;return "US".equalsIgnoreCase(c)||"USA".equalsIgnoreCase(c)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;}
 public boolean acceptsText(String t){if(t==null||t.trim().isEmpty())return false;for(int i=0;i<t.length();){int cp=t.codePointAt(i);i+=Character.charCount(cp);if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}return true;}
 public int sampleRate(int voice){return voice==0?8000:16000;}
 public short[] synthesize(String text,int which,float rate,float pitch){
  if(!acceptsText(text)||which<0||which>1)throw new IllegalArgumentException("Unsupported FreeTTS input");
  synchronized(LOCK){Voice v=new KevinVoiceDirectory().getVoices()[which];PcmPlayer p=new PcmPlayer();v.setAudioPlayer(p);v.allocate();
   try{v.setRate(150*Math.max(.25f,Math.min(4,rate)));v.setPitch(v.getPitch()*Math.max(.5f,Math.min(2,pitch)));if(!v.speak(text))throw new IllegalStateException("FreeTTS synthesis failed");return p.samples();}finally{v.deallocate();}
  }
 }
 private static final class PcmPlayer implements AudioPlayer {
  private final java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();private AudioFormat format;
  public void setAudioFormat(AudioFormat f){if(f.getSampleSizeInBits()!=16||f.getChannels()!=1)throw new IllegalArgumentException("Unexpected FreeTTS format");format=f;}public AudioFormat getAudioFormat(){return format;}
  public void pause(){}public void resume(){}public void reset(){out.reset();}public boolean drain(){return true;}public void begin(int n){}public boolean end(){return true;}public void cancel(){}public void close(){}public float getVolume(){return 1;}public void setVolume(float x){}public long getTime(){return 0;}public void resetTime(){}public void startFirstSampleTimer(){}public void showMetrics(){}
  public boolean write(byte[] b){return write(b,0,b.length);}public boolean write(byte[] b,int offset,int n){if(out.size()+n>16000*2*180)throw new IllegalStateException("FreeTTS chunk too long");out.write(b,offset,n);return true;}
  short[] samples(){byte[] b=out.toByteArray();short[] s=new short[b.length/2];boolean big=format!=null&&format.isBigEndian();for(int i=0;i<s.length;i++){int hi=big?b[2*i]:b[2*i+1],lo=big?b[2*i+1]:b[2*i];s[i]=(short)((hi<<8)|(lo&255));}return s;}
 }
}
