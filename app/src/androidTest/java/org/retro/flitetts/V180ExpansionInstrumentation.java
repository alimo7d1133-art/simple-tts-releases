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
/** 1.8.0 additions: new Arabic, American English, Russian and Indian-language Flite voices. */
public final class V180ExpansionInstrumentation extends Instrumentation {
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 private void note(String m){Bundle b=new Bundle();b.putString("stream",m+"\n");sendStatus(1,b);}
 private void check(Context c,File dir,EnginePlugin e,String engineName,String id,String[] loc,String text,boolean arabic,Set<Integer> hashes)throws Exception{
  int voice=Arrays.asList(e.voiceIds()).indexOf(id);require(voice>=0,"Missing voice "+id);require(loc[0].equals(e.voiceLocale(voice).getLanguage())&&loc[1].equals(e.voiceLocale(voice).getCountry()),"Locale of "+id);require(text.equals(e.sampleText(voice))||loc[0].equals("en"),"Sample text of "+id);
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
   require(EngineRegistry.all().size()==11,"Expected 11 engines");
   File dir=new File(c.getExternalFilesDir(null),"v180-samples");require(dir.isDirectory()||dir.mkdirs(),"Sample directory");
   Set<Integer> hashes=new HashSet<>();
   check(c,dir,flite,"flite","iqcorp",new String[]{"ar","SA"},"\u0645\u064e\u0631\u0652\u062d\u064e\u0628\u064b\u0627 \u0628\u0650\u0643\u064f\u0645\u0652. \u0647\u064e\u0630\u0650\u0647\u0650 \u062a\u064e\u062c\u0652\u0631\u0650\u0628\u064e\u0629\u064c \u0644\u0650\u0644\u0635\u064e\u0651\u0648\u0652\u062a\u0650 \u0627\u0644\u0652\u0639\u064e\u0631\u064e\u0628\u0650\u064a\u0650\u0651 \u0627\u0644\u0652\u0642\u064e\u062f\u0650\u064a\u0645\u0650.",true,hashes);
   check(c,dir,flite,"flite","cvm",new String[]{"ar","SA"},"\u0645\u064e\u0631\u0652\u062d\u064e\u0628\u064b\u0627 \u0628\u0650\u0643\u064f\u0645\u0652. \u0647\u064e\u0630\u0650\u0647\u0650 \u062a\u064e\u062c\u0652\u0631\u0650\u0628\u064e\u0629\u064c \u0644\u0650\u0644\u0635\u064e\u0651\u0648\u0652\u062a\u0650 \u0627\u0644\u0652\u0639\u064e\u0631\u064e\u0628\u0650\u064a\u0650\u0651 \u0627\u0644\u0652\u0642\u064e\u062f\u0650\u064a\u0645\u0650.",true,hashes);
   check(c,dir,flite,"flite","cvf",new String[]{"ar","SA"},"\u0645\u064e\u0631\u0652\u062d\u064e\u0628\u064b\u0627 \u0628\u0650\u0643\u064f\u0645\u0652. \u0647\u064e\u0630\u0650\u0647\u0650 \u062a\u064e\u062c\u0652\u0631\u0650\u0628\u064e\u0629\u064c \u0644\u0650\u0644\u0635\u064e\u0651\u0648\u0652\u062a\u0650 \u0627\u0644\u0652\u0639\u064e\u0631\u064e\u0628\u0650\u064a\u0650\u0651 \u0627\u0644\u0652\u0642\u064e\u062f\u0650\u064a\u0645\u0650.",true,hashes);
   check(c,dir,flite,"flite","hf92",new String[]{"en","US"},"Hello. This is a new voice in Simple TTS. One, two, three.",false,hashes);
   check(c,dir,flite,"flite","hm6097",new String[]{"en","US"},"Hello. This is a new voice in Simple TTS. One, two, three.",false,hashes);
   check(c,dir,flite,"flite","hin_ab",new String[]{"hi","IN"},"\u0928\u092e\u0938\u094d\u0924\u0947\u0964 \u092f\u0939 \u090f\u0915 \u0928\u0908 \u0906\u0935\u093e\u091c\u093c \u0939\u0948\u0964",false,hashes);
   check(c,dir,flite,"flite","ben_rm",new String[]{"bn","IN"},"\u09a8\u09ae\u09b8\u09cd\u0995\u09be\u09b0\u0964 \u098f\u099f\u09bf \u098f\u0995\u099f\u09bf \u09a8\u09a4\u09c1\u09a8 \u0995\u09a3\u09cd\u09a0\u09b8\u09cd\u09ac\u09b0\u0964",false,hashes);
   check(c,dir,flite,"flite","guj_ad",new String[]{"gu","IN"},"\u0aa8\u0aae\u0ab8\u0acd\u0aa4\u0ac7. \u0a86 \u0a8f\u0a95 \u0aa8\u0ab5\u0acb \u0a85\u0ab5\u0abe\u0a9c \u0a9b\u0ac7.",false,hashes);
   check(c,dir,flite,"flite","guj_dp",new String[]{"gu","IN"},"\u0aa8\u0aae\u0ab8\u0acd\u0aa4\u0ac7. \u0a86 \u0a8f\u0a95 \u0aa8\u0ab5\u0acb \u0a85\u0ab5\u0abe\u0a9c \u0a9b\u0ac7.",false,hashes);
   check(c,dir,flite,"flite","guj_kt",new String[]{"gu","IN"},"\u0aa8\u0aae\u0ab8\u0acd\u0aa4\u0ac7. \u0a86 \u0a8f\u0a95 \u0aa8\u0ab5\u0acb \u0a85\u0ab5\u0abe\u0a9c \u0a9b\u0ac7.",false,hashes);
   check(c,dir,flite,"flite","kan_plv",new String[]{"kn","IN"},"\u0ca8\u0cae\u0cb8\u0ccd\u0c95\u0cbe\u0cb0. \u0c87\u0ca6\u0cc1 \u0c92\u0c82\u0ca6\u0cc1 \u0cb9\u0cca\u0cb8 \u0ca7\u0ccd\u0cb5\u0ca8\u0cbf.",false,hashes);
   check(c,dir,flite,"flite","mar_aup",new String[]{"mr","IN"},"\u0928\u092e\u0938\u094d\u0915\u093e\u0930. \u0939\u093e \u090f\u0915 \u0928\u0935\u0940\u0928 \u0906\u0935\u093e\u091c \u0906\u0939\u0947.",false,hashes);
   check(c,dir,flite,"flite","mar_slp",new String[]{"mr","IN"},"\u0928\u092e\u0938\u094d\u0915\u093e\u0930. \u0939\u093e \u090f\u0915 \u0928\u0935\u0940\u0928 \u0906\u0935\u093e\u091c \u0906\u0939\u0947.",false,hashes);
   check(c,dir,flite,"flite","pan_amp",new String[]{"pa","IN"},"\u0a38\u0a24 \u0a38\u0a4d\u0a30\u0a40 \u0a05\u0a15\u0a3e\u0a32\u0964 \u0a07\u0a39 \u0a07\u0a71\u0a15 \u0a28\u0a35\u0a40\u0a02 \u0a06\u0a35\u0a3e\u0a1c\u0a3c \u0a39\u0a48\u0964",false,hashes);
   check(c,dir,flite,"flite","tam_sdr",new String[]{"ta","IN"},"\u0bb5\u0ba3\u0b95\u0bcd\u0b95\u0bae\u0bcd. \u0b87\u0ba4\u0bc1 \u0b92\u0bb0\u0bc1 \u0baa\u0bc1\u0ba4\u0bbf\u0baf \u0b95\u0bc1\u0bb0\u0bb2\u0bcd.",false,hashes);
   check(c,dir,flite,"flite","tel_kpn",new String[]{"te","IN"},"\u0c28\u0c2e\u0c38\u0c4d\u0c15\u0c3e\u0c30\u0c02. \u0c07\u0c26\u0c3f \u0c12\u0c15 \u0c15\u0c4a\u0c24\u0c4d\u0c24 \u0c38\u0c4d\u0c35\u0c30\u0c02.",false,hashes);
   check(c,dir,flite,"flite","tel_sk",new String[]{"te","IN"},"\u0c28\u0c2e\u0c38\u0c4d\u0c15\u0c3e\u0c30\u0c02. \u0c07\u0c26\u0c3f \u0c12\u0c15 \u0c15\u0c4a\u0c24\u0c4d\u0c24 \u0c38\u0c4d\u0c35\u0c30\u0c02.",false,hashes);
   check(c,dir,flite,"flite","tel_ss",new String[]{"te","IN"},"\u0c28\u0c2e\u0c38\u0c4d\u0c15\u0c3e\u0c30\u0c02. \u0c07\u0c26\u0c3f \u0c12\u0c15 \u0c15\u0c4a\u0c24\u0c4d\u0c24 \u0c38\u0c4d\u0c35\u0c30\u0c02.",false,hashes);
   result.putString("stream","PASS V180: 18 new Flite voices; locale, script validation, normal/rate/pitch/48k PCM, WAV and MP3.");
   finish(Activity.RESULT_OK,result);
  }catch(Throwable t){result.putString("error",t.toString());finish(Activity.RESULT_CANCELED,result);}}
 private static long energy(short[] p){long x=0;for(short s:p)x+=Math.abs((long)s);return x;}
 private static void wav(File file,StandaloneAudio.Pcm pcm)throws IOException{
  ByteBuffer b=ByteBuffer.allocate(44+pcm.samples.length*2).order(ByteOrder.LITTLE_ENDIAN);
  b.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.samples.length*2).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(pcm.sampleRate).putInt(pcm.sampleRate*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.samples.length*2);for(short s:pcm.samples)b.putShort(s);
  try(FileOutputStream f=new FileOutputStream(file)){f.write(b.array());}
 }
}
