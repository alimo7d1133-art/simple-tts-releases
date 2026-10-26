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
/** 1.7.0 additions: eight CMU ARCTIC Flite voices, ClArTTS Arabic, and the Flite+HTS SLT voice. */
public final class V170ExpansionInstrumentation extends Instrumentation {
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 private void note(String m){Bundle b=new Bundle();b.putString("stream",m+"\n");sendStatus(1,b);}
 private void check(Context c,File dir,EnginePlugin e,String engineName,String id,String text,boolean arabic,Set<Integer> hashes)throws Exception{
  int voice=Arrays.asList(e.voiceIds()).indexOf(id);require(voice>=0,"Missing voice "+id);
  require(e.acceptsText(text,voice),"Voice rejects own sample text "+id);
  if(!arabic)require(!e.acceptsText("\u0645\u0631\u062d\u0628\u0627 \u0628\u0643\u0645",voice),"Wrong-language text accepted by "+id);
  StandaloneAudio.Pcm normal=StandaloneAudio.render(c,e,voice,text,1f,1f,false,false,()->false);
  require(normal.samples.length>4000&&energy(normal.samples)>20000,"Empty voice "+id);
  require(hashes.add(Arrays.hashCode(normal.samples)),"PCM duplicates another voice "+id);
  String base=engineName+"-"+id;
  wav(new File(dir,base+"-normal.wav"),normal);Mp3Encoder.encode(normal.samples,normal.sampleRate,new File(dir,base+"-normal.mp3"));
  StandaloneAudio.Pcm fast=StandaloneAudio.render(c,e,voice,text,1.4f,1f,false,false,()->false);
  require(fast.samples.length<normal.samples.length*0.9&&energy(fast.samples)>20000,"Rate control ineffective "+id);
  StandaloneAudio.Pcm low=StandaloneAudio.render(c,e,voice,text,1f,.7f,false,false,()->false);
  require(energy(low.samples)>20000&&!Arrays.equals(low.samples,normal.samples),"Pitch control ineffective "+id);
  wav(new File(dir,base+"-rate1.4.wav"),fast);wav(new File(dir,base+"-pitch0.7.wav"),low);
  StandaloneAudio.Pcm hq=StandaloneAudio.render(c,e,voice,text,1f,1f,false,true,()->false);
  require(hq.sampleRate==48000&&energy(hq.samples)>20000,"48 kHz output "+id);
  wav(new File(dir,base+"-48k.wav"),hq);Mp3Encoder.encode(hq.samples,hq.sampleRate,new File(dir,base+"-48k.mp3"));
  note("PASS "+engineName+" voice="+id+" nativeHz="+normal.sampleRate+" normal="+normal.samples.length+" fast="+fast.samples.length+" low="+low.samples.length+" hqHz="+hq.sampleRate);
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();
  EnginePlugin flite=EngineRegistry.find("flite");require(flite!=null&&flite.voiceIds().length==40,"Expected 40 Flite options");
  require("asc".equals(flite.voiceIds()[9])&&"clartts".equals(flite.voiceIds()[21]),"Arabic indexes changed");
  EnginePlugin hts=EngineRegistry.find("flitehts");require(hts!=null&&hts.voiceIds().length==1,"Missing Flite+HTS");
  require(EngineRegistry.all().size()==12,"Expected 12 engines");
  File dir=new File(c.getExternalFilesDir(null),"v170-samples");require(dir.isDirectory()||dir.mkdirs(),"Sample directory");
  Set<Integer> hashes=new HashSet<>();
  String en="Hello. This is a new voice in Simple TTS. One, two, three.";
  for(String id:new String[]{"axb","eey","fem","gka","ljm","lnh","rxr","slp"})check(c,dir,flite,"flite",id,en,false,hashes);
  check(c,dir,hts,"flitehts","cmu-us-arctic-slt-hts",en,false,hashes);
  check(c,dir,flite,"flite","clartts","\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064e\u0647\u0650 \u0627\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0646\u0650 \u0627\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650. \u0647\u064e\u0630\u064e\u0627 \u0635\u064e\u0648\u0652\u062a\u064c \u0639\u064e\u0631\u064e\u0628\u0650\u064a\u0651\u064c \u062c\u064e\u062f\u0650\u064a\u062f\u064c.",true,hashes);
  result.putString("stream","PASS V170: eight CMU ARCTIC Flite voices, ClArTTS Arabic, Flite+HTS SLT; normal/rate/pitch/48k PCM, WAV and MP3.");
  finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("error",t.toString());finish(Activity.RESULT_CANCELED,result);}}
 private static long energy(short[] p){long x=0;for(short s:p)x+=Math.abs((long)s);return x;}
 private static void wav(File file,StandaloneAudio.Pcm pcm)throws IOException{
  ByteBuffer b=ByteBuffer.allocate(44+pcm.samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
  b.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.samples.length*2).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(pcm.sampleRate).putInt(pcm.sampleRate*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.samples.length*2);for(short s:pcm.samples)b.putShort(s);
  try(FileOutputStream f=new FileOutputStream(file)){f.write(b.array());}
 }
}
