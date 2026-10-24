/* Simple TTS JNI adapter for Flite+hts_engine 1.07 (Modified BSD, NITECH) and
 * hts_engine API 1.10. The upstream front end (flite_hts_engine.c) is compiled
 * unmodified; this file only collects the generated samples in memory. */
#include <jni.h>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <vector>

extern "C" {
#include "flite.h"
#include "flite_hts_engine.h"
}

namespace {
constexpr size_t kMaxTextBytes = 4096;
constexpr size_t kMaxSamples = 48000u * 120u; // bounded JNI allocation, 120 s
Flite_HTS_Engine g_engine;
std::mutex g_mutex;
bool g_ready = false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_retro_flitetts_FliteHtsPlugin_nativeInit(JNIEnv* env, jclass, jstring path) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ready) return JNI_TRUE;
    if (!path) return JNI_FALSE;
    const char* p = env->GetStringUTFChars(path, nullptr);
    if (!p) return JNI_FALSE;
    flite_init();
    Flite_HTS_Engine_initialize(&g_engine);
    bool ok = Flite_HTS_Engine_load(&g_engine, p) == TRUE;
    env->ReleaseStringUTFChars(path, p);
    if (!ok) { Flite_HTS_Engine_clear(&g_engine); return JNI_FALSE; }
    g_ready = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_retro_flitetts_FliteHtsPlugin_nativeSampleRate(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_ready ? static_cast<jint>(HTS_Engine_get_sampling_frequency(&g_engine.engine)) : 0;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_org_retro_flitetts_FliteHtsPlugin_nativeRender(JNIEnv* env, jclass, jstring text,
                                                    jfloat rate, jfloat pitch) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_ready || !text) return nullptr;
    const jsize utf16 = env->GetStringLength(text);
    if (utf16 <= 0 || static_cast<size_t>(utf16) > kMaxTextBytes) return nullptr;
    const char* t = env->GetStringUTFChars(text, nullptr);
    if (!t) return nullptr;
    if (!(rate >= 0.1f && rate <= 10.0f)) rate = 1.0f;
    if (!(pitch >= 0.1f && pitch <= 10.0f)) pitch = 1.0f;
    Flite_HTS_Engine_set_speed(&g_engine, rate);
    Flite_HTS_Engine_add_half_tone(&g_engine, 12.0 * std::log2(static_cast<double>(pitch)));

    Flite_Text_Analyzer analyzer;
    Flite_Text_Analyzer_initialize(&analyzer);
    Flite_Text_Analyzer_analysis(&analyzer, t);
    env->ReleaseStringUTFChars(text, t);
    char** labels = nullptr;
    int count = 0;
    jshortArray result = nullptr;
    if (Flite_Text_Analyzer_get_label_data(&analyzer, &labels, &count) == TRUE && count > 0 && labels) {
        if (HTS_Engine_synthesize_from_strings(&g_engine.engine, labels, static_cast<size_t>(count)) == TRUE) {
            size_t n = HTS_Engine_get_nsamples(&g_engine.engine);
            if (n > kMaxSamples) n = kMaxSamples;
            std::vector<jshort> pcm(n);
            for (size_t i = 0; i < n; ++i) {
                double x = HTS_Engine_get_generated_speech(&g_engine.engine, i);
                if (x > 32767.0) x = 32767.0;
                if (x < -32768.0) x = -32768.0;
                pcm[i] = static_cast<jshort>(x);
            }
            HTS_Engine_refresh(&g_engine.engine);
            if (n > 0) {
                result = env->NewShortArray(static_cast<jsize>(n));
                if (result) env->SetShortArrayRegion(result, 0, static_cast<jsize>(n), pcm.data());
            }
        }
        for (int i = 0; i < count; ++i) std::free(labels[i]);
        std::free(labels);
    }
    Flite_Text_Analyzer_clear(&analyzer);
    return result;
}
