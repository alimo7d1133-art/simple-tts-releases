/* Old TTS registry. Register only engines actually included and licensed. */
package org.retro.flitetts;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.*;

public final class EngineRegistry {
 private static final List<EnginePlugin> ENGINES=Collections.unmodifiableList(Arrays.asList(new FlitePlugin(),new PicoPlugin(),new RsynthPlugin(),new MbrolaPlugin(),new GnuspeechPlugin(),new KlattPlugin(),new FreeTtsPlugin(),new FestivalPlugin(),new OpenJtalkPlugin(),new EkhoPlugin(),new FliteHtsPlugin(),new SystemTtsPlugin()));
 private static final Set<String> PREPARED=new HashSet<>();
 public static List<EnginePlugin> all(){return ENGINES;}
 public static synchronized void prepare(Context context,EnginePlugin engine){
  if(PREPARED.contains(engine.id()))return;
  engine.initialize(context.getApplicationContext());PREPARED.add(engine.id());
 }
 public static EnginePlugin find(String id){for(EnginePlugin e:ENGINES)if(e.id().equals(id))return e;return null;}
 public static EnginePlugin preferred(Context c){EnginePlugin e=find(c.getSharedPreferences("retro",Context.MODE_PRIVATE).getString("engine","flite"));return e!=null?e:ENGINES.get(0);}
 public static int preferredVoice(Context c,EnginePlugin e){return Math.max(0,Math.min(e.voiceIds().length-1,c.getSharedPreferences("retro",Context.MODE_PRIVATE).getInt("voice:"+e.id(),0)));}
 public static String voiceName(EnginePlugin e,int voice){return e.id()+":"+e.voiceIds()[voice];}
 private static boolean sameLanguage(Locale locale,String language){return locale.getLanguage().equalsIgnoreCase(language)||locale.getISO3Language().equalsIgnoreCase(language);}
 private static boolean sameCountry(Locale locale,String country){return country==null||country.isEmpty()||locale.getCountry().equalsIgnoreCase(country)||locale.getISO3Country().equalsIgnoreCase(country);}
 public static int voiceForLanguage(Context c,EnginePlugin e,String language,String country,String variant){
  if(e.languageAvailability(language,country,variant)<0)return -1;
  int preferred=preferredVoice(c,e);
  if(sameLanguage(e.voiceLocale(preferred),language)&&sameCountry(e.voiceLocale(preferred),country))return preferred;
  for(int i=0;i<e.voiceIds().length;i++)if(sameLanguage(e.voiceLocale(i),language)&&sameCountry(e.voiceLocale(i),country))return i;
  if(sameLanguage(e.voiceLocale(preferred),language))return preferred;
  for(int i=0;i<e.voiceIds().length;i++)if(sameLanguage(e.voiceLocale(i),language))return i;
  return -1;
 }
 public static Selection resolve(String name){
  if(name==null)return null;
  for(EnginePlugin e:ENGINES){String[] ids=e.voiceIds();for(int i=0;i<ids.length;i++)if(voiceName(e,i).equals(name))return new Selection(e,i);}
  return null;
 }
 public static final class Selection {public final EnginePlugin engine;public final int voice;Selection(EnginePlugin e,int v){engine=e;voice=v;}}
 private EngineRegistry(){}
}
