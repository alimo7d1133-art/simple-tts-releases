package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Real Open JTalk/Nitech smoke test: init, Japanese synthesis, controls and determinism. */
public final class OpenJtalkInstrumentation extends Instrumentation {
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private void note(String message) { Bundle b = new Bundle(); b.putString("stream", message + "\n"); sendStatus(1, b); android.util.Log.i("OldTTS-OpenJtalk", message); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        try {
            Context c = getTargetContext();
            EnginePlugin e = EngineRegistry.find("openjtalk");
            require(e != null, "Open JTalk is not registered");
            require(e.voiceIds().length == 1 && "ja-JP-nitech".equals(e.voiceIds()[0]), "unexpected voice set");
            require(e.languageAvailability("ja", "JP", "") >= 0, "Japanese unavailable");
            require(e.languageAvailability("ar", "", "") < 0, "Arabic must not be advertised");
            EngineRegistry.prepare(c, e);
            String text = e.sampleText(0);
            require(e.acceptsText(text, 0), "fixture rejected");
            long start = System.currentTimeMillis();
            short[] a = e.synthesize(text, 0, 1.0f, 1.0f);
            long elapsed = System.currentTimeMillis() - start;
            require(a != null && a.length > 1000, "empty Nitech PCM");
            require(e.sampleRate(0) == 48000, "wrong Open JTalk sample rate");
            short[] b = e.synthesize(text, 0, 1.25f, .8f);
            require(b != null && b.length > 1000, "parameterized synthesis failed");
            require(!Arrays.equals(a, b), "rate/pitch controls had no effect");
            File out = new File(c.getFilesDir(), "openjtalk-tests");
            require(out.isDirectory() || out.mkdirs(), "cannot create " + out);
            writeText(new File(out, "sample-ja.txt"), text);
            writeWav(new File(out, "nitech-1x-48000-off.wav"), a, 48000);
            Mp3Encoder.encode(a, 48000, new File(out, "nitech-1x-48000-off.mp3"));
            short[] hq = AudioResampler.to48k(a, e.sampleRate(0), () -> false);
            require(hq.length == a.length && Arrays.equals(hq, a), "48k on/off conversion changed native 48k PCM");
            writeWav(new File(out, "nitech-1x-48000-on.wav"), hq, 48000);
            Mp3Encoder.encode(hq, 48000, new File(out, "nitech-1x-48000-on.mp3"));
            writeWav(new File(out, "nitech-1.25x-0.8pitch-48000.wav"), b, 48000);
            require(new File(out, "nitech-1x-48000-off.mp3").length() > 128, "MP3 artifact is empty");
            note("PASS openjtalk:ja-JP-nitech rate=48000Hz samples=" + a.length + " parameterized=" + b.length + " elapsedMs=" + elapsed + " artifacts=" + out);
            finish(Activity.RESULT_OK, new Bundle());
        } catch (Throwable t) {
            note("FAIL " + t);
            Bundle b = new Bundle(); b.putString("error", t.toString()); finish(Activity.RESULT_CANCELED, b);
        }
    }

    private static void writeText(File f, String text) throws IOException { try (FileWriter w = new FileWriter(f)) { w.write(text); w.write('\n'); } }
    private static void writeWav(File f, short[] pcm, int rate) throws IOException {
        try (FileOutputStream o = new FileOutputStream(f)) {
            int data = pcm.length * 2;
            ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
            h.put(new byte[]{'R','I','F','F'}).putInt(36 + data).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(rate).putInt(rate * 2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(data);
            o.write(h.array());
            ByteBuffer b = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
            for (short s : pcm) { if (b.remaining() < 2) { o.write(b.array(), 0, b.position()); b.clear(); } b.putShort(s); }
            if (b.position() > 0) o.write(b.array(), 0, b.position());
        }
    }
}
