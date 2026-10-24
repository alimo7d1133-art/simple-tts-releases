#include <jni.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <cerrno>
#include <cstring>
#include <limits>
#include <mutex>
#include <string>
#include <vector>
#include <sys/stat.h>

#define main openjtalk_unused_main
#define OPEN_JTALK_NO_EXTERN_C
#include "upstream/open_jtalk-1.11/bin/open_jtalk.c"
#undef main

namespace {
constexpr size_t kMaxTextBytes = 1023; // Open JTalk's MAXBUFLEN - 1.
constexpr uint32_t kMaxSamples = 2 * 48000 * 60; // 120 seconds, bounded JNI allocation.
Open_JTalk g_oj;
std::mutex g_mutex;
bool g_ready = false;
std::string g_root;

bool copyAsset(AAssetManager* manager, const std::string& asset, const std::string& path) {
    AAsset* source = AAssetManager_open(manager, asset.c_str(), AASSET_MODE_STREAMING);
    if (!source) return false;
    FILE* out = std::fopen(path.c_str(), "wb");
    if (!out) { AAsset_close(source); return false; }
    char buffer[16384]; bool ok = true; int n;
    while ((n = AAsset_read(source, buffer, sizeof(buffer))) > 0)
        ok = ok && std::fwrite(buffer, 1, static_cast<size_t>(n), out) == static_cast<size_t>(n);
    if (n < 0) ok = false;
    ok = (std::fclose(out) == 0) && ok;
    AAsset_close(source);
    return ok;
}

bool copyFlatDir(AAssetManager* manager, const std::string& assetDir, const std::string& outputDir) {
    if (mkdir(outputDir.c_str(), 0755) != 0 && errno != EEXIST) return false;
    AAssetDir* dir = AAssetManager_openDir(manager, assetDir.c_str());
    if (!dir) return false;
    bool ok = true; const char* name;
    while ((name = AAssetDir_getNextFileName(dir)) != nullptr)
        ok = copyAsset(manager, assetDir + "/" + name, outputDir + "/" + name) && ok;
    AAssetDir_close(dir);
    return ok;
}

uint32_t le32(const unsigned char* p) { return uint32_t(p[0]) | (uint32_t(p[1]) << 8) | (uint32_t(p[2]) << 16) | (uint32_t(p[3]) << 24); }
uint16_t le16(const unsigned char* p) { return uint16_t(p[0]) | (uint16_t(p[1]) << 8); }

bool readWave(const std::string& path, std::vector<short>& samples) {
    FILE* f = std::fopen(path.c_str(), "rb"); if (!f) return false;
    unsigned char header[12];
    if (std::fread(header, 1, sizeof(header), f) != sizeof(header) || std::memcmp(header, "RIFF", 4) || std::memcmp(header + 8, "WAVE", 4)) { std::fclose(f); return false; }
    bool fmt = false, data = false; uint16_t format = 0, channels = 0, bits = 0; uint32_t sampleRate = 0, dataSize = 0;
    unsigned char chunk[8];
    while (std::fread(chunk, 1, 8, f) == 8) {
        uint32_t size = le32(chunk + 4); long payload = std::ftell(f);
        if (!std::memcmp(chunk, "fmt ", 4) && size >= 16) { unsigned char b[16]; if (std::fread(b, 1, 16, f) != 16) break; format = le16(b); channels = le16(b + 2); sampleRate = le32(b + 4); bits = le16(b + 14); fmt = true; if (size > 16) std::fseek(f, size - 16, SEEK_CUR); }
        else if (!std::memcmp(chunk, "data", 4)) { dataSize = size; data = true; break; }
        else if (std::fseek(f, size + (size & 1u), SEEK_CUR) != 0) break;
        (void)payload;
    }
    if (!fmt || !data || format != 1 || channels != 1 || sampleRate != 48000 || bits != 16 || dataSize == 0 || dataSize > kMaxSamples * 2u || (dataSize & 1u)) { std::fclose(f); return false; }
    samples.resize(dataSize / 2); bool ok = std::fread(samples.data(), 2, samples.size(), f) == samples.size(); std::fclose(f); if (!ok) samples.clear(); return ok;
}
}

extern "C" JNIEXPORT jboolean JNICALL Java_org_retro_flitetts_OpenJtalkPlugin_nativeInit(JNIEnv* env, jclass, jobject assetManager, jstring filesDir) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ready) return JNI_TRUE;
    if (!assetManager || !filesDir) return JNI_FALSE;
    const char* files = env->GetStringUTFChars(filesDir, nullptr); if (!files) return JNI_FALSE;
    g_root = std::string(files) + "/openjtalk"; env->ReleaseStringUTFChars(filesDir, files);
    AAssetManager* manager = AAssetManager_fromJava(env, assetManager); if (!manager) return JNI_FALSE;
    if (mkdir(g_root.c_str(), 0755) != 0 && errno != EEXIST) return JNI_FALSE;
    const std::string dict = g_root + "/dict", voice = g_root + "/voice";
    if (!copyFlatDir(manager, "openjtalk/open_jtalk_dic_utf_8-1.11", dict) || (mkdir(voice.c_str(), 0755) != 0 && errno != EEXIST) || !copyAsset(manager, "openjtalk/voice/nitech_jp_atr503_m001.htsvoice", voice + "/nitech_jp_atr503_m001.htsvoice")) return JNI_FALSE;
    Open_JTalk_initialize(&g_oj);
    std::string voiceFile = voice + "/nitech_jp_atr503_m001.htsvoice";
    if (!Open_JTalk_load(&g_oj, const_cast<char*>(dict.c_str()), const_cast<char*>(voiceFile.c_str()))) { Open_JTalk_clear(&g_oj); return JNI_FALSE; }
    Open_JTalk_set_sampling_frequency(&g_oj, 48000);
    g_ready = true; return JNI_TRUE;
}

extern "C" JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_OpenJtalkPlugin_nativeRender(JNIEnv* env, jclass, jstring text, jint, jfloat rate, jfloat pitch) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_ready || !text) return nullptr;
    const char* input = env->GetStringUTFChars(text, nullptr); if (!input) return nullptr;
    size_t bytes = std::strlen(input); if (bytes == 0 || bytes > kMaxTextBytes) { env->ReleaseStringUTFChars(text, input); return nullptr; }
    double safeRate = std::isfinite(rate) ? std::max(0.05, std::min(20.0, static_cast<double>(rate))) : 1.0;
    double safePitch = std::isfinite(pitch) ? std::max(0.05, std::min(20.0, static_cast<double>(pitch))) : 1.0;
    Open_JTalk_set_speed(&g_oj, safeRate);
    Open_JTalk_add_half_tone(&g_oj, 12.0 * std::log2(safePitch)); // setter semantics: no accumulation.
    const std::string wav = g_root + "/render.wav";
    FILE* out = std::fopen(wav.c_str(), "wb"); int ok = out && Open_JTalk_synthesis(&g_oj, input, out, nullptr); if (out) std::fclose(out); env->ReleaseStringUTFChars(text, input); if (!ok) return nullptr;
    std::vector<short> samples; if (!readWave(wav, samples) || samples.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) return nullptr;
    jshortArray result = env->NewShortArray(static_cast<jsize>(samples.size())); if (result) env->SetShortArrayRegion(result, 0, static_cast<jsize>(samples.size()), samples.data()); return result;
}
