/* GnuSpeechSA Android adapter. GPL-3.0-or-later; see GNUSPEECH-COPYING.txt. */
package org.retro.flitetts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class GnuspeechPlugin implements EnginePlugin {
  private static final String[] IDS = {"gnuspeechsa-en-male"};
  private static final String[] LABELS = {"GnuSpeechSA — articulatory English (GPLv3+ male)"};
  private static volatile boolean initialized;
  static { System.loadLibrary("gnuspeech_jni"); }
  private static native void nativeInitialize(String configDir);
  private static native short[] nativeRender(String text, int voice, float rate, float pitch);
  private static native int nativeSampleRate();

  @Override public void initialize(Context context) {
    if (initialized) return;
    try {
      File dir = new File(context.getFilesDir(), "gnuspeech/data/en");
      copyAssets(context, "gnuspeech/data/en", dir);
      nativeInitialize(dir.getAbsolutePath());
      initialized = true;
    } catch (IOException e) { throw new IllegalStateException("GnuSpeechSA data unavailable", e); }
  }
  private static void copyAssets(Context c, String assetPath, File out) throws IOException {
    String[] children = c.getAssets().list(assetPath);
    if (children == null || children.length == 0) {
      out.getParentFile().mkdirs();
      try (InputStream in = c.getAssets().open(assetPath); FileOutputStream f = new FileOutputStream(out)) {
        byte[] b = new byte[8192]; for (int n; (n = in.read(b)) > 0;) f.write(b, 0, n);
      }
      return;
    }
    if (!out.exists() && !out.mkdirs()) throw new IOException("Cannot create " + out);
    for (String child : children) copyAssets(c, assetPath + "/" + child, new File(out, child));
  }
  public String id() { return "gnuspeechsa"; }
  public String label() { return "GnuSpeechSA — محاكاة أعضاء النطق الكلاسيكية"; }
  public Locale locale() { return Locale.US; }
  public String[] voiceIds() { return IDS.clone(); }
  public String[] voiceLabels() { return LABELS.clone(); }
  public Locale voiceLocale(int voice) { checkVoice(voice); return Locale.US; }
  public String sampleText() { return sampleText(0); }
  public String sampleText(int voice) { checkVoice(voice); return "Hello from the real GnuSpeechSA articulatory synthesizer."; }
  public String languageNotice() { return "GnuSpeechSA: صوت إنجليزي واحد يحاكي أعضاء النطق. لا يدعم العربية."; }
  public String[] noticeAssets() { return new String[]{"GNUSPEECH-COPYING.txt", "GNUSPEECH-SOURCE-NOTICE.txt"}; }
  public int languageAvailability(String language, String country, String variant) {
    if(!"en".equalsIgnoreCase(language)&&!"eng".equalsIgnoreCase(language))return TextToSpeech.LANG_NOT_SUPPORTED;return "US".equalsIgnoreCase(country)||"USA".equalsIgnoreCase(country)?TextToSpeech.LANG_COUNTRY_AVAILABLE:TextToSpeech.LANG_AVAILABLE;
  }
  public boolean acceptsText(String text) { if(text==null||text.trim().isEmpty())return false;for(int at=0;at<text.length();){int cp=text.codePointAt(at);at+=Character.charCount(cp);if(Character.isLetter(cp)&&Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)return false;}return true; }
  public int sampleRate(int voice) { checkVoice(voice); return nativeSampleRate(); }
  public short[] synthesize(String text, int voice, float rate, float pitch) {
    checkVoice(voice); if (!acceptsText(text)) throw new IllegalArgumentException("Empty text");
    return nativeRender(text, voice, rate, pitch);
  }
  private static void checkVoice(int voice) { if (voice != 0) throw new IllegalArgumentException("Unknown GnuSpeechSA voice"); }
}
