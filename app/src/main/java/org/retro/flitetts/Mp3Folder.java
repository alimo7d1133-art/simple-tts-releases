/* Simple TTS direct shared-storage export, AGPL-3.0-or-later. */
package org.retro.flitetts;
import android.Manifest;import android.content.*;import android.content.pm.PackageManager;import android.media.MediaScannerConnection;import android.net.Uri;import android.os.*;import android.provider.Settings;import java.io.*;import java.nio.file.Files;
/** Writes only this app's generated MP3 files; never scans or reads unrelated files. */
final class Mp3Folder {
 static boolean permitted(Context c){return Build.VERSION.SDK_INT>=30?Environment.isExternalStorageManager():c.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
 static Intent settings(Context c){return new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+c.getPackageName()));}
 static void forgetLegacyGrant(Context c){String old=c.getSharedPreferences("retro",0).getString("mp3_folder_tree",null);if(old!=null){Uri uri=Uri.parse(old);for(UriPermission p:c.getContentResolver().getPersistedUriPermissions())if(p.getUri().equals(uri)){int flags=(p.isReadPermission()?Intent.FLAG_GRANT_READ_URI_PERMISSION:0)|(p.isWritePermission()?Intent.FLAG_GRANT_WRITE_URI_PERMISSION:0);try{c.getContentResolver().releasePersistableUriPermission(uri,flags);}catch(SecurityException ignored){}}c.getSharedPreferences("retro",0).edit().remove("mp3_folder_tree").apply();}}
 @SuppressWarnings("deprecation") static File directory(Context c)throws IOException {
  if(!permitted(c))throw new IOException("Storage permission not granted");if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))throw new IOException("Primary shared storage unavailable");
  File root=Environment.getExternalStorageDirectory().getCanonicalFile(),folder=new File(root,"Simple TTS");if(!root.equals(folder.getCanonicalFile().getParentFile()))throw new IOException("Unexpected export directory link");
  if(!folder.isDirectory()&&!folder.mkdirs()&&!folder.isDirectory())throw new IOException("Cannot create Simple TTS directory");return folder;
 }
 static File temporary(Context c,String voice)throws IOException {String safe=voice.replaceAll("[^A-Za-z0-9_-]","_");return File.createTempFile("Simple-TTS-"+safe+"-"+System.currentTimeMillis()+"-",".mp3.part",directory(c));}
 static File publish(Context c,File part)throws IOException {File folder=directory(c);if(!folder.getCanonicalFile().equals(part.getCanonicalFile().getParentFile())||!part.getName().endsWith(".mp3.part")||!part.isFile()||part.length()==0)throw new IOException("Invalid MP3 export");File result=new File(folder,part.getName().substring(0,part.getName().length()-5));Files.move(part.toPath(),result.toPath());try{MediaScannerConnection.scanFile(c,new String[]{result.getAbsolutePath()},new String[]{"audio/mpeg"},null);}catch(RuntimeException ignored){}return result;}
}
