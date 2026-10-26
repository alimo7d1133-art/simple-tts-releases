/* Old TTS standalone audio pipeline. AGPL-3.0-or-later; see LICENSE. */
package org.retro.flitetts;

import android.content.Context;
import android.media.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;

/** Calls the bundled engines directly. Never binds to Android TextToSpeech. */
public final class StandaloneAudio {
 public static final int MAX_TEXT_LENGTH=4000;
 private static final Object ENGINE_LOCK=new Object();
 public interface Cancellation { boolean cancelled(); }
 public static final class Pcm {
  public final short[] samples; public final int sampleRate;
  Pcm(short[] samples,int hz){this.samples=samples;sampleRate=hz;}
 }
 public static final class RawiFailure extends IOException {RawiFailure(Exception cause){super("Rawi diacritization failed",cause);}}
 static void check(Cancellation cancel){if(cancel.cancelled()||Thread.currentThread().isInterrupted())throw new CancellationException();}
 public static Pcm render(Context context,EnginePlugin engine,int voice,String text,float rate,float pitch,Cancellation cancel) throws IOException {
  return render(context,engine,voice,text,rate,pitch,false,false,cancel);
 }
 public static Pcm render(Context context,EnginePlugin engine,int voice,String text,float rate,float pitch,boolean useRawi,Cancellation cancel) throws IOException {
  return render(context,engine,voice,text,rate,pitch,useRawi,false,cancel);
 }
 public static Pcm render(Context context,EnginePlugin engine,int voice,String text,float rate,float pitch,boolean useRawi,boolean highQuality,Cancellation cancel) throws IOException {
  if(text==null||text.trim().isEmpty()||text.length()>MAX_TEXT_LENGTH||voice<0||voice>=engine.voiceIds().length||!engine.acceptsText(text,voice))throw new IllegalArgumentException("Unsupported text or voice");
  if(!engine.voiceAvailable(context,voice)){String need=engine.requiredPack(voice);throw new PackStore.PackMissingException(need==null?engine.id():need);}
  check(cancel);
  if(useRawi&&"ar".equals(engine.voiceLocale(voice).getLanguage())){try{text=RawiDiacritizer.diacritize(context,text);}catch(CancellationException cancelled){throw cancelled;}catch(Exception failure){throw new RawiFailure(failure);}check(cancel);}
  synchronized(ENGINE_LOCK){
   check(cancel);EngineRegistry.prepare(context,engine);int hz=engine.sampleRate(voice);if(hz<8000||hz>96000)throw new IOException("Invalid sample rate");
   ArrayList<short[]> parts=new ArrayList<>();int total=0,offset=0;
   while(offset<text.length()){
    check(cancel);int end=Math.min(offset+160,text.length());
    if(end<text.length()){int cut=end;while(cut>offset+60&&!Character.isWhitespace(text.charAt(cut-1)))cut--;if(cut>offset+60)end=cut;if(end>offset&&Character.isHighSurrogate(text.charAt(end-1)))end--;}
    String part=text.substring(offset,end).trim();offset=end;if(part.isEmpty())continue;
    short[] pcm=engine.synthesize(part,voice,Math.max(.25f,Math.min(4,rate)),Math.max(.5f,Math.min(2,pitch)));check(cancel);
    if(pcm==null)throw new IOException("Synthesis failed");
    if(pcm.length>12000000-total)throw new IOException("Audio exceeds memory-safe length; shorten the text");
    parts.add(pcm);total+=pcm.length;
   }
   if(total==0)throw new IOException("Empty audio");short[] result=new short[total];int at=0;
   for(short[] p:parts){check(cancel);System.arraycopy(p,0,result,at,p.length);at+=p.length;}
   if(!highQuality||hz==AudioResampler.TARGET_SAMPLE_RATE)return new Pcm(result,hz);
   long outputLength=((long)result.length*AudioResampler.TARGET_SAMPLE_RATE+hz/2L)/hz;
   if(outputLength>AudioResampler.MAX_OUTPUT_SAMPLES)throw new IOException("Audio exceeds memory-safe length; shorten the text");
   try{return new Pcm(AudioResampler.to48k(result,hz,cancel),AudioResampler.TARGET_SAMPLE_RATE);}
   catch(IllegalArgumentException tooLong){throw new IOException("Audio exceeds memory-safe length; shorten the text",tooLong);}
  }
 }
 /** Blocking PCM playback on a worker; caller owns focus and cancellation. */
 public static void play(Pcm pcm,Cancellation cancel) throws IOException {
  check(cancel);int minimum=AudioTrack.getMinBufferSize(pcm.sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
  if(minimum<=0)throw new IOException("Audio output unavailable");
  AudioTrack track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(pcm.sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(Math.max(minimum,pcm.sampleRate/5*2)).setTransferMode(AudioTrack.MODE_STREAM).build();
  try{
   if(track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IOException("Audio output initialization failed");
   track.play();int at=0;long deadline=android.os.SystemClock.elapsedRealtime()+Math.max(30000,pcm.samples.length*1000L/pcm.sampleRate+15000);
   while(at<pcm.samples.length){check(cancel);int n=track.write(pcm.samples,at,Math.min(2048,pcm.samples.length-at),AudioTrack.WRITE_NON_BLOCKING);if(n<0)throw new IOException("Audio output failed");at+=n;if(android.os.SystemClock.elapsedRealtime()>deadline)throw new IOException("Audio output timed out");if(n==0)pause();}
   while(Integer.toUnsignedLong(track.getPlaybackHeadPosition())<pcm.samples.length){check(cancel);if(android.os.SystemClock.elapsedRealtime()>deadline)throw new IOException("Audio drain timed out");pause();}
  }finally{try{track.pause();track.flush();track.stop();}catch(IllegalStateException ignored){}track.release();}
 }
 private static void pause(){try{Thread.sleep(10);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}}
 private StandaloneAudio(){}
}
