/* New JNI adapter for Flite Retro, 2026. Upstream Flite files are unmodified.
 * This adapter is distributed under the same license as Flite (see bundled COPYING). */
#include <jni.h>
#include <sys/stat.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "flite.h"
#include "usenglish.h"
#include "cmu_lex.h"
#include "cmu_grapheme_lang.h"
#include "cmu_grapheme_lex.h"
extern cst_voice *register_cmu_us_kal(const char *);
extern cst_voice *register_cmu_us_kal16(const char *);
extern cst_voice *register_cmu_us_awb(const char *);
extern cst_voice *register_cmu_us_rms(const char *);
extern cst_voice *register_cmu_us_slt(const char *);
extern void cmu_grapheme_lang_init(cst_voice *);
extern cst_lexicon *cmu_grapheme_lex_init(void);
extern void cmu_indic_lang_init(cst_voice *);
extern cst_lexicon *cmu_indic_lex_init(void);
static cst_voice *voices[40];
/* 40 voices are addressable: five built-ins and 35 extracted voices. */
static float means[40], stretches[40];
static char *extra_paths[35];
static int language_registered;
static void init(void) {
 if(voices[0]) return;
 flite_init();
 if(!language_registered) { flite_add_lang("eng", usenglish_init, cmu_lex_init); flite_add_lang("cmu_grapheme_lang", cmu_grapheme_lang_init, cmu_grapheme_lex_init); flite_add_lang("cmu_indic_lang", cmu_indic_lang_init, cmu_indic_lex_init); language_registered=1; }
 voices[0]=register_cmu_us_kal(NULL); voices[1]=register_cmu_us_kal16(NULL);
 voices[2]=register_cmu_us_awb(NULL); voices[3]=register_cmu_us_rms(NULL); voices[4]=register_cmu_us_slt(NULL);
 for(int i=0;i<5;i++) {
  means[i]=get_param_float(voices[i]->features,"int_f0_target_mean",100.0);
  stretches[i]=get_param_float(voices[i]->features,"duration_stretch",1.0);
 }
}
static cst_voice *voice_for(int index) {
 init();
 if(index<0||index>=40) index=0;
 if(index>=5 && !voices[index] && extra_paths[index-5]) {
  voices[index]=flite_voice_load(extra_paths[index-5]);
  if(voices[index]) {
   means[index]=get_param_float(voices[index]->features,"int_f0_target_mean",100.0);
   stretches[index]=get_param_float(voices[index]->features,"duration_stretch",1.0);
  }
 }
 return voices[index];
}
JNIEXPORT jboolean JNICALL Java_org_retro_flitetts_NativeBridge_loadExtraVoices(JNIEnv *e,jclass c,jstring directory) {
 init(); if(!directory) return JNI_FALSE;
 const char *dir=(*e)->GetStringUTFChars(e,directory,NULL); if(!dir) return JNI_FALSE;
    const char *names[]={"cmu_us_bdl.flitevox","cmu_us_clb.flitevox","cmu_us_jmk.flitevox","cmu_us_ksp.flitevox","kacst_ar_asc.flitevox","cmu_us_aew.flitevox","cmu_us_ahw.flitevox","cmu_us_aup.flitevox","cmu_us_axb.flitevox","cmu_us_eey.flitevox","cmu_us_fem.flitevox","cmu_us_gka.flitevox","cmu_us_ljm.flitevox","cmu_us_lnh.flitevox","cmu_us_rxr.flitevox","cmu_us_slp.flitevox","clartts_ar.flitevox","iqcorp_ar.flitevox","cvm_ar.flitevox","cvf_ar.flitevox","hf92_us.flitevox","hm6097_us.flitevox","cmu_indic_hin_ab.flitevox","cmu_indic_ben_rm.flitevox","cmu_indic_guj_ad.flitevox","cmu_indic_guj_dp.flitevox","cmu_indic_guj_kt.flitevox","cmu_indic_kan_plv.flitevox","cmu_indic_mar_aup.flitevox","cmu_indic_mar_slp.flitevox","cmu_indic_pan_amp.flitevox","cmu_indic_tam_sdr.flitevox","cmu_indic_tel_kpn.flitevox","cmu_indic_tel_sk.flitevox","cmu_indic_tel_ss.flitevox"};
    int found=0;
    for(int i=0;i<35;i++) { char path[1024]; struct stat st; snprintf(path,sizeof(path),"%s/%s",dir,names[i]);
     if(stat(path,&st)==0 && S_ISREG(st.st_mode) && st.st_size>0) { size_t n=strlen(path)+1; char *copy=(char *)malloc(n); if(copy){memcpy(copy,path,n);free(extra_paths[i]);extra_paths[i]=copy;found=1;} }
    }
(*e)->ReleaseStringUTFChars(e,directory,dir); return found?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT jint JNICALL Java_org_retro_flitetts_NativeBridge_sampleRate(JNIEnv *e,jclass c,jint index) {
 cst_voice *v=voice_for(index); return v?get_param_int(v->features,"sample_rate",16000):0;
}
JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_NativeBridge_render(JNIEnv *e,jclass c,jstring text,jint index,jfloat rate,jfloat pitch) {
 if(index<0||index>=40)index=0; cst_voice *v=voice_for(index); if(!v)return NULL;
 const char *t=(*e)->GetStringUTFChars(e,text,NULL); if(!t)return NULL;
 float old_stretch=get_param_float(v->features,"duration_stretch",stretches[index]);
 float old_mean=get_param_float(v->features,"int_f0_target_mean",means[index]);
 float old_shift=get_param_float(v->features,"f0_shift",0.0);
 feat_set_float(v->features,"duration_stretch",stretches[index]/rate);
 if(index<2)feat_set_float(v->features,"int_f0_target_mean",means[index]*pitch);
 else feat_set_float(v->features,"f0_shift",pitch);
 cst_wave *w=flite_text_to_wave(t,v);
 feat_set_float(v->features,"duration_stretch",old_stretch);
 feat_set_float(v->features,"int_f0_target_mean",old_mean);
 feat_set_float(v->features,"f0_shift",old_shift);
 (*e)->ReleaseStringUTFChars(e,text,t);
 if(!w) return NULL;
 jshortArray out=(*e)->NewShortArray(e,w->num_samples);
 if(out) (*e)->SetShortArrayRegion(e,out,0,w->num_samples,w->samples);
 delete_wave(w); return out;
}
