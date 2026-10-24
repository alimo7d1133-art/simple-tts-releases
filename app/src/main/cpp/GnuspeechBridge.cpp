/* GnuSpeechSA JNI bridge, GPL-3.0-or-later; see gnuspeech/source/COPYING. */
#include <algorithm>
#include <cmath>
#include <exception>
#include <memory>
#include <string>
#include <vector>
#include <jni.h>
#include "Controller.h"
#include "Exception.h"
#include "global.h"
#include "Model.h"
#include "TRMControlModelConfiguration.h"
#include "en/phonetic_string_parser/PhoneticStringParser.h"
#include "en/text_parser/TextParser.h"

namespace {
std::string g_config_dir;
const int kSampleRate = 44100;
void throwJava(JNIEnv* env, const char* message) {
    jclass c = env->FindClass("java/lang/IllegalStateException");
    if (c) env->ThrowNew(c, message);
}
}

extern "C" JNIEXPORT void JNICALL
Java_org_retro_flitetts_GnuspeechPlugin_nativeInitialize(JNIEnv* env, jclass, jstring configDir) {
    const char* chars = env->GetStringUTFChars(configDir, nullptr);
    if (!chars) return;
    g_config_dir.assign(chars);
    env->ReleaseStringUTFChars(configDir, chars);
}

extern "C" JNIEXPORT jint JNICALL
Java_org_retro_flitetts_GnuspeechPlugin_nativeSampleRate(JNIEnv*, jclass) { return kSampleRate; }

extern "C" JNIEXPORT jshortArray JNICALL
Java_org_retro_flitetts_GnuspeechPlugin_nativeRender(JNIEnv* env, jclass, jstring text, jint,
                                                      jfloat rate, jfloat pitch) {
    if (g_config_dir.empty()) { throwJava(env, "GnuSpeechSA is not initialized"); return nullptr; }
    const char* chars = env->GetStringUTFChars(text, nullptr);
    if (!chars) return nullptr;
    try {
        std::string input(chars);
        env->ReleaseStringUTFChars(text, chars);chars=nullptr;
        GS::TRMControlModel::Model model;
        model.load(g_config_dir.c_str(), TRM_CONTROL_MODEL_CONFIG_FILE);
        GS::TRMControlModel::Controller controller(g_config_dir.c_str(), model);
        auto& conf = controller.trmControlModelConfiguration();
        conf.tempo *= std::max(0.25f,std::min(4.0f,rate));
        conf.pitchOffset += 12.0*std::log2(std::max(0.5f,std::min(2.0f,pitch)));
        GS::En::TextParser parser(g_config_dir.c_str(), conf.dictionary1File,
                                  conf.dictionary2File, conf.dictionary3File);
        GS::En::PhoneticStringParser phonetic(g_config_dir.c_str(), controller);
        std::string phones = parser.parseText(input.c_str());
        std::vector<float> samples;
        controller.synthesizePhoneticString(phonetic, phones.c_str(),
                                             (g_config_dir + "/oldtts-parameters.tmp").c_str(), samples);
        jshortArray result = env->NewShortArray(static_cast<jsize>(samples.size()));
        if (!result) return nullptr;
        std::vector<jshort> pcm(samples.size());
        for (size_t i = 0; i < samples.size(); ++i) {
            float x = std::max(-1.0f, std::min(1.0f, samples[i]));
            pcm[i] = static_cast<jshort>(std::lrintf(x * 32767.0f));
        }
        env->SetShortArrayRegion(result, 0, static_cast<jsize>(pcm.size()), pcm.data());
        return result;
    } catch (const std::exception& e) {
        if(chars)env->ReleaseStringUTFChars(text, chars);
        throwJava(env, e.what());
    } catch (...) {
        if(chars)env->ReleaseStringUTFChars(text, chars);
        throwJava(env, "GnuSpeechSA synthesis failed");
    }
    return nullptr;
}
