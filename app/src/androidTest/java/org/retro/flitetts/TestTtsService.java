package org.retro.flitetts;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;
import java.util.*;

/** A tiny fake system TTS engine that lives only in the test package: it speaks a tone whose length follows the text length and speech rate. */
public final class TestTtsService extends TextToSpeechService {
 private String lang="eng",country="USA";
 @Override protected int onIsLanguageAvailable(String l,String c,String v){return "eng".equals(l)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_NOT_SUPPORTED;}
 @Override protected String[] onGetLanguage(){return new String[]{lang,country,""};}
 @Override protected int onLoadLanguage(String l,String c,String v){return onIsLanguageAvailable(l,c,v);}
 @Override public List<Voice> onGetVoices(){ArrayList<Voice> l=new ArrayList<>();l.add(new Voice("test-en-us",Locale.US,Voice.QUALITY_NORMAL,Voice.LATENCY_NORMAL,false,new HashSet<String>()));return l;}
 @Override public String onGetDefaultVoiceNameFor(String l,String c,String v){return "test-en-us";}
 @Override public int onIsValidVoiceName(String name){return "test-en-us".equals(name)?TextToSpeech.SUCCESS:TextToSpeech.ERROR;}
 @Override public int onLoadVoice(String name){return onIsValidVoiceName(name);}
 @Override protected void onStop(){}
 @Override protected void onSynthesizeText(SynthesisRequest r,SynthesisCallback cb){
  int rate=22050;cb.start(rate,android.media.AudioFormat.ENCODING_PCM_16BIT,1);
  double seconds=Math.max(.3,r.getCharSequenceText().length()*0.06/(Math.max(10,r.getSpeechRate())/100.0));
  int n=(int)(seconds*rate);byte[] buf=new byte[n*2];
  for(int i=0;i<n;i++){short v=(short)(9000*Math.sin(2*Math.PI*(220*r.getPitch()/100.0)*i/rate));buf[2*i]=(byte)v;buf[2*i+1]=(byte)(v>>8);}
  int max=cb.getMaxBufferSize();for(int o=0;o<buf.length;o+=max)cb.audioAvailable(buf,o,Math.min(max,buf.length-o));
  cb.done();
 }
}
