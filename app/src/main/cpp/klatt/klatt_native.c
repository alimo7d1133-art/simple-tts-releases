/* Old TTS standalone Klatt integration, AGPL-3.0-or-later adapter.
 * Upstream Klatt core retains GPL-3.0-or-later. Flite supplies linguistic
 * analysis only. Original approximate phone-to-formant schedule below.
 * No eSpeak engine, phonemizer, dictionaries, or voice tables are used. */
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "flite.h"
#include "usenglish.h"
#include "cmu_lex.h"
#include "parwave.h"
static cst_voice *frontend;
static cst_utterance *no_wave(cst_utterance *u){return u;}
static void init_frontend(void){
 if(frontend)return;flite_init();frontend=new_voice();frontend->name="oldtts-klatt-frontend";usenglish_init(frontend);
 cst_lexicon *lex=cmu_lex_init();feat_set(frontend->features,"lexicon",lexicon_val(lex));feat_set(frontend->features,"postlex_func",uttfunc_val(lex->postlex));feat_set(frontend->features,"wave_synth_func",uttfunc_val(no_wave));
}
static int oneof(const char *p,const char *list){char key[24];snprintf(key,sizeof(key)," %s ",p);return strstr(list,key)!=NULL;}
static void frame_for(const char *p,klatt_frame_t *f,float pitch,float progress){
 memset(f,0,sizeof(*f));f->F0hz10=(long)(1150*pitch);f->AVdb=60;f->AVpdb=0;f->Gain0=62;
 f->F1hz=500;f->B1hz=70;f->F2hz=1500;f->B2hz=100;f->F3hz=2500;f->B3hz=150;f->F4hz=3500;f->B4hz=250;f->F5hz=4500;f->B5hz=300;f->F6hz=4900;f->B6hz=400;
 f->FNZhz=250;f->BNZhz=100;f->FNPhz=250;f->BNPhz=100;f->Kopen=40;f->TLTdb=6;
 f->B1phz=80;f->B2phz=130;f->B3phz=200;f->B4phz=250;f->B5phz=300;f->B6phz=400;
 if(oneof(p," pau  sil  # ")){f->F0hz10=0;f->AVdb=0;return;}
 static const char *v[]={"aa","ae","ah","ao","aw","ax","axr","ay","eh","er","ey","ih","ix","iy","ow","oy","uh","uw",NULL};
 static const int formants[][3]={{730,1090,2440},{660,1720,2410},{620,1220,2550},{570,840,2410},{700,1200,2450},{500,1500,2500},{490,1350,1690},{730,1090,2440},{530,1840,2480},{490,1350,1690},{480,1920,2600},{390,1990,2550},{420,1750,2500},{270,2290,3010},{520,1000,2400},{570,840,2410},{440,1020,2240},{300,870,2240}};
 for(int i=0;v[i];i++)if(!strcmp(p,v[i])){
  f->F1hz=formants[i][0];f->F2hz=formants[i][1];f->F3hz=formants[i][2];
  int target= !strcmp(p,"ay")||!strcmp(p,"ey")||!strcmp(p,"oy")?1:!strcmp(p,"aw")||!strcmp(p,"ow")?2:0;
  if(target){float k=fmaxf(0,(progress-.25f)/.75f);f->F1hz=(long)(f->F1hz*(1-k)+(target==1?300:330)*k);f->F2hz=(long)(f->F2hz*(1-k)+(target==1?2200:850)*k);}
  return;
 }
 if(oneof(p," l  el ")){f->F1hz=360;f->F2hz=1100;f->F3hz=2800;f->AVdb=55;return;}
 if(!strcmp(p,"r")){f->F1hz=350;f->F2hz=1300;f->F3hz=1650;f->AVdb=56;return;}
 if(!strcmp(p,"w")){f->F1hz=300;f->F2hz=750;f->F3hz=2200;f->AVdb=55;return;}
 if(!strcmp(p,"y")){f->F1hz=270;f->F2hz=2200;f->F3hz=2850;f->AVdb=55;return;}
 if(oneof(p," m  n  ng  em  en  nx ")){f->F1hz=280;f->F2hz=!strcmp(p,"m")?900:1500;f->F3hz=2250;f->FNZhz=500;f->FNPhz=250;f->AVdb=54;return;}
 int voiced=oneof(p," b  d  g  jh  v  dh  z  zh  dx ");f->AVdb=voiced?48:0;if(!voiced)f->F0hz10=0;
 if(oneof(p," p  t  k  b  d  g  ch  jh  dx ")){
  if(progress<.72f){f->AVdb=voiced?36:0;return;}f->AF=55;f->AB=oneof(p," p  b ")?40:30;
  f->A3=40;f->A4=45;f->A5=45;f->A6=35;return;
 }
 if(oneof(p," hh  hv ")){f->ASP=52;return;}
 f->AF=oneof(p," s  z ")?60:oneof(p," sh  zh ")?56:48;
 if(oneof(p," s  z ")){f->F3hz=3500;f->A4=52;f->A5=55;f->A6=45;f->AB=25;}
 else if(oneof(p," sh  zh ")){f->F2hz=1800;f->F3hz=2400;f->A2=42;f->A3=52;f->A4=45;f->AB=15;}
 else{f->AB=42;f->A3=25;f->A4=25;}
}
JNIEXPORT jint JNICALL Java_org_retro_flitetts_NativeKlatt_sampleRate(JNIEnv *e,jclass c,jint voice){return 10000;}
JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_NativeKlatt_render(JNIEnv *e,jclass c,jstring text,jint voice,jfloat speed,jfloat pitch){
 if(!text||voice<0||voice>1)return NULL;speed=fmaxf(.25f,fminf(4,speed));pitch=fmaxf(.5f,fminf(2,pitch));init_frontend();
 const char *input=(*e)->GetStringUTFChars(e,text,NULL);if(!input)return NULL;cst_utterance *u=flite_synth_text(input,frontend);(*e)->ReleaseStringUTFChars(e,text,input);if(!u)return NULL;
 klatt_global_t g;memset(&g,0,sizeof(g));g.synthesis_model=CASCADE_PARALLEL;g.samrate=10000;g.nfcascade=5;g.glsource=voice==0?IMPULSIVE:NATURAL;g.nspfr=100;g.quiet_flag=1;parwave_init(&g);
 int cap=50000,total=0;short *wave=malloc((size_t)cap*sizeof(short));if(!wave){delete_utterance(u);return NULL;}
 cst_relation *r=utt_relation(u,"Segment");long prev1=500,prev2=1500,prev3=2500;
 for(cst_item *s=r?relation_head(r):NULL;s;s=item_next(s)){
  const char *phone=item_feat_string(s,"name");float end=item_feat_float(s,"end"),start=item_prev(s)?item_feat_float(item_prev(s),"end"):0;int n=(int)((end-start)*10000/speed+.5f);if(n<1)n=1;
  if(n>1800000-total){free(wave);delete_utterance(u);return NULL;}
  if(total+n>cap){while(total+n>cap)cap*=2;short *p=realloc(wave,(size_t)cap*sizeof(short));if(!p){free(wave);delete_utterance(u);return NULL;}wave=p;}
  long last1=prev1,last2=prev2,last3=prev3;
  for(int pos=0;pos<n;pos+=100){int count=n-pos<100?n-pos:100;int pcm[100];klatt_frame_t f;frame_for(phone,&f,pitch,((float)pos+count/2.f)/n);
   last1=f.F1hz;last2=f.F2hz;last3=f.F3hz;float blend=fminf(1,(pos+count/2.f)/200.f);f.F1hz=(long)(prev1*(1-blend)+f.F1hz*blend);f.F2hz=(long)(prev2*(1-blend)+f.F2hz*blend);f.F3hz=(long)(prev3*(1-blend)+f.F3hz*blend);
   g.nspfr=count;parwave(&g,&f,pcm);for(int k=0;k<count;k++)wave[total++]=(short)pcm[k];
  }
  prev1=last1;prev2=last2;prev3=last3;
 }
 delete_utterance(u);jshortArray result=(*e)->NewShortArray(e,total);if(result&&total)(*e)->SetShortArrayRegion(e,result,0,total,wave);free(wave);return result;
}
