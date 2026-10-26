/* Old TTS MBROLA adapter, AGPL-3.0-or-later.
 * English uses Flite analysis; Arabic uses original Java frontend commands.
 * MBROLA alone generates PCM. Modified for three databases and bounded buffers. */
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "flite.h"
#include "usenglish.h"
#include "cmu_lex.h"
#include "incdll.h"
#include "parser.h"
#include "onechannel.h"
static cst_voice *frontend_voice;
static int ready,active=-1,rate_hz=16000;
static char databases[6][4096];
static cst_utterance *no_wave(cst_utterance *u){return u;}
static void frontend_init(void){
 if(frontend_voice)return;
 flite_init();frontend_voice=new_voice();frontend_voice->name="oldtts-mbrola-frontend";usenglish_init(frontend_voice);
 cst_lexicon *lex=cmu_lex_init();feat_set(frontend_voice->features,"lexicon",lexicon_val(lex));
 feat_set(frontend_voice->features,"postlex_func",uttfunc_val(lex->postlex));
 feat_set(frontend_voice->features,"wave_synth_func",uttfunc_val(no_wave));
 feat_set_float(frontend_voice->features,"int_f0_target_mean",110.0f);feat_set_float(frontend_voice->features,"duration_stretch",1.0f);
}
static int select_database(int voice){
 if(voice<0||voice>5)return 0;if(active==voice)return 1;
 if(active>=0)close_MBR();active=-1;
 if(init_MBR(databases[voice])!=0)return 0;
 active=voice;rate_hz=getFreq_MBR();
 /* Arabic sets lack some consonant transitions. MBROLA missing-diphone
  * tolerance avoids failing the service but may reduce clarity; see guide. */
 setNoError_MBR(voice==1||voice==2?1:0);return rate_hz==(voice==2?22050:16000);
}
static const char *sampa(const char *p,int american){
 static const char *from[]={"pau","sil","aa","ae","ah","ao","aw","ax","axr","ay","b","ch","d","dh","dx","eh","el","em","en","er","ey","f","g","hh","hv","ih","ix","iy","jh","k","l","m","n","ng","nx","ow","oy","p","q","r","s","sh","t","th","uh","uw","v","w","y","z","zh",NULL};
 static const char *to[]={"_","_","A:","{","V","O:","aU","@","3:","aI","b","tS","d","D","t","e","l","m","n","3:","eI","f","g","h","h","I","I","i:","dZ","k","l","m","n","N","n","@U","OI","p","_","r","s","S","t","T","U","u:","v","w","j","z","Z"};
 static const char *us[]={"_","_","A","{","V","O","aU","@","r=","AI","b","tS","d","D","4","E","l","m","n","r=","EI","f","g","h","h","I","I","i","dZ","k","l","m","n","N","n","@U","OI","p","_","r","s","S","t","T","U","u","v","w","j","z","Z"};
 for(int i=0;from[i];i++)if(!strcmp(p,from[i]))return american?us[i]:to[i];return NULL;
}
static jshortArray render_commands(JNIEnv *e,const char *commands){
 resetError_MBR();reset_MBR();if(write_MBR((char*)commands)<=0||flush_MBR()<=0)return NULL;
 int capacity=rate_hz*4,total=0,n;const int limit=rate_hz*180;
 short *pcm=malloc((size_t)capacity*sizeof(short));if(!pcm)return NULL;
 while(1){
  if(capacity-total<4096){int next=capacity*2;if(next>limit)next=limit;if(next<=capacity){free(pcm);return NULL;}short *larger=realloc(pcm,(size_t)next*sizeof(short));if(!larger){free(pcm);return NULL;}pcm=larger;capacity=next;}
  n=read_MBR(pcm+total,4096);if(n<0){free(pcm);return NULL;}if(n==0)break;total+=n;
 }
 if(total==0){free(pcm);return NULL;}jshortArray result=(*e)->NewShortArray(e,total);if(result)(*e)->SetShortArrayRegion(e,result,0,total,(jshort*)pcm);free(pcm);return result;
}
JNIEXPORT jboolean JNICALL Java_org_retro_flitetts_NativeMbrola_initialize(JNIEnv *e,jclass c,jstring directory,jobject assets){
 if(ready)return JNI_TRUE;const char *path=(*e)->GetStringUTFChars(e,directory,NULL);if(!path)return JNI_FALSE;
 const char *ids[]={"en1","ar1","ar2","us1","us2","us3"};int valid=1;
 for(int i=0;i<6;i++){int n=snprintf(databases[i],sizeof(databases[i]),"%s/%s",path,ids[i]);if(n<0||(size_t)n>=sizeof(databases[i]))valid=0;}
 (*e)->ReleaseStringUTFChars(e,directory,path);if(!valid)return JNI_FALSE; /* databases are selected on first use: the English ones live in an optional pack */
 frontend_init();ready=1;return JNI_TRUE;
}
JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_NativeMbrola_renderPhones(JNIEnv *e,jclass c,jstring phones,jint voice){
 if(!ready||voice<1||voice>2||!select_database(voice))return NULL;
 const char *commands=(*e)->GetStringUTFChars(e,phones,NULL);if(!commands)return NULL;
 jshortArray result=render_commands(e,commands);(*e)->ReleaseStringUTFChars(e,phones,commands);return result;
}
JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_NativeMbrola_render(JNIEnv *e,jclass c,jstring text,jint voice,jfloat speed,jfloat pitch){
 if(!ready||voice<0||voice>5||voice==1||voice==2||!select_database(voice))return NULL;if(speed<0.25f)speed=0.25f;if(pitch<0.5f)pitch=0.5f;
 const char *input=(*e)->GetStringUTFChars(e,text,NULL);if(!input)return NULL;
 cst_utterance *utterance=flite_synth_text(input,frontend_voice);(*e)->ReleaseStringUTFChars(e,text,input);if(!utterance)return NULL;
 cst_relation *segments=utt_relation(utterance,"Segment");char commands[32768];size_t used=snprintf(commands,sizeof(commands),"_ 30\n");
 for(cst_item *s=segments?relation_head(segments):NULL;s;s=item_next(s)){
  const char *phone=sampa(item_feat_string(s,"name"),voice>=3);if(!phone){delete_utterance(utterance);return NULL;}
  float end=item_feat_float(s,"end"),start=item_prev(s)?item_feat_float(item_prev(s),"end"):0;
  int duration=(int)((end-start)*1000.0f/speed+0.5f);if(duration<1)duration=1;
  int n;if(!strcmp(phone,"_"))n=snprintf(commands+used,sizeof(commands)-used,"_ %d\n",duration);
  else n=snprintf(commands+used,sizeof(commands)-used,"%s %d 0 %.1f 100 %.1f\n",phone,duration,110.0f*pitch,110.0f*pitch);
  if(n<0||(size_t)n>=sizeof(commands)-used){delete_utterance(utterance);return NULL;}used+=(size_t)n;
 }
 delete_utterance(utterance);if(used+8>=sizeof(commands))return NULL;strcpy(commands+used,"_ 30\n");return render_commands(e,commands);
}
