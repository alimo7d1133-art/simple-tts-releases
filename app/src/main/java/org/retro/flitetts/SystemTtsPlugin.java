/* Simple TTS: voices of the text-to-speech engines installed on the phone (Google, Samsung, ...),
 * used through android.speech.tts.TextToSpeech.synthesizeToFile. Simple TTS itself is never a system engine.
 * AGPL-3.0-or-later. These engines and voices are external; they may be neural and may use the internet
 * according to their own settings. */
package org.retro.flitetts;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class SystemTtsPlugin implements EnginePlugin {
 public static final class EngineInfo {public final String pkg,label;EngineInfo(String p,String l){pkg=p;label=l;}}
 private static final Object LOCK=new Object();
 private volatile Context app;
 private TextToSpeech tts;private String boundPackage="";
 private volatile ArrayList<Voice> voices=new ArrayList<>();
 private volatile String[] ids=new String[0],labels=new String[0];
 private volatile int engineRate=0;
 public String id(){return "system";}
 public String label(){return "Android — installed voices";}
 public Locale locale(){return Locale.getDefault();}
 public String[] voiceIds(){return ids.clone();}
 public String[] voiceLabels(){return labels.clone();}
 public Locale voiceLocale(int voice){ArrayList<Voice> v=voices;return voice>=0&&voice<v.size()?v.get(voice).getLocale():Locale.getDefault();}
 public String sampleText(){return "Hello. This is a voice of the engine installed on this phone.";}
 public String sampleText(int voice){
  String lang=voiceLocale(voice).getLanguage();
  switch(lang){case "ar":return "مَرْحَبًا بِكُمْ. هَذَا صَوْتٌ مِنَ الْمُحَرِّكِ الْمُثَبَّتِ عَلَى الْهَاتِفِ.";case "de":return "Hallo. Dies ist eine Stimme des auf dem Telefon installierten Sprachmotors.";
   case "es":return "Hola. Esta es una voz del motor instalado en el teléfono.";case "fr":return "Bonjour. Voici une voix du moteur installé sur le téléphone.";case "it":return "Ciao. Questa è una voce del motore installato sul telefono.";
   case "ru":return "Здравствуйте. Это голос движка, установленного на телефоне.";case "ja":return "こんにちは。これは端末にインストールされた音声エンジンの声です。";case "hi":return "नमस्ते। यह फ़ोन में स्थापित इंजन की आवाज़ है।";default:return sampleText();}
 }
 public String[] noticeAssets(){return new String[0];}
 public String languageNotice(){return "أصوات النظام: محركات TTS المثبتة على الهاتف (Google وSamsung وغيرها) تُستخدم عبر أندرويد. هذه محركات خارجية قد تكون عصبية وقد تستخدم الإنترنت حسب إعداداتها؛ التطبيق نفسه بلا إذن إنترنت.";}
 public int languageAvailability(String l,String c,String v){return TextToSpeech.LANG_NOT_SUPPORTED;}
 public boolean acceptsText(String text){
  if(text==null||text.trim().isEmpty())return false;
  for(int i=0;i<text.length();){int cp=text.codePointAt(i);i+=Character.charCount(cp);if(Character.isISOControl(cp)&&cp!='\n'&&cp!='\r'&&cp!='\t')return false;}
  return true;
 }
 public void initialize(Context c){app=c.getApplicationContext()!=null?c.getApplicationContext():c;}

 /** Installed TTS engines, without Simple TTS itself. Needs the TTS_SERVICE query declared in the manifest. */
 public static List<EngineInfo> engines(Context c){
  ArrayList<EngineInfo> out=new ArrayList<>();PackageManager pm=c.getPackageManager();
  List<ResolveInfo> list=pm.queryIntentServices(new Intent("android.intent.action.TTS_SERVICE"),0);
  for(ResolveInfo r:list){
   if(r.serviceInfo==null)continue;String pkg=r.serviceInfo.packageName,label=String.valueOf(r.loadLabel(pm));
   if(pkg.equals(c.getPackageName())||label.contains("Simple TTS"))continue;
   boolean dup=false;for(EngineInfo e:out)if(e.pkg.equals(pkg))dup=true;if(!dup)out.add(new EngineInfo(pkg,label));
  }
  Collections.sort(out,(a,b)->a.label.compareToIgnoreCase(b.label));return out;
 }
 public String currentEngine(){return boundPackage;}
 /** Binds to the engine and lists its installed voices. Blocking: never call on the main thread. Returns false on failure. */
 public boolean load(Context c,String pkg){
  initialize(c);
  synchronized(LOCK){
   if(pkg.equals(boundPackage)&&tts!=null&&!voices.isEmpty())return true;
   if(tts!=null){try{tts.shutdown();}catch(RuntimeException ignored){}tts=null;}
   boundPackage="";voices=new ArrayList<>();ids=new String[0];labels=new String[0];engineRate=0;
   final CountDownLatch ready=new CountDownLatch(1);final int[] status={TextToSpeech.ERROR};
   TextToSpeech created=new TextToSpeech(app,s->{status[0]=s;ready.countDown();},pkg);
   try{if(!ready.await(15,TimeUnit.SECONDS)||status[0]!=TextToSpeech.SUCCESS){created.shutdown();return false;}}catch(InterruptedException e){created.shutdown();Thread.currentThread().interrupt();return false;}
   Set<Voice> set;try{set=created.getVoices();}catch(RuntimeException e){set=null;}
   ArrayList<Voice> list=new ArrayList<>();
   if(set!=null)for(Voice v:set)if(!v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))list.add(v);
   if(list.isEmpty()){Voice d=null;try{d=created.getDefaultVoice();}catch(RuntimeException ignored){}if(d!=null)list.add(d);}
   if(list.isEmpty()){created.shutdown();return false;}
   Collections.sort(list,(a,b)->{int x=a.getLocale().getDisplayName().compareToIgnoreCase(b.getLocale().getDisplayName());return x!=0?x:a.getName().compareToIgnoreCase(b.getName());});
   String[] i=new String[list.size()],l=new String[list.size()];
   for(int k=0;k<i.length;k++){Voice v=list.get(k);i[k]=v.getName();l[k]=v.getName()+" — "+v.getLocale().getDisplayName()+(v.isNetworkConnectionRequired()?" — network":"");}
   tts=created;boundPackage=pkg;voices=list;ids=i;labels=l;return true;
  }
 }
 public void release(){synchronized(LOCK){if(tts!=null){try{tts.shutdown();}catch(RuntimeException ignored){}tts=null;}boundPackage="";voices=new ArrayList<>();ids=new String[0];labels=new String[0];}}

 public int sampleRate(int voice){
  if(engineRate>0)return engineRate;
  try{short[] probe=synthesize("1",voice,1f,1f);if(probe!=null&&engineRate>0)return engineRate;}catch(RuntimeException ignored){}
  return 22050;
 }
 public short[] synthesize(String text,int voice,float rate,float pitch){
  TextToSpeech t;Voice v;synchronized(LOCK){t=tts;ArrayList<Voice> list=voices;if(t==null||voice<0||voice>=list.size())throw new IllegalStateException("System voice is not loaded");v=list.get(voice);}
  File wav=null;
  try{
   wav=File.createTempFile("system-tts-",".wav",app.getCacheDir());
   final CountDownLatch done=new CountDownLatch(1);final boolean[] ok={false};
   final String uid="s"+System.nanoTime();
   synchronized(t){
    t.setOnUtteranceProgressListener(new UtteranceProgressListener(){
     public void onStart(String u){}
     public void onDone(String u){if(uid.equals(u)){ok[0]=true;done.countDown();}}
     @SuppressWarnings("deprecation") public void onError(String u){if(uid.equals(u))done.countDown();}
     public void onError(String u,int code){if(uid.equals(u))done.countDown();}
    });
    if(t.setVoice(v)!=TextToSpeech.SUCCESS)throw new IllegalStateException("System voice rejected");
    t.setSpeechRate(Math.max(.25f,Math.min(4f,rate)));t.setPitch(Math.max(.5f,Math.min(2f,pitch)));
    if(t.synthesizeToFile(text,new Bundle(),wav,uid)!=TextToSpeech.SUCCESS)throw new IllegalStateException("System engine refused the request");
   }
   if(!done.await(90,TimeUnit.SECONDS)||!ok[0])throw new IllegalStateException("System engine failed or timed out");
   return readWav(wav);
  }catch(IOException e){throw new IllegalStateException("System engine output unreadable",e);}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException();}
  finally{if(wav!=null)wav.delete();}
 }
 /** Reads 16-bit PCM WAV (mono or stereo, down-mixed); tolerant of streaming headers with unknown sizes. */
 private short[] readWav(File f)throws IOException {
  byte[] all=new byte[(int)Math.min(f.length(),64L<<20)];try(DataInputStream in=new DataInputStream(new FileInputStream(f))){in.readFully(all);}
  ByteBuffer b=ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
  if(all.length<44||b.getInt(0)!=0x46464952||b.getInt(8)!=0x45564157)throw new IOException("Not a WAV file");
  int pos=12,channels=1,rate=0,bits=16,format=1,dataAt=-1,dataLen=0;
  while(pos+8<=all.length){
   int tag=b.getInt(pos),size=b.getInt(pos+4);
   if(tag==0x20746d66){format=b.getShort(pos+8)&0xffff;channels=b.getShort(pos+10)&0xffff;rate=b.getInt(pos+12);bits=b.getShort(pos+22)&0xffff;}
   else if(tag==0x61746164){dataAt=pos+8;dataLen=(size<=0||dataAt+size>all.length)?all.length-dataAt:size;break;}
   if(size<0||pos+8L+size>all.length)break;pos+=8+size+(size&1);
  }
  if(dataAt<0||format!=1||bits!=16||channels<1||channels>2||rate<8000||rate>96000)throw new IOException("Unsupported WAV format");
  int frames=dataLen/(2*channels);short[] pcm=new short[frames];
  for(int i=0;i<frames;i++){int s=b.getShort(dataAt+i*2*channels);if(channels==2)s=(s+b.getShort(dataAt+i*4+2))/2;pcm[i]=(short)s;}
  if(engineRate==0)engineRate=rate;
  if(rate!=engineRate){ // keep the reported sample rate stable inside one text
   int n=(int)((long)frames*engineRate/rate);short[] out=new short[n];for(int i=0;i<n;i++){double x=(double)i*rate/engineRate;int k=(int)x;double fr=x-k;int a=pcm[Math.min(k,frames-1)],c=pcm[Math.min(k+1,frames-1)];out[i]=(short)Math.round(a+(c-a)*fr);}return out;
  }
  return pcm;
 }
}
