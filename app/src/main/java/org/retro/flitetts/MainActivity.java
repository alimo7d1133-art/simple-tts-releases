/* Simple TTS accessible UI, AGPL-3.0-or-later; see LICENSE. */
package org.retro.flitetts;
import android.app.*;
import android.os.*;
import android.content.*;
import android.media.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
 private boolean voiceSelectionReady,busy;
 private volatile boolean destroyed;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final AtomicInteger generation=new AtomicInteger();
 private Future<?> task;
 private AudioManager audioManager;
 private AudioFocusRequest focus;
 private boolean storageRequestActive,pendingStorageSave,storageScreenPaused;
 private static final int STORAGE_ACCESS=201;
 private final BroadcastReceiver noisy=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(i.getAction()))stopAudio();}};
 private TextView status,languageInfo;
 private EditText input;
 private Spinner voices,engines,localeSpinner;
 private EnginePlugin engine;
 private Switch rawiToggle,highQualityToggle;
 private TextView rawiHint;
 private SeekBar speed,pitch;
 private LinearLayout column;
 private int[] voiceMap=new int[0];
 private Spinner systemEngines;private TextView systemEngineLabel,systemInfo,packHint;
 private List<SystemTtsPlugin.EngineInfo> systemList=new ArrayList<>();
 private final ExecutorService packWorker=Executors.newSingleThreadExecutor();
 private volatile boolean scanning;
 private List<PackStore.Status> lastStatuses;
 private AlertDialog packsDialog;private LinearLayout packsBody;
 private int systemSelectionToken;
 @Override protected void attachBaseContext(Context base){super.attachBaseContext(UiLocale.wrap(base));}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);Locale ui=UiLocale.localeFor(this);
  if(state!=null){storageRequestActive=state.getBoolean("storage_request_active");pendingStorageSave=state.getBoolean("pending_storage_save");storageScreenPaused=storageRequestActive;}
  Mp3Folder.forgetLegacyGrant(this);
  audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
  ScrollView scroll=new ScrollView(this);column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(24,24,24,32);column.setLayoutDirection("ar".equals(ui.getLanguage())?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(column);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  TextView title=label(R.string.title);title.setTextSize(25);if(Build.VERSION.SDK_INT>=28)title.setAccessibilityHeading(true);label(R.string.intro);label(R.string.standalone_notice);label(R.string.arabic_notice);
  TextView uiLabel=label(R.string.interface_language);localeSpinner=new Spinner(this);localeSpinner.setId(View.generateViewId());localeSpinner.setContentDescription(getString(R.string.interface_language_desc));uiLabel.setLabelFor(localeSpinner.getId());
  String[] localeLabels=new String[UiLocale.TAGS.length];localeLabels[0]=getString(R.string.system_default);for(int i=1;i<localeLabels.length;i++){Locale self=new Locale(UiLocale.TAGS[i]);localeLabels[i]=self.getDisplayName(self);}localeSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,localeLabels));
  String current=getSharedPreferences(UiLocale.PREFS,MODE_PRIVATE).getString(UiLocale.KEY,UiLocale.SYSTEM);int languagePosition=0;for(int i=0;i<UiLocale.TAGS.length;i++)if(UiLocale.TAGS[i].equals(current))languagePosition=i;localeSpinner.setSelection(languagePosition);column.addView(localeSpinner);
  localeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){String tag=UiLocale.TAGS[position];if(!tag.equals(getSharedPreferences(UiLocale.PREFS,MODE_PRIVATE).getString(UiLocale.KEY,UiLocale.SYSTEM))){UiLocale.save(MainActivity.this,tag);recreate();}}});
  PackStore.init(this);engine=EngineRegistry.preferred(this);TextView engineLabel=label(R.string.choose_engine);engines=new Spinner(this);engines.setId(View.generateViewId());engines.setContentDescription(getString(R.string.engine));engineLabel.setLabelFor(engines.getId());
  String[] engineLabels=new String[EngineRegistry.all().size()];int selected=0;for(int i=0;i<engineLabels.length;i++){EnginePlugin e=EngineRegistry.all().get(i);engineLabels[i]=engineName(e);if(e.id().equals(engine.id()))selected=i;}engines.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,engineLabels));engines.setSelection(selected);column.addView(engines);
  languageInfo=label(noticeKey(engine));
  systemEngineLabel=label(R.string.system_engine_label);systemEngines=new Spinner(this);systemEngines.setId(View.generateViewId());systemEngines.setContentDescription(getString(R.string.system_engine_label));systemEngineLabel.setLabelFor(systemEngines.getId());column.addView(systemEngines);systemInfo=label(R.string.system_info);
  TextView voiceLabel=label(R.string.choose_voice);voices=new Spinner(this);voices.setId(View.generateViewId());voices.setContentDescription(getString(R.string.voice));voiceLabel.setLabelFor(voices.getId());column.addView(voices);packHint=label(R.string.voices_unavailable_hint);packHint.setVisibility(View.GONE);resetVoiceChoices(null);updateSystemVisibility();if(engine.id().equals("system"))setupSystemEngine();
  rawiToggle=new Switch(this);rawiToggle.setText(R.string.rawi_toggle);rawiToggle.setContentDescription(getString(R.string.rawi_toggle));rawiToggle.setMinHeight((int)(48*getResources().getDisplayMetrics().density));rawiToggle.setChecked(getSharedPreferences("retro",MODE_PRIVATE).getBoolean("rawi_enabled",false));rawiToggle.setOnCheckedChangeListener((button,checked)->getSharedPreferences("retro",MODE_PRIVATE).edit().putBoolean("rawi_enabled",checked).apply());column.addView(rawiToggle,new LinearLayout.LayoutParams(-1,-2));rawiHint=label(R.string.rawi_hint);updateRawiVisibility();
  highQualityToggle=new Switch(this);highQualityToggle.setText(R.string.hq_toggle);highQualityToggle.setContentDescription(getString(R.string.hq_toggle));highQualityToggle.setMinHeight((int)(48*getResources().getDisplayMetrics().density));highQualityToggle.setChecked(getSharedPreferences("retro",MODE_PRIVATE).getBoolean("high_quality_48000",false));highQualityToggle.setOnCheckedChangeListener((button,checked)->getSharedPreferences("retro",MODE_PRIVATE).edit().putBoolean("high_quality_48000",checked).apply());column.addView(highQualityToggle,new LinearLayout.LayoutParams(-1,-2));label(R.string.hq_hint);
  speed=slider(R.string.speed,"speed_slider_140","rate_percent",25f,400f);pitch=slider(R.string.pitch,"pitch_slider_140","pitch_percent",50f,200f);
  TextView textLabel=label(R.string.sample_text);input=new EditText(this);input.setId(View.generateViewId());textLabel.setLabelFor(input.getId());input.setContentDescription(getString(R.string.sample_hint));input.setText(state!=null&&state.containsKey("trial_text")?state.getString("trial_text"):engine.sampleText(currentVoice()));input.setMinLines(3);input.setGravity(Gravity.TOP|Gravity.START);input.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);column.addView(input,new LinearLayout.LayoutParams(-1,-2));
  label(R.string.folder_hint);
  status=label(R.string.ready);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  busy=false;
  button(R.string.diacritize_text,this::diacritizeText);
  button(R.string.speak,()->startAudio(false));button(R.string.save_mp3,()->startAudio(true));button(R.string.stop,this::stopAudio);button(R.string.restore,()->input.setText(engine.sampleText(currentVoice())));button(R.string.voice_packs,this::showPacks);button(R.string.copyrights,this::noticesDialog);
  voices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){if(position>=voiceMap.length)return;int realVoice=voiceMap[position];getSharedPreferences("retro",MODE_PRIVATE).edit().putInt("voice:"+engine.id(),realVoice).apply();updateRawiVisibility();if(!voiceSelectionReady){voiceSelectionReady=true;return;}if(input!=null)input.setText(engine.sampleText(realVoice));}});
  engines.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){EnginePlugin next=EngineRegistry.all().get(position);if(next.id().equals(engine.id()))return;stopAudio();engine=next;getSharedPreferences("retro",MODE_PRIVATE).edit().putString("engine",engine.id()).apply();resetVoiceChoices(null);updateSystemVisibility();if(engine.id().equals("system"))setupSystemEngine();languageInfo.setText(noticeKey(engine));input.setText(engine.sampleText(currentVoice()));status.setText(getString(R.string.engine_selected,engineName(engine)));}});
  column.post(()->{if(!destroyed&&!storageRequestActive){if(Mp3Folder.permitted(this)){initializeStorage();scanPacks(false);}else if(!getSharedPreferences("retro",MODE_PRIVATE).getBoolean("storage_intro_131",false))showStorageIntro();}});
 }
 private void postStatus(int token,int resource,boolean finished){runOnUiThread(()->{if(!destroyed&&generation.get()==token){status.setText(resource);if(finished){busy=false;abandonFocus();}}});}
 private String engineName(EnginePlugin e){return getString(getResources().getIdentifier("engine_"+e.id(),"string",getPackageName()));}
 private int noticeKey(EnginePlugin e){return getResources().getIdentifier("notice_"+e.id(),"string",getPackageName());}
 private int currentVoice(){if(voices==null||voiceMap.length==0)return 0;return voiceMap[Math.max(0,Math.min(voiceMap.length-1,voices.getSelectedItemPosition()))];}
 private void updateRawiVisibility(){if(rawiToggle==null)return;boolean arabic="ar".equals(engine.voiceLocale(currentVoice()).getLanguage());rawiToggle.setVisibility(arabic?View.VISIBLE:View.GONE);rawiHint.setVisibility(arabic?View.VISIBLE:View.GONE);}
 private void resetVoiceChoices(Integer keep){
  voiceSelectionReady=false;String[] ids=engine.voiceIds(),all=engine.voiceLabels();ArrayList<Integer> ok=new ArrayList<>();
  for(int i=0;i<ids.length;i++)if(engine.voiceAvailable(this,i))ok.add(i);
  voiceMap=new int[ok.size()];for(int i=0;i<voiceMap.length;i++)voiceMap[i]=ok.get(i);
  Locale ui=UiLocale.localeFor(this);String[] names=new String[Math.max(1,voiceMap.length)];
  if(voiceMap.length==0)names[0]=getString(engine.id().equals("system")?R.string.system_no_voices:R.string.no_voices_available);
  for(int i=0;i<voiceMap.length;i++){int v=voiceMap[i];names[i]=engine.id().equals("system")?(v<all.length?all[v]:ids[v]):engineName(engine)+" — "+ids[v]+" — "+engine.voiceLocale(v).getDisplayName(ui);}
  voices.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
  int want=keep!=null?keep:EngineRegistry.preferredVoice(this,engine),at=0;for(int i=0;i<voiceMap.length;i++)if(voiceMap[i]==want)at=i;
  voices.setSelection(at);
  int missing=ids.length-voiceMap.length;if(packHint!=null){packHint.setVisibility(missing>0&&!engine.id().equals("system")?View.VISIBLE:View.GONE);packHint.setText(getString(R.string.voices_unavailable_hint,missing));}
 }
 private void updateSystemVisibility(){boolean sys=engine.id().equals("system");int vis=sys?View.VISIBLE:View.GONE;systemEngineLabel.setVisibility(vis);systemEngines.setVisibility(vis);systemInfo.setVisibility(vis);}
 private void setupSystemEngine(){
  systemList=SystemTtsPlugin.engines(this);final SystemTtsPlugin plugin=(SystemTtsPlugin)engine;
  if(systemList.isEmpty()){systemEngines.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{getString(R.string.system_none_installed)}));status.setText(R.string.system_none_installed);return;}
  String[] labels=new String[systemList.size()];String saved=getSharedPreferences("retro",MODE_PRIVATE).getString("system_engine","");int at=0;
  for(int i=0;i<labels.length;i++){labels[i]=systemList.get(i).label;if(systemList.get(i).pkg.equals(saved))at=i;}
  systemEngines.setOnItemSelectedListener(null);systemEngines.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));systemEngines.setSelection(at);
  systemEngines.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int position,long id){if(position<systemList.size())loadSystemEngine(plugin,systemList.get(position));}});
 }
 private void loadSystemEngine(SystemTtsPlugin plugin,SystemTtsPlugin.EngineInfo info){
  getSharedPreferences("retro",MODE_PRIVATE).edit().putString("system_engine",info.pkg).apply();
  if(info.pkg.equals(plugin.currentEngine())&&plugin.voiceIds().length>0){resetVoiceChoices(null);return;}
  final int token=++systemSelectionToken;status.setText(getString(R.string.system_loading,info.label));
  packWorker.submit(()->{boolean ok=plugin.load(getApplicationContext(),info.pkg);runOnUiThread(()->{if(destroyed||token!=systemSelectionToken)return;resetVoiceChoices(null);
   if(ok){input.setText(engine.sampleText(currentVoice()));status.setText(getString(R.string.system_loaded,info.label,plugin.voiceIds().length));}else status.setText(getString(R.string.system_failed,info.label));});});
 }
 private String packMissingMessage(String packId){PackStore.Pack p=PackStore.find(this,packId);return getString(R.string.pack_missing,p!=null?p.title:packId,p!=null?p.zip:packId,packId,PackStore.DATA_FOLDER);}
 private void scanPacks(boolean manual){
  if(scanning||destroyed)return;
  if(!Mp3Folder.permitted(this)){if(manual){status.setText(R.string.packs_need_storage);requestStorage(false);}return;}
  scanning=true;if(manual)status.setText(R.string.packs_scanning);final int before=countActive();
  packWorker.submit(()->{List<PackStore.Status> result;try{result=PackStore.refresh(getApplicationContext(),p->runOnUiThread(()->{if(!destroyed)status.setText(getString(R.string.packs_checking,p.id));}));}catch(RuntimeException failure){result=null;}
   final List<PackStore.Status> done=result;
   runOnUiThread(()->{scanning=false;if(destroyed)return;if(done!=null){lastStatuses=done;int bad=0;for(PackStore.Status st:done)if(st.state==PackStore.INVALID||st.state==PackStore.LOW_SPACE)bad++;
     int now=countActive();if(manual||now!=before||bad>0)status.setText(getString(R.string.packs_done,now,done.size(),bad));
     if(engine.id().equals("system")){}else resetVoiceChoices(currentVoice());}
    if(packsDialog!=null&&packsDialog.isShowing())fillPacks();});});
 }
 private int countActive(){int n=0;for(PackStore.Pack p:PackStore.catalog(this))if(PackStore.complete(this,p.id))n++;return n;}
 private PackStore.Status statusOf(PackStore.Pack p){
  if(lastStatuses!=null)for(PackStore.Status st:lastStatuses)if(st.pack.id.equals(p.id)&&(st.state!=PackStore.MISSING||!PackStore.complete(this,p.id)))return st;
  return null;
 }
 private void showPacks(){
  ScrollView sv=new ScrollView(this);packsBody=new LinearLayout(this);packsBody.setOrientation(LinearLayout.VERTICAL);packsBody.setPadding(24,16,24,16);sv.addView(packsBody);
  packsDialog=new AlertDialog.Builder(this).setTitle(R.string.voice_packs).setView(sv).setPositiveButton(R.string.packs_refresh,null).setNegativeButton(R.string.close,null).create();
  packsDialog.show();packsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->scanPacks(true));fillPacks();
 }
 private void fillPacks(){
  if(packsBody==null)return;packsBody.removeAllViews();
  TextView intro=new TextView(this);intro.setText(getString(R.string.packs_intro,PackStore.DATA_FOLDER));intro.setTextSize(16);intro.setTextIsSelectable(true);packsBody.addView(intro);
  if(scanning){TextView t=new TextView(this);t.setText(R.string.packs_scanning);t.setTextSize(16);t.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);packsBody.addView(t);}
  for(PackStore.Pack p:PackStore.catalog(this)){
   boolean installed=PackStore.complete(this,p.id);PackStore.Status st=statusOf(p);String line;
   if(installed&&(st==null||st.state==PackStore.ACTIVE||st.state==PackStore.MISSING))line=getString(R.string.pack_state_active);
   else if(st!=null&&st.state==PackStore.INVALID)line=getString(R.string.pack_state_invalid,st.detail);
   else if(st!=null&&st.state==PackStore.LOW_SPACE)line=getString(R.string.pack_state_space);
   else if(st!=null&&st.state==PackStore.NO_PERMISSION)line=getString(R.string.pack_state_permission);
   else line=getString(R.string.pack_state_missing,p.zip,p.id,PackStore.DATA_FOLDER);
   TextView t=new TextView(this);String sizes=getString(R.string.pack_sizes,p.zipBytes/1048576.0,p.installBytes/1048576.0);
   t.setText(p.title+"\n"+p.id+" — "+sizes+"\n"+line);t.setTextSize(16);t.setPadding(0,20,0,4);t.setTextIsSelectable(true);packsBody.addView(t);
   if(installed){Button del=new Button(this);del.setText(getString(R.string.pack_delete,p.id));del.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
    del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage(getString(R.string.pack_delete_confirm,p.title)).setPositiveButton(R.string.pack_delete_yes,(d,w)->{PackStore.remove(this,p.id);status.setText(getString(R.string.pack_deleted,p.id));if(lastStatuses!=null){ArrayList<PackStore.Status> keep=new ArrayList<>();for(PackStore.Status x:lastStatuses)if(!x.pack.id.equals(p.id))keep.add(x);lastStatuses=keep;}resetVoiceChoices(currentVoice());fillPacks();}).setNegativeButton(R.string.close,null).show());packsBody.addView(del);}
  }
 }
 private TextView label(int resource){TextView v=new TextView(this);v.setText(resource);v.setTextSize(18);v.setPadding(0,12,0,12);column.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
 static float speedFactor(int value){return (float)Math.pow(2.0,(Math.max(0,Math.min(100,value))-50)/25.0);}
 static float pitchFactor(int value){return (float)Math.pow(2.0,(Math.max(0,Math.min(100,value))-50)/50.0);}
 private SeekBar slider(int labelId,String key,String oldKey,float oldMin,float oldMax){
  android.content.SharedPreferences prefs=getSharedPreferences("retro",MODE_PRIVATE);
  int saved;
  if(prefs.contains(key))saved=prefs.getInt(key,50);
  else {int legacy=prefs.getInt(oldKey,prefs.getInt(labelId==R.string.speed?"سرعة الكلام":"طبقة الصوت",100));double factor=Math.max(oldMin,Math.min(oldMax,legacy))/100.0;saved=(int)Math.round(50+(labelId==R.string.speed?25:50)*Math.log(factor)/Math.log(2));prefs.edit().putInt(key,saved).apply();}
  saved=Math.max(0,Math.min(100,saved));String name=getString(labelId);TextView description=label(labelId);description.setText(getString(R.string.slider_value,name,saved));SeekBar bar=new SeekBar(this);bar.setId(View.generateViewId());description.setLabelFor(bar.getId());bar.setMin(0);bar.setMax(100);bar.setProgress(saved);bar.setContentDescription(getString(R.string.slider_value,name,saved));column.addView(bar,new LinearLayout.LayoutParams(-1,-2));
  bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int value,boolean fromUser){description.setText(getString(R.string.slider_value,name,value));bar.setContentDescription(getString(R.string.slider_value,name,value));getSharedPreferences("retro",MODE_PRIVATE).edit().putInt(key,value).apply();}});return bar;
 }

 private void diacritizeText(){
  if(busy||storageRequestActive){status.setText(R.string.busy);return;}
  String original=input.getText().toString();String text=original.trim();
  if(text.isEmpty()){status.setText(R.string.empty_text);return;}
  if(text.length()>StandaloneAudio.MAX_TEXT_LENGTH){status.setText(getString(R.string.text_too_long,StandaloneAudio.MAX_TEXT_LENGTH));return;}
  if(!ArabicFrontend.accepts(text)){status.setText(R.string.unsupported_text);return;}
  busy=true;int token=generation.incrementAndGet();status.setText(R.string.rawi_processing);
  task=worker.submit(()->{try{
   String result=RawiDiacritizer.diacritize(getApplicationContext(),original);
   if(Thread.currentThread().isInterrupted())return;
   runOnUiThread(()->{if(!destroyed&&generation.get()==token){if(input.getText().toString().equals(original)){input.setText(result);input.setSelection(input.length());status.setText(R.string.diacritize_done);}else status.setText(R.string.diacritize_text_changed);busy=false;}});
  }catch(CancellationException stopped){Thread.currentThread().interrupt();}catch(Exception|LinkageError failure){postStatus(token,R.string.rawi_failed,true);}});
 }
 private void button(int resource,Runnable action){Button b=new Button(this);b.setText(resource);b.setMinHeight((int)(48*getResources().getDisplayMetrics().density));b.setOnClickListener(v->action.run());column.addView(b,new LinearLayout.LayoutParams(-1,-2));}
 private void startAudio(boolean export){
  if(busy||storageRequestActive){status.setText(R.string.busy);return;}String text=input.getText().toString().trim();if(text.isEmpty()){status.setText(R.string.empty_text);return;}if(voiceMap.length==0){String need=engine.requiredPack(0);status.setText(need!=null?packMissingMessage(need):getString(R.string.system_no_voices));return;}
int selected=currentVoice();EnginePlugin chosen=engine;if(!chosen.acceptsText(text,selected)){status.setText(R.string.unsupported_text);return;}if(text.length()>StandaloneAudio.MAX_TEXT_LENGTH){status.setText(getString(R.string.text_too_long,StandaloneAudio.MAX_TEXT_LENGTH));return;}
  if(export&&!Mp3Folder.permitted(this)){requestStorage(true);return;}
  busy=true;int token=generation.incrementAndGet();float rate=speedFactor(speed.getProgress()),tone=pitchFactor(pitch.getProgress());boolean highQuality=highQualityToggle.isChecked();boolean useRawi=rawiToggle.isChecked()&&"ar".equals(chosen.voiceLocale(selected).getLanguage());status.setText(useRawi?R.string.rawi_processing:R.string.generating);
  task=worker.submit(()->{File file=null;try{
   StandaloneAudio.Cancellation cancel=()->destroyed||generation.get()!=token;
   StandaloneAudio.Pcm pcm=StandaloneAudio.render(getApplicationContext(),chosen,selected,text,rate,tone,useRawi,highQuality,cancel);StandaloneAudio.check(cancel);
   if(export){postStatus(token,R.string.saving_mp3,false);file=Mp3Folder.temporary(this,chosen.id()+"-"+chosen.voiceIds()[selected]);Mp3Encoder.encode(pcm.samples,pcm.sampleRate,file);StandaloneAudio.check(cancel);Mp3Folder.publish(this,file);file=null;postStatus(token,R.string.mp3_saved,true);}
   else{CountDownLatch focused=new CountDownLatch(1);boolean[] granted={false};runOnUiThread(()->{if(!destroyed&&generation.get()==token){focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener(change->{if(change<0)stopAudio();}).build();granted[0]=audioManager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}focused.countDown();});if(!focused.await(5,TimeUnit.SECONDS)||!granted[0])throw new IOException("Audio focus unavailable");StandaloneAudio.check(cancel);postStatus(token,R.string.speaking,false);StandaloneAudio.play(pcm,cancel);postStatus(token,R.string.done,true);}
  }catch(CancellationException|InterruptedException stopped){Thread.currentThread().interrupt();}catch(StandaloneAudio.RawiFailure failure){postStatus(token,R.string.rawi_failed,true);}catch(PackStore.PackMissingException missing){runOnUiThread(()->{if(!destroyed&&generation.get()==token){status.setText(packMissingMessage(missing.pack));busy=false;abandonFocus();}});}catch(Exception|LinkageError failure){postStatus(token,export?R.string.save_failed:R.string.synthesis_failed,true);}finally{if(file!=null)file.delete();}});
 }
 private void showStorageIntro(){
  Runnable seen=()->getSharedPreferences("retro",MODE_PRIVATE).edit().putBoolean("storage_intro_131",true).apply();
  new AlertDialog.Builder(this).setTitle(R.string.storage_first_title).setMessage(R.string.storage_first_message).setPositiveButton(R.string.storage_open,(dialog,which)->{seen.run();requestStorage(false);}).setNegativeButton(R.string.storage_later,(dialog,which)->{seen.run();status.setText(R.string.storage_denied);}).setOnCancelListener(dialog->{seen.run();status.setText(R.string.storage_denied);}).show();
 }
 private void requestStorage(boolean saveAfter){
  if(storageRequestActive)return;pendingStorageSave=saveAfter;storageRequestActive=true;status.setText(R.string.storage_requesting);
  if(Build.VERSION.SDK_INT>=30){try{startActivityForResult(Mp3Folder.settings(this),STORAGE_ACCESS);}catch(ActivityNotFoundException|SecurityException missing){try{startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),STORAGE_ACCESS);}catch(ActivityNotFoundException|SecurityException absent){storageRequestActive=false;pendingStorageSave=false;status.setText(R.string.storage_settings_missing);}}}
  else requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_ACCESS);
 }
 private void completeStorageRequest(){boolean save=pendingStorageSave;storageRequestActive=false;pendingStorageSave=false;if(!Mp3Folder.permitted(this)){status.setText(R.string.storage_denied);return;}if(save)startAudio(true);else initializeStorage();}
 private void initializeStorage(){if(busy||destroyed)return;busy=true;int token=generation.incrementAndGet();task=worker.submit(()->{try{Mp3Folder.directory(this);postStatus(token,R.string.ready,true);}catch(Exception failure){postStatus(token,R.string.save_failed,true);}});}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==STORAGE_ACCESS&&storageRequestActive)completeStorageRequest();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==STORAGE_ACCESS&&storageRequestActive)completeStorageRequest();}
 private void abandonFocus(){if(focus!=null){audioManager.abandonAudioFocusRequest(focus);focus=null;}}
 private void stopAudio(){generation.incrementAndGet();if(task!=null)task.cancel(true);pendingStorageSave=false;busy=false;abandonFocus();if(status!=null)status.setText(R.string.stopped);}
 private void noticesDialog(){
  StringBuilder text=new StringBuilder("Simple TTS\n\n");LinkedHashSet<String> notices=new LinkedHashSet<>(Arrays.asList("NOTICE.txt","APP-LICENSE-AGPL-3.0.txt","NDK-NOTICE.txt","LAME-NOTICE.txt","LAME-COPYING.txt","rawi/MODEL-NOTICE.txt","rawi/source/LICENSE-APACHE-2.0.txt","ONNX-RUNTIME-LICENSE.txt","ONNX-RUNTIME-THIRD-PARTY-NOTICES.txt"));for(EnginePlugin e:EngineRegistry.all()){text.append(engineName(e)).append("\n").append(getString(noticeKey(e))).append("\n\n");Collections.addAll(notices,e.noticeAssets());}
  for(String f:notices)try(java.io.InputStream in=getAssets().open(f)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);text.append("\n--- ").append(f).append(" ---\n").append(bytes.toString("UTF-8")).append("\n");}catch(Exception missing){text.append("\n").append(f).append("\n");}
  ScrollView scroll=new ScrollView(this);TextView body=new TextView(this);body.setText(text.toString());body.setTextIsSelectable(true);body.setTextSize(16);body.setPadding(24,16,24,16);scroll.addView(body);new AlertDialog.Builder(this).setTitle(R.string.copyrights).setView(scroll).setPositiveButton(R.string.close,null).show();
 }
 @Override protected void onSaveInstanceState(Bundle out){if(input!=null)out.putString("trial_text",input.getText().toString());out.putBoolean("storage_request_active",storageRequestActive);out.putBoolean("pending_storage_save",pendingStorageSave);super.onSaveInstanceState(out);}
 @Override protected void onPause(){if(storageRequestActive)storageScreenPaused=true;super.onPause();}
 @Override protected void onResume(){super.onResume();if(!storageRequestActive&&status!=null)scanPacks(false);if(storageRequestActive&&storageScreenPaused){storageScreenPaused=false;completeStorageRequest();}}
 @Override protected void onStop(){super.onStop();if(focus!=null)stopAudio();}
 @Override protected void onDestroy(){destroyed=true;generation.incrementAndGet();if(task!=null)task.cancel(true);worker.shutdownNow();packWorker.shutdownNow();abandonFocus();unregisterReceiver(noisy);super.onDestroy();}
}
