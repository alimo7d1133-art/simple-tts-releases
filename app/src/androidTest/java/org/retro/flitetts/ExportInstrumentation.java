package org.retro.flitetts;
import android.app.*;import android.content.*;import android.os.*;import android.provider.Settings;import android.view.accessibility.AccessibilityNodeInfo;import android.accessibilityservice.AccessibilityService;import android.widget.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
/** Actual runtime permission/settings launch, automatic mkdir, direct saves; separate grant/deny runs. */
public final class ExportInstrumentation extends Instrumentation {
 private Activity activity;private Field busy,status,request;private Method start;private String mode;
 @Override public void onCreate(Bundle b){super.onCreate(b);mode=b==null?"grant":b.getString("mode","grant");start();}
 static void require(boolean condition,String text){if(!condition)throw new AssertionError(text);}
 @Override public void onStart(){Bundle result=new Bundle();try{
  android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;getUiAutomation().setServiceInfo(info);
  Context c=getTargetContext();c.getSharedPreferences("retro",0).edit().putBoolean("storage_intro_131",false).putString("engine","flite").putInt("voice:flite",0).putInt("rate_percent",100).putInt("pitch_percent",100).apply();
  require(!Mp3Folder.permitted(c),"Test must begin without storage access");
  activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));busy=field("busy");status=field("status");request=field("storageRequestActive");start=MainActivity.class.getDeclaredMethod("startAudio",boolean.class);start.setAccessible(true);
  clickIds(60000,"android:id/button1");note("PASS first-launch explanation displayed and Open permissions selected");
  if(Build.VERSION.SDK_INT>=30){
   require(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION.equals(Mp3Folder.settings(c).getAction()),"Wrong all-files settings action");require(("package:"+c.getPackageName()).equals(Mp3Folder.settings(c).getDataString()),"Wrong settings package");
   awaitPackage("com.android.settings",90000);note("PASS actual package-specific all-files settings opened");
   if(!"deny".equals(mode)){try(ParcelFileDescriptor p=getUiAutomation().executeShellCommand("appops set --uid "+c.getPackageName()+" MANAGE_EXTERNAL_STORAGE allow");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(p)){byte[] b=new byte[4096];while(in.read(b)!=-1){}}long end=SystemClock.elapsedRealtime()+20000;while(!Mp3Folder.permitted(c)&&SystemClock.elapsedRealtime()<end)Thread.sleep(100);require(Mp3Folder.permitted(c),"All-files app-op not effective");}
   getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
  }else clickIds(60000,"deny".equals(mode)?new String[]{"com.android.packageinstaller:id/permission_deny_button","com.android.permissioncontroller:id/permission_deny_button"}:new String[]{"com.android.packageinstaller:id/permission_allow_button","com.android.permissioncontroller:id/permission_allow_button"});
  awaitIdle(90000);
  if("deny".equals(mode)){
   require(!Mp3Folder.permitted(c)&&statusId(R.string.storage_denied),"Denied permission not handled");launch(false);awaitIdle(60000);require(statusId(R.string.done),"Listening requires storage unexpectedly");note("PASS storage denial is safe and actual AudioTrack listening still works");
   ActivityMonitor restored=addMonitor(MainActivity.class.getName(),null,false);runOnMainSync(()->{try{request.setBoolean(activity,true);activity.recreate();}catch(Exception e){throw new RuntimeException(e);}});Activity next=waitForMonitorWithTimeout(restored,30000);removeMonitor(restored);require(next!=null,"Activity recreation failed");activity=next;awaitIdle(30000);require(!request.getBoolean(activity),"Restored permission request remained stuck");note("PASS restored interrupted permission request reconciled without blocking listening or retry");
   result.putString("stream","PASS first-launch permission denied; listen works without storage\n");
  }else{
   require(Mp3Folder.permitted(c),"Permission not granted");File folder=Mp3Folder.directory(c);require(folder.isDirectory()&&folder.getName().equals("Simple TTS"),"Root folder not created");note("PASS permission grant automatically created root Simple TTS folder before first save");
   IntentFilter treeFilter=new IntentFilter(Intent.ACTION_OPEN_DOCUMENT_TREE);ActivityMonitor trees=addMonitor(treeFilter,null,true);IntentFilter docFilter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);docFilter.addDataType("audio/mpeg");ActivityMonitor docs=addMonitor(docFilter,null,true);
   Field text=field("input");runOnMainSync(()->{try{((EditText)text.get(activity)).setText("Hello. Simple TTS automatically saves this MP3 file.");}catch(Exception e){throw new RuntimeException(e);}});
   Set<String> before=mp3Names(folder);for(int i=0;i<2;i++){launch(true);awaitIdle(90000);require(statusId(R.string.mp3_saved),"Direct MP3 save failed");}
   Set<String> after=mp3Names(folder);after.removeAll(before);require(after.size()==2,"Two distinct MP3 files were not saved");for(String name:after){File f=new File(folder,name);try(InputStream in=new FileInputStream(f)){int a=in.read(),b=in.read(),d=in.read();require((a==255&&(b&224)==224)||(a=='I'&&b=='D'&&d=='3'),"Not real MP3");}}
   require(trees.getHits()==0&&docs.getHits()==0,"A manual folder/file picker was opened");removeMonitor(trees);removeMonitor(docs);note("PASS two genuine MP3 saves without folder/document picker or filename collision");
   File part=Mp3Folder.temporary(c,"cleanup");require(part.delete(),"Cannot clean incomplete output");File[] leftovers=folder.listFiles((d,n)->n.endsWith(".part"));require(leftovers!=null&&leftovers.length==0,"Temporary MP3 part leaked");
   result.putString("stream","PASS first-launch permission grant, automatic root folder creation and direct MP3 exports\n");
  }
  finish(Activity.RESULT_OK,result);
 }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}finally{if(activity!=null)runOnMainSync(activity::finish);}}
 private Set<String> mp3Names(File dir){Set<String> r=new HashSet<>();File[] files=dir.listFiles();require(files!=null,"Cannot list export directory");for(File f:files)if(f.getName().endsWith(".mp3"))r.add(f.getName());return r;}
 private Field field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private void launch(boolean export){runOnMainSync(()->{try{start.invoke(activity,export);}catch(Exception e){throw new RuntimeException(e);}});}
 private void awaitIdle(long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;boolean[] active={true};while(SystemClock.elapsedRealtime()<end){runOnMainSync(()->{try{active[0]=busy.getBoolean(activity)||request.getBoolean(activity);}catch(Exception e){throw new RuntimeException(e);}});if(!active[0]){waitForIdleSync();return;}Thread.sleep(70);}throw new AssertionError("Activity permission/audio request timed out");}
 private boolean statusId(int id){boolean[] match={false};runOnMainSync(()->{try{match[0]=activity.getString(id).contentEquals(((TextView)status.get(activity)).getText());}catch(Exception e){throw new RuntimeException(e);}});return match[0];}
 private void clickIds(long timeout,String...ids)throws Exception {long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null)for(String id:ids)for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId(id))if(n.isVisibleToUser()&&n.isEnabled()&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK))return;Thread.sleep(200);}throw new AssertionError("Permission control not found: "+Arrays.toString(ids));}
 private void awaitPackage(String name,long timeout)throws Exception {long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null&&name.equals(String.valueOf(root.getPackageName())))return;Thread.sleep(200);}throw new AssertionError("Settings did not become visible");}
 private void note(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(1,b);}
}
