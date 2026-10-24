/* Simple TTS Android Festival JNI adapter, AGPL-3.0-or-later.
 * Uses actual upstream Festival HTS and LPC synthesis. No substitute engine. */
#include <jni.h>
#include <mutex>
#include <string>
#include <cmath>
#include <algorithm>
#include <cstdio>
#include <vector>
#include "festival.h"
#include "EST_Wave.h"
extern "C" { char *editline_history_file=nullptr; }
static std::mutex engineMutex;
static std::string libraryPath;
static bool initialized=false,attempted=false;
static std::string quoted(const std::string& value){std::string r="\"";for(char c:value){if(c=='\\'||c=='\"')r+='\\';r+=c;}return r+'\"';}
static void failure(JNIEnv* env,const char* message){env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),message);}
extern "C" JNIEXPORT jboolean JNICALL Java_org_retro_flitetts_NativeFestival_initialize(JNIEnv* env,jclass,jstring path){
 std::lock_guard<std::mutex> lock(engineMutex);
 if(initialized)return JNI_TRUE;
 if(attempted||!path)return JNI_FALSE;
 attempted=true;
 const char* raw=env->GetStringUTFChars(path,nullptr);if(!raw)return JNI_FALSE;
 libraryPath=raw;env->ReleaseStringUTFChars(path,raw);
 festival_libdir=libraryPath.c_str();festival_initialize(0,4000000);
 if(!festival_eval_command(EST_String(("(set! load-path '("+quoted(libraryPath+"/")+"))").c_str())))return JNI_FALSE;
 if(!festival_load_file(EST_String((libraryPath+"/init.scm").c_str())))return JNI_FALSE;
 const std::string arabic=libraryPath+"/voices/arabic/ara_norm_ziad_hts/",kurt=libraryPath+"/voices/english/ked_diphone/";
 std::string setup="(begin (set! voice-locations (cons (cons 'ara_norm_ziad_hts "+quoted(arabic)+") (cons (cons 'ked_diphone "+quoted(kurt)+") voice-locations))) (load "+quoted(arabic+"festvox/ara_norm_ziad_hts.scm")+") (load "+quoted(kurt+"festvox/ked_diphone.scm")+") (voice_ara_norm_ziad_hts))";
 initialized=festival_eval_command(EST_String(setup.c_str()))!=0;
 return initialized?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_NativeFestival_render(JNIEnv* env,jclass,jstring text,jint voice,jfloat rate,jfloat pitch){
 std::lock_guard<std::mutex> lock(engineMutex);
 if(!initialized||!text){failure(env,"Festival voices are not initialized");return nullptr;}
 if(voice<0||voice>1||!std::isfinite(rate)||!std::isfinite(pitch)||rate<=0||pitch<=0){failure(env,"Invalid Festival parameters");return nullptr;}
 const float r=std::max(.25f,std::min(4.f,rate)),p=std::max(.5f,std::min(2.f,pitch));
 char parameters[640];
 if(voice==0){
  // Restore Arabic after an English render before setting HTS-specific controls.
  std::snprintf(parameters,sizeof(parameters),"(begin (voice_ara_norm_ziad_hts) (set! hts_engine_params (append (list (list \"-r\" %.7f) (list \"-fm\" %.7f)) ara_norm_ziad_hts::hts_engine_params)))",r,12.0*std::log2(p));
 }else{
  // Preserve upstream normal duration 1.1 and F0 mean/std 105/15. Scale targets,
  // not model statistics; rate and pitch therefore remain independent.
  std::snprintf(parameters,sizeof(parameters),"(begin (voice_ked_diphone) (Parameter.set 'Duration_Stretch %.7f) (set! int_lr_params '((target_f0_mean %.7f) (target_f0_std %.7f) (model_f0_mean 170) (model_f0_std 34))))",1.1/r,105.0*p,15.0*p);
 }
 if(!festival_eval_command(EST_String(parameters))){failure(env,"Cannot select Festival voice or parameters");return nullptr;}
 const char* raw=env->GetStringUTFChars(text,nullptr);if(!raw)return nullptr;
 EST_Wave wave;int ok=festival_text_to_wave(EST_String(raw),wave);env->ReleaseStringUTFChars(text,raw);
 const int expected=voice==0?48000:16000;
 if(!ok||wave.num_samples()<=0||wave.num_samples()>12000000||wave.sample_rate()!=expected){char message[180];std::snprintf(message,sizeof(message),"Festival synthesis failed (status=%d,samples=%d,Hz=%d,expected=%d)",ok,wave.num_samples(),wave.sample_rate(),expected);failure(env,message);return nullptr;}
 jshortArray result=env->NewShortArray(wave.num_samples());
 if(result){std::vector<jshort> samples(wave.num_samples());for(int i=0;i<wave.num_samples();i++)samples[i]=wave.a(i,0);env->SetShortArrayRegion(result,0,wave.num_samples(),samples.data());}
 return result;
}
