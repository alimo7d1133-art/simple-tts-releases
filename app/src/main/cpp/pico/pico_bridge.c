/* Old TTS JNI adapter. Modified integration: checked asset extraction, initialization,
 * per-call resource cleanup and serialized native entry points. Upstream Pico unchanged. */
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/stat.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "picoapi.h"
#include "picodefs.h"
#define PMEM (32*1024*1024)
static pico_System sys;
static uint8_t *mem;
static pico_Resource ta[6],sg[6];
static const char *ids[]={"en-US","en-GB","de-DE","es-ES","fr-FR","it-IT"};
static const char *suffix[]={"lh0_sg","kh0_sg","gl0_sg","zl0_sg","nk0_sg","cm0_sg"};
static char root[1024];static int ready;
static int copy_asset(AAssetManager *manager,const char *name,const char *dst){
 AAsset *asset=AAssetManager_open(manager,name,AASSET_MODE_STREAMING);if(!asset)return 0;
 FILE *f=fopen(dst,"wb");if(!f){AAsset_close(asset);return 0;}
 char buffer[8192];int n,ok=1;while((n=AAsset_read(asset,buffer,sizeof(buffer)))>0)if(fwrite(buffer,1,n,f)!=(size_t)n){ok=0;break;}
 if(n<0)ok=0;if(fclose(f)!=0)ok=0;AAsset_close(asset);return ok;
}
static int load(AAssetManager *manager,const char *file,pico_Resource *resource){
 char dst[1200],asset[128];snprintf(asset,sizeof(asset),"pico/%s",file);snprintf(dst,sizeof(dst),"%s/%s",root,file);
 return copy_asset(manager,asset,dst)&&pico_loadResource(sys,dst,resource)==PICO_OK;
}
JNIEXPORT jboolean JNICALL Java_org_retro_flitetts_PicoPlugin_nativeInit(JNIEnv *e,jclass c,jobject assets,jstring files){
 if(ready)return JNI_TRUE;
 AAssetManager *manager=AAssetManager_fromJava(e,assets);if(!manager)return JNI_FALSE;
 const char *path=(*e)->GetStringUTFChars(e,files,NULL);if(!path)return JNI_FALSE;snprintf(root,sizeof(root),"%s/pico",path);(*e)->ReleaseStringUTFChars(e,files,path);mkdir(root,0700);
 mem=calloc(1,PMEM);if(!mem)return JNI_FALSE;
 if(pico_initialize(mem,PMEM,&sys)!=PICO_OK){free(mem);mem=NULL;return JNI_FALSE;}
 for(int i=0;i<6;i++){
  char name[80];snprintf(name,sizeof(name),"%s_ta.bin",ids[i]);if(!load(manager,name,&ta[i]))goto fail;
  snprintf(name,sizeof(name),"%s_%s.bin",ids[i],suffix[i]);if(!load(manager,name,&sg[i]))goto fail;
 }
 ready=1;return JNI_TRUE;
 fail:pico_terminate(&sys);free(mem);mem=NULL;return JNI_FALSE;
}
JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_PicoPlugin_nativeRender(JNIEnv *e,jclass c,jstring text,jint voice,jfloat rate,jfloat pitch){
 if(!ready||voice<0||voice>=6)return NULL;
 char definition[32],resource[128];snprintf(definition,sizeof(definition),"oldtts%d",voice);
 if(pico_createVoiceDefinition(sys,definition)!=PICO_OK)return NULL;
 pico_Engine engine=NULL;int16_t *pcm=NULL;char *input=NULL;jshortArray out=NULL;
 if(pico_getResourceName(sys,ta[voice],resource)!=PICO_OK||pico_addResourceToVoiceDefinition(sys,definition,resource)!=PICO_OK)goto done;
 if(pico_getResourceName(sys,sg[voice],resource)!=PICO_OK||pico_addResourceToVoiceDefinition(sys,definition,resource)!=PICO_OK)goto done;
 if(pico_newEngine(sys,definition,&engine)!=PICO_OK)goto done;
 const char *t=(*e)->GetStringUTFChars(e,text,NULL);if(!t)goto done;
 size_t len=strlen(t);input=malloc(len+128);if(!input){(*e)->ReleaseStringUTFChars(e,text,t);goto done;}
 int size=snprintf(input,len+128,"<speed level='%d'><pitch level='%d'>%s</pitch></speed>",(int)(rate*100),(int)(pitch*100),t);(*e)->ReleaseStringUTFChars(e,text,t);
 int capacity=8192,total=0,offset=0;pcm=malloc(capacity*sizeof(int16_t));if(!pcm)goto done;
 int16_t buffer[2048];pico_Status status;
 while(offset<=size){
  pico_Int16 supplied=0;int remaining=size+1-offset;if(remaining>32767)remaining=32767;
  if(pico_putTextUtf8(engine,(pico_Char*)input+offset,(pico_Int16)remaining,&supplied)!=PICO_OK)goto done;
  offset+=supplied;
  do {
   pico_Int16 bytes=0,type=0;status=pico_getData(engine,buffer,sizeof(buffer),&bytes,&type);
   if(status!=PICO_STEP_BUSY&&status!=PICO_STEP_IDLE)goto done;
   int count=bytes/2;if(count>0){
    if(total+count>capacity){int next=(total+count)*2;int16_t *larger=realloc(pcm,next*sizeof(int16_t));if(!larger)goto done;pcm=larger;capacity=next;}
    memcpy(pcm+total,buffer,count*sizeof(int16_t));total+=count;
   }
  }while(status==PICO_STEP_BUSY);
  if(supplied==0&&offset<=size)goto done;
 }
 out=(*e)->NewShortArray(e,total);if(out&&total>0)(*e)->SetShortArrayRegion(e,out,0,total,(jshort*)pcm);
 done:free(input);free(pcm);if(engine)pico_disposeEngine(sys,&engine);pico_releaseVoiceDefinition(sys,definition);return out;
}
