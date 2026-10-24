package org.retro.flitetts;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import java.io.*;
import java.nio.*;
import java.util.Arrays;

/** Actual registered-engine path; no inference from build success or file names. */
public final class EkhoInstrumentation extends Instrumentation {
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();EnginePlugin e=EngineRegistry.find("ekho");require(e!=null&&e.voiceIds().length==1,"Ekho not registered");
  require(!e.acceptsText(null,0)&&!e.acceptsText("Hello",0)&&!e.acceptsText("こんにちは",0)&&!e.acceptsText("\u0000",0),"Unsupported text accepted");
  File out=new File(c.getFilesDir(),"ekho-test");require(out.isDirectory()||out.mkdirs(),"Sample directory");
  String text=e.sampleText(0);
  StandaloneAudio.Pcm normal=StandaloneAudio.render(c,e,0,text,1f,1f,false,false,()->false);
  require(normal.sampleRate==16000&&energy(normal.samples)>10000,"Invalid/silent native PCM");save(out,"cantonese-16000",normal);
  StandaloneAudio.Pcm fast=StandaloneAudio.render(c,e,0,text,1.4f,1f,false,false,()->false);
  require(fast.samples.length<normal.samples.length&&energy(fast.samples)>10000,"Speed control ineffective");save(out,"cantonese-rate1.4",fast);
  StandaloneAudio.Pcm pitched=StandaloneAudio.render(c,e,0,text,1f,1.3f,false,false,()->false);
  require(!Arrays.equals(normal.samples,pitched.samples)&&energy(pitched.samples)>10000,"Pitch control ineffective");save(out,"cantonese-pitch1.3",pitched);
  StandaloneAudio.Pcm hq=StandaloneAudio.render(c,e,0,text,1f,1f,false,true,()->false);
  require(hq.sampleRate==48000&&energy(hq.samples)>10000,"48k path invalid");save(out,"cantonese-48000",hq);
  require(Math.abs(hq.samples.length/48000.0-normal.samples.length/16000.0)<.08,"HQ changed duration");
  result.putString("stream","PASS Ekho registered Cantonese voice, supported-text validation, actual nonzero PCM, speed shortened "+normal.samples.length+" -> "+fast.samples.length+", independent pitch change, shared StandaloneAudio 16k/48k WAV/MP3 exports. Samples="+out+"\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("error",t.toString());finish(Activity.RESULT_CANCELED,result);}}
 private static long energy(short[] p){long n=0;for(short s:p)n+=Math.abs((long)s);return n;}
 private static void save(File dir,String name,StandaloneAudio.Pcm pcm)throws IOException{
  ByteBuffer b=ByteBuffer.allocate(44+pcm.samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
  b.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.samples.length*2).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(pcm.sampleRate).putInt(pcm.sampleRate*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.samples.length*2);for(short s:pcm.samples)b.putShort(s);
  try(FileOutputStream f=new FileOutputStream(new File(dir,name+".wav"))){f.write(b.array());}
  Mp3Encoder.encode(pcm.samples,pcm.sampleRate,new File(dir,name+".mp3"));
 }
}
