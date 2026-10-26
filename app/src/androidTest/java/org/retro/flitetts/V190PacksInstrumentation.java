package org.retro.flitetts;
import android.content.Context;
import android.os.Bundle;
import android.app.Instrumentation;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/** 1.9.0: downloadable voice packs (verification, activation, tamper rejection, deletion) and system-engine voices.
 *  The host script first unzips the real release ZIPs into "Simple TTS data" and tampers one pack. */
public final class V190PacksInstrumentation extends Instrumentation {
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 private void note(String m){Bundle b=new Bundle();b.putString("stream",m+"\n");sendStatus(1,b);}
 private static long energy(short[] s){long e=0;for(short v:s)e+=Math.abs(v);return e;}
 private PackStore.Status st(List<PackStore.Status> l,String id){for(PackStore.Status s:l)if(s.pack.id.equals(id))return s;throw new AssertionError("no status "+id);}
 private void renders(Context c,String engineId,String voiceId,String text)throws Exception{
  EnginePlugin e=EngineRegistry.find(engineId);int v=Arrays.asList(e.voiceIds()).indexOf(voiceId);require(v>=0,"voice "+voiceId);
  require(e.voiceAvailable(c,v),"voice not available "+voiceId);
  StandaloneAudio.Pcm p=StandaloneAudio.render(c,e,v,text,1f,1f,false,false,()->false);
  require(p.samples.length>4000&&energy(p.samples)>20000,"empty audio from "+voiceId);note("PASS render "+engineId+":"+voiceId+" samples="+p.samples.length+" hz="+p.sampleRate);
 }
 private void missing(Context c,String engineId,String voiceId,String pack)throws Exception{
  EnginePlugin e=EngineRegistry.find(engineId);int v=Arrays.asList(e.voiceIds()).indexOf(voiceId);require(v>=0,"voice "+voiceId);
  require(!e.voiceAvailable(c,v),"voice should be unavailable "+voiceId);require(pack.equals(e.requiredPack(v)),"requiredPack of "+voiceId+" = "+e.requiredPack(v));
  try{StandaloneAudio.render(c,e,v,e.sampleText(v),1f,1f,false,false,()->false);throw new AssertionError("render must fail without pack: "+voiceId);}
  catch(PackStore.PackMissingException ok){require(pack.equals(ok.pack),"exception pack "+ok.pack);}
  note("PASS unavailable-without-pack "+engineId+":"+voiceId+" -> "+pack);
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();PackStore.init(c);
  require(Mp3Folder.permitted(c),"storage permission must be granted by the host script");
  List<PackStore.Pack> cat=PackStore.catalog(c);require(cat.size()==14,"catalog size "+cat.size());note("catalog packs="+cat.size());
  // 1. simulate a fresh install: remove every pack
  for(PackStore.Pack p:cat){PackStore.remove(c,p.id);require(!PackStore.complete(c,p.id),"still complete "+p.id);}
  note("PASS all packs removed (fresh-install state)");
  // base voices stay available without any pack
  EnginePlugin flite=EngineRegistry.find("flite"),mb=EngineRegistry.find("mbrola");
  renders(c,"flite","asc","\u0645\u0631\u062d\u0628\u0627");
  missing(c,"flite","hin_ab","flite-indic-hi");
  missing(c,"mbrola","en1","mbrola-english");
  missing(c,"openjtalk","ja-JP-nitech","openjtalk-ja");
  missing(c,"ekho",EngineRegistry.find("ekho").voiceIds()[0],"ekho-cantonese");
  // 2. scan: the host placed valid folders for most packs and a tampered one for flite-indic-bn
  List<PackStore.Status> r=PackStore.refresh(c,null);
  for(PackStore.Status s:r)note("status "+s.pack.id+" state="+s.state+" "+s.detail);
  for(String id:new String[]{"flite-indic-hi","mbrola-english","openjtalk-ja","ekho-cantonese"})require(st(r,id).state==PackStore.ACTIVE&&PackStore.complete(c,id),"not activated "+id);
  require(st(r,"flite-indic-bn").state==PackStore.INVALID,"tampered pack must be rejected: "+st(r,"flite-indic-bn").state);
  require(!PackStore.complete(c,"flite-indic-bn"),"tampered pack must not be activated");
  File extra=new File(c.getFilesDir(),"flite-extra");String[] left=extra.list();for(String n:left==null?new String[0]:left)require(!n.endsWith(".new"),"temporary file left: "+n);
  require(st(r,"flite-indic-gu").state==PackStore.MISSING,"absent pack should be MISSING");
  note("PASS tampered pack rejected, valid packs activated, no temp files");
  // 3. activated voices work
  renders(c,"flite","hin_ab","\u0928\u092e\u0938\u094d\u0924\u0947, \u092f\u0939 \u090f\u0915 \u0928\u0908 \u0906\u0935\u093e\u091c\u093c \u0939\u0948\u0964");
  renders(c,"mbrola","en1","Hello, this is a voice pack test.");
  renders(c,"openjtalk","ja-JP-nitech","\u3053\u3093\u306b\u3061\u306f\u3002");
  renders(c,"ekho",EngineRegistry.find("ekho").voiceIds()[0],"\u4f60\u597d\u3002");
  // 4. repeated scan is idempotent and cheap
  List<PackStore.Status> again=PackStore.refresh(c,null);require(st(again,"flite-indic-hi").state==PackStore.ACTIVE,"second scan");
  // 5. delete one pack; its voices disappear, other packs and the user's folder stay
  PackStore.remove(c,"flite-indic-hi");missing(c,"flite","hin_ab","flite-indic-hi");
  require(new File(PackStore.dataRoot(),"flite-indic-hi/manifest.json").isFile(),"user folder must be untouched");
  renders(c,"mbrola","us1","Hello again.");
  // 6. re-activate from the untouched folder
  PackStore.refresh(c,null);require(PackStore.complete(c,"flite-indic-hi"),"re-activation after delete");
  renders(c,"flite","hin_ab","\u0928\u092e\u0938\u094d\u0924\u0947");
  // 7. system engines (Android TextToSpeech): use an installed engine other than Simple TTS, if the device has one
  SystemTtsPlugin sys=(SystemTtsPlugin)EngineRegistry.find("system");List<SystemTtsPlugin.EngineInfo> list=SystemTtsPlugin.engines(c);
  for(SystemTtsPlugin.EngineInfo e:list)require(!e.pkg.equals(c.getPackageName()),"Simple TTS listed as system engine");
  note("system engines on device: "+list.size());
  if(list.isEmpty())note("SKIP system voice render: no other TTS engine installed on this device");
  else{
   boolean done=false;
   for(SystemTtsPlugin.EngineInfo e:list){
    if(!sys.load(c,e.pkg)){note("engine "+e.label+" ("+e.pkg+"): could not load");continue;}
    String[] ids=sys.voiceIds();note("engine "+e.label+" voices="+ids.length);
    int pick=0;for(int i=0;i<ids.length;i++)if(sys.voiceLocale(i).getLanguage().equals("en")){pick=i;break;}
    try{
     StandaloneAudio.Pcm p=StandaloneAudio.render(c,sys,pick,"Hello. This is a system voice test.",1f,1f,false,false,()->false);
     StandaloneAudio.Pcm f=StandaloneAudio.render(c,sys,pick,"Hello. This is a system voice test.",1.6f,1f,false,false,()->false);
     require(p.samples.length>2000&&energy(p.samples)>5000,"silent system voice");
     note("PASS system voice "+e.pkg+":"+ids[pick]+" samples="+p.samples.length+" fast="+f.samples.length+" hz="+p.sampleRate);done=true;break;
    }catch(RuntimeException ex){note("engine "+e.pkg+" synthesis failed: "+ex);}
   }
   require(done,"no installed system engine could synthesize");
  }
  result.putString("stream","V190-PACKS-ALL-PASS\n");finish(android.app.Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("stream","FAIL "+t+"\n");finish(android.app.Activity.RESULT_CANCELED,result);}}
}
