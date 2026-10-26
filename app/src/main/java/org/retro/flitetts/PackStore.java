/* Simple TTS voice packs, AGPL-3.0-or-later. Packs are ZIP files the user downloads and unzips
 * by hand into "Simple TTS data/<pack>/" on internal storage; this class verifies them against
 * the catalog compiled into the app (no network access, no INTERNET permission). */
package org.retro.flitetts;
import android.content.Context;
import android.os.Environment;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

public final class PackStore {
 public static final String DATA_FOLDER="Simple TTS data";
 public static final int ACTIVE=0,MISSING=1,INVALID=2,NO_PERMISSION=3,LOW_SPACE=4;
 public static final class Pack {
  public String id,title,target,zip,zipSha256,manifestSha256;
  public long zipBytes,installBytes;
  public String[] paths;public long[] sizes;
 }
 public static final class Status {
  public final Pack pack;public final int state;public final String detail;public final boolean folderFound;
  Status(Pack p,int s,String d,boolean f){pack=p;state=s;detail=d;folderFound=f;}
 }
 public interface Progress { void checking(Pack pack); }
 /** A voice needs a pack that is not installed. */
 public static final class PackMissingException extends IllegalStateException {
  public final String pack;
  public PackMissingException(String pack){super("Voice pack not installed: "+pack);this.pack=pack;}
 }
 private static List<Pack> catalog;
 private static Context app;
 public static synchronized void init(Context c){if(app==null)app=c.getApplicationContext()!=null?c.getApplicationContext():c;}
 public static Pack packForFile(String target,String path){if(app==null)return null;return packForFile(app,target,path);}
 public static synchronized List<Pack> catalog(Context c){
  init(c);if(catalog!=null)return catalog;
  try(InputStream in=c.getAssets().open("packs/catalog.json")){
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
   JSONArray a=new JSONArray(out.toString("UTF-8"));ArrayList<Pack> list=new ArrayList<>();
   for(int i=0;i<a.length();i++){
    JSONObject o=a.getJSONObject(i);Pack p=new Pack();p.id=o.getString("id");p.title=o.getString("title");p.target=o.getString("target");p.zip=o.getString("zip");
    p.zipSha256=o.getString("zipSha256");p.manifestSha256=o.getString("manifestSha256");p.zipBytes=o.getLong("zipBytes");p.installBytes=o.getLong("installBytes");
    JSONArray f=o.getJSONArray("files");p.paths=new String[f.length()];p.sizes=new long[f.length()];
    for(int k=0;k<f.length();k++){p.paths[k]=f.getJSONObject(k).getString("path");p.sizes[k]=f.getJSONObject(k).getLong("size");}
    list.add(p);
   }
   catalog=Collections.unmodifiableList(list);return catalog;
  }catch(IOException|JSONException e){throw new IllegalStateException("Voice pack catalog is unreadable",e);}
 }
 public static Pack find(Context c,String id){for(Pack p:catalog(c))if(p.id.equals(id))return p;return null;}
 /** Pack that provides the given file of a target directory, or null if the file is part of the base app. */
 public static Pack packForFile(Context c,String target,String path){
  for(Pack p:catalog(c))if(p.target.equals(target))for(String q:p.paths)if(q.equals(path))return p;return null;
 }
 private static File targetDir(Context c,Pack p){return new File(c.getFilesDir(),p.target);}
 public static boolean fileReady(Context c,Pack p,int index){
  File f=new File(targetDir(c,p),p.paths[index]);return f.isFile()&&f.length()==p.sizes[index];
 }
 /** True when every file of the pack is installed in private storage (cheap size check). */
 public static boolean complete(Context c,String id){
  Pack p=find(c,id);if(p==null)return false;for(int i=0;i<p.paths.length;i++)if(!fileReady(c,p,i))return false;return true;
 }
 public static boolean fileReady(Context c,String target,String path){
  Pack p=packForFile(c,target,path);if(p==null)return true;
  for(int i=0;i<p.paths.length;i++)if(p.paths[i].equals(path))return fileReady(c,p,i);return false;
 }
 public static File dataRoot(){return new File(Environment.getExternalStorageDirectory(),DATA_FOLDER);}
 public static String dataRootPath(){return dataRoot().getPath();}
 /** Creates "Simple TTS data" (with a short README) so the user can find it; ignored if storage is not permitted. */
 public static void ensureFolder(Context c){
  try{
   if(!Mp3Folder.permitted(c))return;File root=dataRoot();if(!root.isDirectory()&&!root.mkdirs())return;
   File readme=new File(root,"README.txt");
   if(!readme.exists())try(FileOutputStream o=new FileOutputStream(readme)){o.write(("Simple TTS voice packs\n\nUnzip a downloaded pack ZIP and move the folder it contains (for example flite-indic-hi) into this folder, "+
     "so that the path is \"Simple TTS data/flite-indic-hi/manifest.json\". Open Simple TTS (or press Refresh in Voice packs): every file is checked with SHA-256 and the voices are activated. "+
     "After activation the copy in this folder may be deleted.\n").getBytes("UTF-8"));}
  }catch(IOException|SecurityException ignored){}
 }
 private static String hex(byte[] d){StringBuilder s=new StringBuilder();for(byte b:d)s.append(String.format("%02x",b));return s.toString();}
 private static File stateFile(Context c,String id){return new File(new File(c.getFilesDir(),"packs-state"),id+".json");}

 /** Scans "Simple TTS data", verifies and activates every pack found. Blocking; call from a worker thread. */
 public static synchronized List<Status> refresh(Context c,Progress progress){
  ArrayList<Status> result=new ArrayList<>();boolean permitted=Mp3Folder.permitted(c);
  if(permitted)ensureFolder(c);
  for(Pack p:catalog(c)){
   boolean done=complete(c,p.id);File folder=new File(dataRoot(),p.id);boolean found=permitted&&new File(folder,"manifest.json").isFile();
   if(!permitted){result.add(new Status(p,done?ACTIVE:NO_PERMISSION,"",false));continue;}
   if(!found){result.add(new Status(p,done?ACTIVE:MISSING,"",permitted&&folder.isDirectory()));continue;}
   if(done&&stateMatches(c,p)){result.add(new Status(p,ACTIVE,"",true));continue;}
   if(progress!=null)progress.checking(p);
   result.add(activate(c,p,folder));
  }
  return result;
 }
 private static boolean stateMatches(Context c,Pack p){
  File s=stateFile(c,p.id);if(!s.isFile())return false;
  try(BufferedReader r=new BufferedReader(new FileReader(s))){return p.manifestSha256.equals(new JSONObject(r.readLine()).optString("manifest"));}catch(IOException|JSONException e){return false;}
 }
 private static Status activate(Context c,Pack p,File folder){
  ArrayList<File> pending=new ArrayList<>();
  try{
   File manifestFile=new File(folder,"manifest.json");
   if(manifestFile.length()>1048576)return new Status(p,INVALID,"manifest.json is too large",true);
   byte[] mb=new byte[(int)manifestFile.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(manifestFile))){in.readFully(mb);}
   if(!hex(MessageDigest.getInstance("SHA-256").digest(mb)).equals(p.manifestSha256))return new Status(p,INVALID,"manifest.json does not match this version of the app (wrong or modified pack)",true);
   JSONObject m=new JSONObject(new String(mb,"UTF-8"));JSONArray files=m.getJSONArray("files");
   if(!p.id.equals(m.getString("pack"))||!p.target.equals(m.getString("target"))||files.length()!=p.paths.length)return new Status(p,INVALID,"manifest.json content is inconsistent",true);
   File root=targetDir(c,p);if(!root.isDirectory()&&!root.mkdirs())return new Status(p,INVALID,"cannot create private storage folder",true);
   if(root.getUsableSpace()<p.installBytes+(64L<<20))return new Status(p,LOW_SPACE,"",true);
   String canonicalFolder=folder.getCanonicalPath()+File.separator;
   for(int i=0;i<files.length();i++){
    JSONObject f=files.getJSONObject(i);String path=f.getString("path");
    if(!path.equals(p.paths[i])||f.getLong("size")!=p.sizes[i])return new Status(p,INVALID,"manifest.json lists unexpected file "+path,true);
    File src=new File(folder,path);
    if(!src.isFile()||!src.getCanonicalPath().startsWith(canonicalFolder))return new Status(p,INVALID,"missing file: "+path+" (unzip the whole pack)",true);
    if(src.length()!=p.sizes[i])return new Status(p,INVALID,"wrong size (incomplete download or unzip): "+path,true);
    File dst=new File(root,path),tmp=new File(root,path+".new");File parent=dst.getParentFile();
    if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("cannot create "+parent);
    MessageDigest md=MessageDigest.getInstance("SHA-256");
    try(InputStream in=new FileInputStream(src);FileOutputStream out=new FileOutputStream(tmp)){
     byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){md.update(buf,0,n);out.write(buf,0,n);}out.getFD().sync();
    }
    pending.add(tmp);
    if(!hex(md.digest()).equals(f.getString("sha256"))){deleteAll(pending);return new Status(p,INVALID,"checksum mismatch (corrupt download or modified file): "+path,true);}
   }
   for(int i=0;i<files.length();i++){File tmp=pending.get(i),dst=new File(root,p.paths[i]);if(dst.exists()&&!dst.delete()||!tmp.renameTo(dst))throw new IOException("cannot activate "+p.paths[i]);}
   File sf=stateFile(c,p.id);sf.getParentFile().mkdirs();
   try(FileWriter w=new FileWriter(sf)){w.write(new JSONObject().put("manifest",p.manifestSha256).toString()+"\n");}
   return new Status(p,ACTIVE,"",true);
  }catch(Exception e){deleteAll(pending);return new Status(p,INVALID,e.getClass().getSimpleName()+": "+e.getMessage(),true);}
 }
 private static void deleteAll(List<File> l){for(File f:l)f.delete();}
 /** Deletes the activated copy of a pack (the user's downloaded folder is never touched). */
 public static synchronized boolean remove(Context c,String id){
  Pack p=find(c,id);if(p==null)return false;boolean ok=true;File root=targetDir(c,p);
  for(String path:p.paths){File f=new File(root,path);if(f.exists()&&!f.delete())ok=false;}
  if(!p.target.equals("flite-extra")){ // remove emptied sub-folders (dict/, voice/, syllable folders)
   for(String path:p.paths){File d=new File(root,path).getParentFile();while(d!=null&&!d.equals(root)&&d.isDirectory()&&d.list()!=null&&d.list().length==0){d.delete();d=d.getParentFile();}}
  }
  stateFile(c,id).delete();return ok;
 }
}
