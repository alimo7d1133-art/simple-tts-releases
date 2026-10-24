#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "lame.h"

static int interrupted(JNIEnv *env) {
    jclass thread = (*env)->FindClass(env, "java/lang/Thread");
    if (!thread) return 1;
    jmethodID current = (*env)->GetStaticMethodID(env, thread, "currentThread", "()Ljava/lang/Thread;");
    jmethodID is_interrupted = (*env)->GetMethodID(env, thread, "isInterrupted", "()Z");
    jobject self = current ? (*env)->CallStaticObjectMethod(env, thread, current) : NULL;
    int result = (self && is_interrupted) ? (*env)->CallBooleanMethod(env, self, is_interrupted) : 1;
    if (self) (*env)->DeleteLocalRef(env, self);
    (*env)->DeleteLocalRef(env, thread);
    return result || (*env)->ExceptionCheck(env);
}

JNIEXPORT jint JNICALL Java_org_retro_flitetts_Mp3Encoder_encodeNative
  (JNIEnv *env, jclass klass, jshortArray pcm, jint sample_rate, jstring destination) {
    (void)klass;
    if (!pcm || !destination || sample_rate <= 0) return -1;
    const char *path = (*env)->GetStringUTFChars(env, destination, NULL);
    if (!path) return -1;
    size_t path_len = strlen(path);
    char *tmp = (char *)malloc(path_len + 32);
    if (!tmp) { (*env)->ReleaseStringUTFChars(env, destination, path); return -1; }
    snprintf(tmp, path_len + 32, "%s.tmp.%ld", path, (long)getpid());
    FILE *out = fopen(tmp, "wb");
    if (!out) { free(tmp); (*env)->ReleaseStringUTFChars(env, destination, path); return -3; }

    lame_t lame = lame_init();
    int rc = lame ? 0 : -4;
    unsigned char *encoded = NULL;
    if (lame) {
        lame_set_num_channels(lame, 1);
        lame_set_in_samplerate(lame, sample_rate);
        lame_set_out_samplerate(lame, sample_rate);
        lame_set_brate(lame, 64);
        lame_set_mode(lame, MONO);
        lame_set_VBR(lame, vbr_off);
        lame_set_quality(lame, 5);
        lame_set_write_id3tag_automatic(lame, 0);
        rc = lame_init_params(lame);
    }
    const jsize total = (*env)->GetArrayLength(env, pcm);
    const int chunk = 1152 * 8;
    encoded = (unsigned char *)malloc((size_t)(chunk * 5 / 4 + 7200));
    if (!encoded) rc = -5;
    for (jsize pos = 0; rc == 0 && pos < total; pos += chunk) {
        if (interrupted(env)) { rc = -2; break; }
        jsize n = total - pos; if (n > chunk) n = chunk;
        jshort *samples = (*env)->GetPrimitiveArrayCritical(env, pcm, NULL);
        if (!samples) { rc = -6; break; }
        int bytes = lame_encode_buffer(lame, samples + pos, NULL, (int)n, encoded, chunk * 5 / 4 + 7200);
        (*env)->ReleasePrimitiveArrayCritical(env, pcm, samples, JNI_ABORT);
        if (bytes < 0 || (bytes && fwrite(encoded, 1, (size_t)bytes, out) != (size_t)bytes)) rc = -7;
    }
    if (rc == 0) {
        int bytes = lame_encode_flush(lame, encoded, chunk * 5 / 4 + 7200);
        if (bytes < 0 || (bytes && fwrite(encoded, 1, (size_t)bytes, out) != (size_t)bytes)) rc = -8;
    }
    if (out) { if (fclose(out) != 0 && rc == 0) rc = -9; }
    if (lame) lame_close(lame);
    free(encoded);
    if (rc == 0 && rename(tmp, path) != 0) rc = -10;
    if (rc != 0) unlink(tmp);
    free(tmp);
    (*env)->ReleaseStringUTFChars(env, destination, path);
    return rc;
}
