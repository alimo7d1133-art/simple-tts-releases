package org.retro.flitetts;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

/** Applies the user's interface locale without changing speech/sample data. */
public final class UiLocale {
 public static final String PREFS="retro", KEY="ui_language", SYSTEM="system";
 public static final String[] TAGS={"system","en","ar","de","es","fr","it"};
 private UiLocale(){}
 public static Locale localeFor(Context context){
  String value=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,SYSTEM);
  if(value==null||SYSTEM.equals(value)){
   Locale system;
   if(Build.VERSION.SDK_INT>=24) system=Resources.getSystem().getConfiguration().getLocales().get(0);
   else system=Resources.getSystem().getConfiguration().locale;
   String lang=system==null?"en":system.getLanguage();
   for(int i=1;i<TAGS.length;i++) if(TAGS[i].equalsIgnoreCase(lang)) return new Locale(lang);
   return Locale.ENGLISH;
  }
  for(int i=1;i<TAGS.length;i++) if(TAGS[i].equalsIgnoreCase(value)) return new Locale(value);
  return Locale.ENGLISH;
 }
 public static Context wrap(Context base){
  Configuration c=new Configuration(base.getResources().getConfiguration());
  Locale l=localeFor(base); c.setLocale(l); c.setLayoutDirection(l);
  return base.createConfigurationContext(c);
 }
 public static void save(Context context,String tag){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,tag).apply();}
 public static String tag(Locale l){return l.getLanguage();}
}
