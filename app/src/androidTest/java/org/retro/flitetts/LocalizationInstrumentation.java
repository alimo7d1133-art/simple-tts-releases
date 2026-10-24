package org.retro.flitetts;
import android.app.*;import android.content.*;import android.content.res.*;import android.os.*;import android.view.*;import android.widget.*;import java.util.*;
/** Real localized Activity/resources checks, not a human screen-reader test. */
public class LocalizationInstrumentation extends Instrumentation {
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){Bundle result=new Bundle();Activity initial=null,switched=null;try{
  Context target=getTargetContext();String[] tags={"en","ar","de","es","fr","it"};String[] keys={"title","intro","choose_engine","choose_voice","interface_language","speak","stop","restore","save_mp3","standalone_notice","generating","saving_mp3","mp3_saved","save_failed","save_cancelled","busy","copyrights","engine_openjtalk","notice_openjtalk"};
  Configuration english=new Configuration(target.getResources().getConfiguration());english.setLocale(Locale.ENGLISH);String englishSpeak=target.createConfigurationContext(english).getString(R.string.speak);
  for(String tag:tags){
   UiLocale.save(target,tag);Context localized=UiLocale.wrap(target);
   require(UiLocale.localeFor(target).getLanguage().equals(tag),"Locale preference not saved: "+tag);
   require(localized.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(tag),"Locale not applied: "+tag);
   require(localized.getResources().getConfiguration().getLayoutDirection()==("ar".equals(tag)?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR),"Direction: "+tag);
   for(String key:keys){int id=localized.getResources().getIdentifier(key,"string",target.getPackageName());require(id!=0&&!localized.getString(id).isEmpty(),tag+" missing "+key);}
   if(!"en".equals(tag))require(!localized.getString(R.string.speak).equals(englishSpeak),"Untranslated speak label: "+tag);
  }
  require(EngineRegistry.all().size()==11,"Expected 11 engines");int voices=0;for(EnginePlugin e:EngineRegistry.all())voices+=e.voiceIds().length;require(voices==63,"Expected 63 voices");
  UiLocale.save(target,UiLocale.SYSTEM);initial=startActivitySync(new Intent(target,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();final Activity original=initial;final Spinner[] selector={null};runOnMainSync(()->selector[0]=findSpinner(original.getWindow().getDecorView(),original.getString(R.string.interface_language_desc)));
  require(selector[0]!=null&&selector[0].getAdapter().getCount()==7,"Expected system default plus six language choices");
  ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(()->selector[0].setSelection(2));switched=monitor.waitForActivityWithTimeout(20000);removeMonitor(monitor);require(switched!=null,"Language selection did not recreate Activity");waitForIdleSync();
  require("ar".equals(target.getSharedPreferences(UiLocale.PREFS,Context.MODE_PRIVATE).getString(UiLocale.KEY,"")),"Spinner preference not persisted");require(switched.getResources().getConfiguration().getLayoutDirection()==View.LAYOUT_DIRECTION_RTL,"Arabic Activity is not RTL");
  UiLocale.save(target,UiLocale.SYSTEM);String system=Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();boolean supported=Arrays.asList(tags).contains(system);require(UiLocale.localeFor(target).getLanguage().equals(supported?system:"en"),"System default not restored");
  result.putString("stream","PASS six actual translations, saved preferences, RTL/LTR, live language Spinner recreation, system default restoration, eleven engines and 63 voices. No human TalkBack claim.\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable failure){result.putString("error",failure.toString());finish(Activity.RESULT_CANCELED,result);}finally{UiLocale.save(getTargetContext(),UiLocale.SYSTEM);final Activity a=initial,b=switched;runOnMainSync(()->{if(a!=null)a.finish();if(b!=null&&b!=a)b.finish();});}}
 private Spinner findSpinner(View view,String description){if(view instanceof Spinner&&description.equals(String.valueOf(view.getContentDescription())))return (Spinner)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Spinner found=findSpinner(((ViewGroup)view).getChildAt(i),description);if(found!=null)return found;}return null;}
}
