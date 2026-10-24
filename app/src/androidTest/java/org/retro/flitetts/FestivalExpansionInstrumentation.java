package org.retro.flitetts;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import java.io.*;
import java.nio.*;
import java.util.Arrays;

/** Tests actual classic Festival LPC and cross-voice state restoration. */
public final class FestivalExpansionInstrumentation extends Instrumentation {
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 private void note(String m){Bundle b=new Bundle();b.putString("stream",m+"\n");sendStatus(1,b);}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();EnginePlugin e=EngineRegistry.find("festival");
  require(e!=null&&e.voiceIds().length==2&&"ked_diphone".equals(e.voiceIds()[1]),"Kurt not registered");
  require(!e.acceptsText(null,1)&&!e.acceptsText("مرحبا",1)&&!e.acceptsText("\u0000",1),"Kurt unsupported-text validation");
  require("en".equals(e.voiceLocale(1).getLanguage())&&"ar".equals(e.voiceLocale(0).getLanguage()),"Voice locales");
  String text="Hello. This is Kurt, a classic American English voice in Simple TTS.";
  File d=new File(c.getExternalFilesDir(null),"new-festival-voice");require(d.isDirectory()||d.mkdirs(),"Samples directory");
  StandaloneAudio.Pcm normal=StandaloneAudio.render(c,e,1,text,1f,1f,false,false,()->false);
  require(normal.sampleRate==16000&&energy(normal.samples)>10000,"Kurt PCM invalid");
  save(d,"kurt-normal",normal);
  StandaloneAudio.Pcm fast=StandaloneAudio.render(c,e,1,text,1.4f,1f,false,false,()->false);
  require(fast.samples.length<normal.samples.length&&energy(fast.samples)>10000,"Kurt rate control");save(d,"kurt-rate1.4",fast);
  StandaloneAudio.Pcm pitched=StandaloneAudio.render(c,e,1,text,1f,1.3f,false,false,()->false);
  require(!Arrays.equals(normal.samples,pitched.samples)&&energy(pitched.samples)>10000,"Kurt pitch control");save(d,"kurt-pitch1.3",pitched);
  StandaloneAudio.Pcm hq=StandaloneAudio.render(c,e,1,text,1f,1f,false,true,()->false);
  require(hq.sampleRate==48000&&energy(hq.samples)>10000,"Kurt 48k");save(d,"kurt-48k",hq);
  StandaloneAudio.Pcm arabic=StandaloneAudio.render(c,e,0,e.sampleText(0),1f,1f,false,false,()->false);
  require(arabic.sampleRate==48000&&energy(arabic.samples)>10000,"Arabic after Kurt");
  StandaloneAudio.Pcm kurtAgain=StandaloneAudio.render(c,e,1,text,1f,1f,false,false,()->false);
  require(kurtAgain.sampleRate==16000&&energy(kurtAgain.samples)>10000,"Kurt after Arabic");
  note("PASS Kurt native16000 normalSamples="+normal.samples.length+" fastSamples="+fast.samples.length+" pitchedSamples="+pitched.samples.length+" HQ48000; English-Arabic-English restoration");
  result.putString("stream","PASS Festival Kurt actual LPC synthesis, speed, pitch, 48k, WAV/MP3, text/locales, and Ziad restoration. Samples="+d+"\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("error",t.toString());finish(Activity.RESULT_CANCELED,result);}}
 private static long energy(short[] p){long v=0;for(short s:p)v+=Math.abs((long)s);return v;}
 private static void save(File dir,String name,StandaloneAudio.Pcm pcm)throws IOException{
  ByteBuffer b=ByteBuffer.allocate(44+pcm.samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
  b.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.samples.length*2).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(pcm.sampleRate).putInt(pcm.sampleRate*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.samples.length*2);for(short s:pcm.samples)b.putShort(s);
  try(FileOutputStream f=new FileOutputStream(new File(dir,name+".wav"))){f.write(b.array());}
  Mp3Encoder.encode(pcm.samples,pcm.sampleRate,new File(dir,name+".mp3"));
 }
}
