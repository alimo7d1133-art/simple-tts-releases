/* JNI adapter for rsynth with one Bionic FPCHECK portability patch. rsynth upstream
 * source and COPYING/COPYING.LIB are kept under cpp/rsynth/. */
#include <jni.h>
#include <math.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include "rsynth/rsynth.h"
#include "rsynth/say.h"
#include "rsynth/english.h"
lang_t *lang = &English;

#define RSYNTH_RATE 16000L
struct rsynth_capture { short *data; size_t count; size_t capacity; };
static pthread_mutex_t rsynth_lock = PTHREAD_MUTEX_INITIALIZER;
static void *capture_sample(void *opaque, float sample, unsigned nsamp, rsynth_t *r) {
  struct rsynth_capture *c = (struct rsynth_capture *)opaque;
  /* rsynth passes a cumulative frame count; each callback carries one sample. */
  if (c->count + 1 > c->capacity) {
    size_t n = c->capacity ? c->capacity : 4096;
    while (n < c->count + 1) n *= 2;
    short *p = (short *)realloc(c->data, n * sizeof(*p));
    if (!p) return NULL;
    c->data = p; c->capacity = n;
  }
  float v = sample;
  if (v > 32767.0f) v = 32767.0f;
  if (v < -32768.0f) v = -32768.0f;
  c->data[c->count++] = (short)lrintf(v);
  return opaque;
}
static void *capture_flush(void *opaque, unsigned nsamp, rsynth_t *r) { return opaque; }
JNIEXPORT jint JNICALL Java_org_retro_flitetts_RsynthNative_sampleRate(JNIEnv *e, jclass c, jint voice) { return RSYNTH_RATE; }
JNIEXPORT jshortArray JNICALL Java_org_retro_flitetts_RsynthNative_render(JNIEnv *e, jclass cls, jstring text, jint voice, jfloat rate, jfloat pitch) {
  if (!text) return NULL;
  pthread_mutex_lock(&rsynth_lock);
  const char *utf = (*e)->GetStringUTFChars(e, text, NULL);
  if (!utf) { pthread_mutex_unlock(&rsynth_lock); return NULL; }
  struct rsynth_capture cap = {0};
  float f0 = 110.0f * (pitch > 0.1f ? pitch : 1.0f);
  rsynth_t *r = rsynth_init(RSYNTH_RATE, 10.0f, rsynth_speaker(f0, 60.0f, Elements), capture_sample, capture_flush, &cap);
  if (r) { r->speed = rate > 0.1f ? rate : 1.0f; say_string(r, (char *)utf); rsynth_term(r); }
  (*e)->ReleaseStringUTFChars(e, text, utf);
  jshortArray out = NULL;
  if (cap.data && r) { out = (*e)->NewShortArray(e, (jsize)cap.count); if (out) (*e)->SetShortArrayRegion(e, out, 0, (jsize)cap.count, cap.data); }
  free(cap.data);
  pthread_mutex_unlock(&rsynth_lock);
  return out;
}
