package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Actual new voice synthesis and export. Normal samples are rate=1, pitch=1. */
public final class FliteExpansionInstrumentation extends Instrumentation {
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 private void note(String m){Bundle b=new Bundle();b.putString("stream",m+"\n");sendStatus(1,b);}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();EnginePlugin e=EngineRegistry.find("flite");require(e!=null,"Missing Flite");
  require(e.voiceIds().length==22,"Expected twenty-two Flite options");require("asc".equals(e.voiceIds()[9]),"Existing Arabic index changed");
  File dir=new File(c.getExternalFilesDir(null),"new-flite-voices");require(dir.isDirectory()||dir.mkdirs(),"Sample directory");
  Set<Integer> waveHashes=new HashSet<>();
  for(String id:new String[]{"aew","ahw","aup"}){
   int voice=Arrays.asList(e.voiceIds()).indexOf(id);require(voice>=10,"Missing new distinct voice "+id);
   require(!e.acceptsText("مرحبا بكم",voice),"English voice claims Arabic");
   String text="Hello. This is a new classic voice in Simple TTS. One, two, three.";
   StandaloneAudio.Pcm normal=StandaloneAudio.render(c,e,voice,text,1f,1f,false,false,()->false);
   require(normal.samples.length>1000&&energy(normal.samples)>10000,"Empty voice "+id);
   require(waveHashes.add(Arrays.hashCode(normal.samples)),"New voice exactly duplicates another PCM "+id);
   wav(new File(dir,id+"-normal.wav"),normal);Mp3Encoder.encode(normal.samples,normal.sampleRate,new File(dir,id+"-normal.mp3"));
   StandaloneAudio.Pcm changed=StandaloneAudio.render(c,e,voice,text,1.4f,.8f,false,false,()->false);
   require(changed.samples.length<normal.samples.length,"Rate control did not shorten "+id);
   require(energy(changed.samples)>10000,"Changed params silent "+id);
   wav(new File(dir,id+"-rate1.4-pitch0.8.wav"),changed);
   StandaloneAudio.Pcm hq=StandaloneAudio.render(c,e,voice,text,1f,1f,false,true,()->false);
   require(hq.sampleRate==48000&&energy(hq.samples)>10000,"48k output "+id);
   wav(new File(dir,id+"-48k.wav"),hq);Mp3Encoder.encode(hq.samples,hq.sampleRate,new File(dir,id+"-48k.mp3"));
   note("PASS Flite new="+id+" nativeHz="+normal.sampleRate+" normalSamples="+normal.samples.length+" changedSamples="+changed.samples.length+" hqHz="+hq.sampleRate);
  }
  result.putString("stream","PASS three additional distinct Flite models: AEW, AHW, AUP; actual normal and changed-parameter PCM; MP3 and 48k WAV/MP3; old Arabic index and text validation preserved. Samples="+dir+"\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("error",t.toString());finish(Activity.RESULT_CANCELED,result);}}
 private static long energy(short[] p){long x=0;for(short s:p)x+=Math.abs((long)s);return x;}
 private static void wav(File file,StandaloneAudio.Pcm pcm)throws IOException{
  ByteBuffer b=ByteBuffer.allocate(44+pcm.samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
  b.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.samples.length*2).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(pcm.sampleRate).putInt(pcm.sampleRate*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.samples.length*2);for(short s:pcm.samples)b.putShort(s);
  try(FileOutputStream f=new FileOutputStream(file)){f.write(b.array());}
 }
}
