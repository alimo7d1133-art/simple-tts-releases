#include <jni.h>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <string>
#include "ekho.h"
#include "audio.h"
#include <cmath>

static std::mutex g_mutex;
static ekho::Ekho* g_engine = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_org_retro_flitetts_EkhoPlugin_nativeInit(JNIEnv* env, jclass, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    std::lock_guard<std::mutex> lock(g_mutex);
    setenv("EKHO_DATA_PATH", p, 1);
    setenv("TMPDIR", p, 1);
    env->ReleaseStringUTFChars(path, p);
    if (g_engine) return JNI_TRUE;
    try {
        g_engine = new ekho::Ekho();
        g_engine->setSampleRate(16000);
        g_engine->setChannels(1);
        if (g_engine->setVoice("jyutping-wong-16000-v6") != 0) {
            delete g_engine; g_engine = nullptr; return JNI_FALSE;
        }
        return JNI_TRUE;
    } catch (...) { delete g_engine; g_engine = nullptr; return JNI_FALSE; }
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_org_retro_flitetts_EkhoPlugin_nativeRender(JNIEnv* env, jclass, jstring text, jfloat rate, jfloat pitch) {
    const char* utf = env->GetStringUTFChars(text, nullptr);
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_engine) { env->ReleaseStringUTFChars(text, utf); return nullptr; }
    try {
        // Ekho's documented deltas are percentages: 1.0 is unchanged.
        int speed = (int)((rate - 1.0f) * 100.0f);
        int pdelta = (int)std::lround((pitch - 1.0f) * (pitch < 1.0f ? 200.0f : 100.0f));
        g_engine->setSpeed(speed);
        g_engine->setPitch(pdelta);
        int samples = 0;
        short* pcm = g_engine->synth3(std::string(utf), samples);
        env->ReleaseStringUTFChars(text, utf);
        if (!pcm || samples <= 0) { delete[] pcm; return nullptr; }
        jshortArray result = env->NewShortArray(samples);
        if (result) env->SetShortArrayRegion(result, 0, samples, pcm);
        delete[] pcm;
        return result;
    } catch (...) { env->ReleaseStringUTFChars(text, utf); return nullptr; }
}
