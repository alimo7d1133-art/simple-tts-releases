package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Release identity and registry-resource integration; not a human accessibility audit. */
public final class SimpleTtsInstrumentation extends Instrumentation {
 private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{
  Context c=getTargetContext();PackageManager pm=c.getPackageManager();
  PackageInfo info=pm.getPackageInfo(c.getPackageName(),PackageManager.GET_PERMISSIONS);
  require("org.oldtts.android".equals(c.getPackageName()),"Upgrade package changed");
  require(info.getLongVersionCode()==11&&"1.8.0".equals(info.versionName),"Wrong release version");
  for(String permission:info.requestedPermissions==null?new String[0]:info.requestedPermissions){
   require(!permission.equals("android.permission.INTERNET")&&!permission.equals("android.permission.RECORD_AUDIO"),"Unexpected permission "+permission);
  }
  int voices=0;Set<String> engineIds=new HashSet<>(),voiceIds=new HashSet<>();
  for(EnginePlugin e:EngineRegistry.all()){
   require(engineIds.add(e.id()),"Duplicate engine id "+e.id());
   require(e.voiceIds().length>0&&e.voiceIds().length==e.voiceLabels().length,"Voice metadata "+e.id());
   for(int i=0;i<e.voiceIds().length;i++){
    require(voiceIds.add(EngineRegistry.voiceName(e,i)),"Duplicate voice id");
    require(EngineRegistry.resolve(EngineRegistry.voiceName(e,i))!=null,"Voice resolution");
    require(e.acceptsText(e.sampleText(i),i),"Invalid sample "+e.id()+":"+i);voices++;
   }
   for(String asset:e.noticeAssets())try(InputStream in=c.getAssets().open(asset)){require(in.read()!=-1,"Empty license "+asset);}
  }
  for(String tag:new String[]{"en","ar","de","es","fr","it"}){
   Configuration config=new Configuration(c.getResources().getConfiguration());config.setLocale(Locale.forLanguageTag(tag));Context l=c.createConfigurationContext(config);
   require("Simple TTS".equals(l.getString(R.string.app_name)),"Wrong localized name "+tag);
   require("Simple TTS".equals(l.getString(R.string.title)),"Wrong title "+tag);
   require("Simple TTS".equals(l.getResources().getString(c.getApplicationInfo().labelRes)),"Wrong launcher label "+tag);
   require(l.getString(R.string.engine_festival).contains("HTS")&&l.getString(R.string.engine_festival).contains("LPC"),"Festival English description absent "+tag);
   for(EnginePlugin e:EngineRegistry.all())for(String prefix:new String[]{"engine_","notice_"}){
    int id=l.getResources().getIdentifier(prefix+e.id(),"string",c.getPackageName());
    require(id!=0&&!l.getString(id).trim().isEmpty(),"Missing UI resource "+prefix+e.id()+" "+tag);
   }
  }
  out.putString("stream","PASS Simple TTS 1.8.0 identity, package/version, six localized labels, complete engine UI resources, unique registry IDs, sample validation, bundled license assets, no internet/microphone; engines="+engineIds.size()+" voices="+voices+"\n");finish(Activity.RESULT_OK,out);
 }catch(Throwable t){out.putString("error",t.toString());finish(Activity.RESULT_CANCELED,out);}}
}
