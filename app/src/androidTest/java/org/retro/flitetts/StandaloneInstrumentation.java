package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/** End-to-end checks for the direct, non-Android-TTS standalone pipeline. */
public final class StandaloneInstrumentation extends Instrumentation {
    private static final String TAG = "OldTTS-Standalone-1.3.1";

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void note(String message) {
        Bundle status = new Bundle();
        status.putString("stream", message + "\n");
        sendStatus(1, status);
        android.util.Log.i(TAG, message);
    }

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        try {
            Context context = getTargetContext();
            verifyNotAnAndroidTtsEngine(context);
            verifyRegistry();
            verifyCancellationAndValidation(context);
            verifyAllVoices(context);
            verifyArabicRatePitchAndChunking(context);
            verifyUi(context);
            note("PASS standalone instrumentation 1.3.1");
            Bundle result=new Bundle();result.putString("stream","PASS standalone instrumentation 1.3.1\n");finish(Activity.RESULT_OK,result);
        } catch (Throwable failure) {
            note("FAIL " + failure);
            Bundle result=new Bundle();result.putString("error",failure.toString());finish(Activity.RESULT_CANCELED,result);
        }
    }

    private void verifyNotAnAndroidTtsEngine(Context context) throws Exception {
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent("android.intent.action.TTS_SERVICE");
        query.setPackage(context.getPackageName());
        List<?> services = pm.queryIntentServices(query, PackageManager.MATCH_ALL);
        require(services == null || services.isEmpty(),
                "standalone package advertises an Android TTS service");
        PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SERVICES);
        require(info.services == null || info.services.length == 0,
                "standalone package declares services");
        note("PASS package has no TTS intent service or declared service");
    }

    private void verifyRegistry() {
        require(EngineRegistry.all().size() == 12, "expected 12 engines");
        int voices = 0;
        for (EnginePlugin engine : EngineRegistry.all()) {
            require(engine.voiceIds().length == engine.voiceLabels().length,
                    "voice metadata mismatch for " + engine.id());
            voices += engine.voiceIds().length;
        }
        require(voices == 63, "expected 63 voices, got " + voices);
        note("PASS registry engines=11 voices=63");
    }

    private void verifyCancellationAndValidation(Context context) throws Exception {
        EnginePlugin engine = EngineRegistry.all().get(0);
        StandaloneAudio.Cancellation alreadyCancelled = () -> true;
        boolean cancelled = false;
        try {
            StandaloneAudio.render(context, engine, 0, engine.sampleText(0), 1f, 1f,
                    alreadyCancelled);
        } catch (CancellationException expected) {
            cancelled = true;
        }
        require(cancelled, "cancellation before render was ignored");
        String[] invalid = {null, "   ", "\u0000", repeat("x", StandaloneAudio.MAX_TEXT_LENGTH + 1)};
        for (String text : invalid) {
            boolean rejected = false;
            try {
                StandaloneAudio.render(context, engine, 0, text, 1f, 1f, () -> false);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "invalid text accepted: " + (text == null ? "null" : "invalid"));
        }
        note("PASS cancellation and invalid-text validation");
    }

    private void verifyAllVoices(Context context) throws Exception {
        File output = new File(context.getExternalFilesDir(null), "standalone-mp3-tests");
        require(output.exists() || output.mkdirs(), "cannot create " + output);
        int rendered = 0;
        for (EnginePlugin engine : EngineRegistry.all()) {
            for (int voice = 0; voice < engine.voiceIds().length; voice++) {
                String text = engine.sampleText(voice);
                StandaloneAudio.Pcm pcm = StandaloneAudio.render(context, engine, voice,
                        text, 1f, 1f, () -> false);
                require(pcm != null && pcm.samples.length > 0, "empty PCM " + engine.id() + ":" + voice);
                require(pcm.sampleRate >= 8000 && pcm.sampleRate <= 96000,
                        "bad sample rate " + engine.id() + ":" + pcm.sampleRate);
                require(energy(pcm.samples) > 1000, "silent PCM " + engine.id() + ":" + voice);
                File mp3 = new File(output, engine.id() + "-" + engine.voiceIds()[voice] + ".mp3");
                Mp3Encoder.encode(pcm.samples, pcm.sampleRate, mp3);
                require(mp3.isFile() && mp3.length() > 128,
                        "tiny MP3 " + engine.id() + ":" + voice + " size=" + mp3.length());
                require(hasMp3Header(mp3), "invalid MP3 header " + mp3);
                rendered++;
                note("PASS voice engine=" + engine.id() + " voice=" + engine.voiceIds()[voice]
                        + " hz=" + pcm.sampleRate + " samples=" + pcm.samples.length
                        + " mp3=" + mp3.length());
            }
        }
        require(rendered == 63, "rendered " + rendered + " voices, expected 63");
    }

    private void verifyArabicRatePitchAndChunking(Context context) throws Exception {
        EnginePlugin arabic = EngineRegistry.find("mbrola");
        require(arabic != null, "MBROLA registry entry missing");
        StandaloneAudio.Pcm arabicPcm = StandaloneAudio.render(context, arabic, 1,
                arabic.sampleText(1), .65f, 1.35f, () -> false);
        require(energy(arabicPcm.samples) > 1000, "silent Arabic rate/pitch render");

        EnginePlugin chunkEngine = EngineRegistry.all().get(0);
        String longText = repeat(chunkEngine.sampleText(0) + " ", 35);
        require(longText.length() > 160, "chunk fixture is too short");
        StandaloneAudio.Pcm chunked = StandaloneAudio.render(context, chunkEngine, 0,
                longText, 1.1f, .9f, () -> false);
        require(chunked.samples.length > 2000 && energy(chunked.samples) > 1000,
                "long text chunk render was empty or silent");
        note("PASS Arabic rate/pitch and long-text chunk render");
    }

    private void verifyUi(Context context) throws Exception {
        Activity activity = startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        View root = activity.getWindow().getDecorView();
        require(count(root, Spinner.class) == 4, "UI must expose 4 spinners (language, engine, installed-engine picker, voice)");
        require(count(root, SeekBar.class) == 2, "UI must expose 2 seekbars");
        require(count(root, Button.class) == 7, "UI must expose 7 buttons (excluding Switch controls)");
        require(find(root, EditText.class) != null, "UI missing sample editor");
        require(activity.getString(R.string.standalone_notice).length() > 0,
                "standalone_notice resource missing");
        require(activity.getString(R.string.save_mp3).length() > 0,
                "save_mp3 resource missing");

        Method start = MainActivity.class.getDeclaredMethod("startAudio", boolean.class);
        Method stop = MainActivity.class.getDeclaredMethod("stopAudio");
        Field busy = MainActivity.class.getDeclaredField("busy");
        start.setAccessible(true); stop.setAccessible(true); busy.setAccessible(true);
        runOnMainSync(() -> {
            try { start.invoke(activity, false); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        boolean observedBusy = false;
        while (System.nanoTime() < deadline) {
            if ((Boolean) busy.get(activity)) { observedBusy = true; break; }
            Thread.sleep(25);
        }
        runOnMainSync(() -> {
            try { stop.invoke(activity); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        require(observedBusy, "listen did not enter busy state");
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while ((Boolean) busy.get(activity) && System.nanoTime() < deadline) Thread.sleep(25);
        require(!((Boolean) busy.get(activity)), "stop did not clear busy state");
        runOnMainSync(activity::finish);
        note("PASS UI controls and bounded listen/stop reflection check");
    }

    private static long energy(short[] samples) {
        long total = 0;
        for (short sample : samples) total += Math.abs((long) sample);
        return total;
    }

    private static boolean hasMp3Header(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            int a = in.read(), b = in.read(), c = in.read();
            if (a == 'I' && b == 'D' && c == '3') return true;
            return a == 0xff && (b & 0xe0) == 0xe0;
        }
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    private static int count(View root, Class<?> type) {
        int result = type.isInstance(root) && !(type == Button.class && root instanceof android.widget.CompoundButton) ? 1 : 0;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) result += count(group.getChildAt(i), type);
        }
        return result;
    }

    private static <T> T find(View root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = find(group.getChildAt(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
