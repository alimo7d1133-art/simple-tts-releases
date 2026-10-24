package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** End-to-end checks for optional offline Rawi Arabic diacritization. */
public final class RawiInstrumentation extends Instrumentation {
    private static final String TAG = "OldTTS-Rawi";
    private static final String ARABIC = "السلام عليكم 123 ABC";
    private static final String VOWELLED = "السَّلَام عَلَيْكُمْ";

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
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
        Activity activity = null;
        Bundle result = new Bundle();
        try {
            Context context = getTargetContext();
            verifyLocalizedResources(context);
            verifyDiacritizer(context);
            verifyAudio(context);
            activity = verifyUi(context);
            result.putString("stream", "PASS Rawi inference, preservation, UI, localized resources, and MP3/PCM checks\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("error", failure.toString());
            note("FAIL " + failure);
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) {
                final Activity toFinish = activity;
                runOnMainSync(toFinish::finish);
            }
        }
    }

    private void verifyLocalizedResources(Context target) {
        String[] tags = {"en", "ar", "de", "es", "fr", "it"};
        String[] keys = {"rawi_toggle", "rawi_hint", "rawi_processing", "rawi_failed"};
        for (String tag : tags) {
            Configuration configuration = new Configuration(target.getResources().getConfiguration());
            configuration.setLocale(new Locale(tag));
            Context localized = target.createConfigurationContext(configuration);
            for (String key : keys) {
                int id = localized.getResources().getIdentifier(key, "string", target.getPackageName());
                require(id != 0 && !localized.getString(id).trim().isEmpty(), tag + " missing " + key);
            }
        }
        note("PASS Rawi resource keys in six locales");
    }

    private void verifyDiacritizer(Context context) throws Exception {
        String output = RawiDiacritizer.diacritize(context, ARABIC);
        require(output != null && output.length() > ARABIC.length(), "unvowelled Arabic did not gain diacritics");
        require(hasArabicMark(output), "Rawi output has no Arabic diacritic mark");
        require(stripMarks(output).equals(ARABIC), "Arabic/Latin/digit characters changed: " + output);

        String vowelledOutput = RawiDiacritizer.diacritize(context, VOWELLED);
        String[] inputWords = VOWELLED.split(" ");
        String[] outputWords = vowelledOutput.split(" ");
        require(inputWords.length == outputWords.length, "Rawi changed explicitly vowelled word boundaries");
        for (int i = 0; i < inputWords.length; i++) {
            require(stripMarks(outputWords[i]).equals(stripMarks(inputWords[i])),
                    "vowelled word letters changed at " + i + ": " + outputWords[i]);
            require(outputWords[i].equals(inputWords[i]), "vowelled word was not preserved: " + outputWords[i]);
        }
        note("PASS Rawi Arabic marks and per-word preservation");
    }

    private void verifyAudio(Context context) throws Exception {
        EnginePlugin mbrola = EngineRegistry.find("mbrola");
        require(mbrola != null, "MBROLA registry entry missing");
        File output = new File(context.getExternalFilesDir(null), "rawi-mp3-tests");
        require(output.exists() || output.mkdirs(), "cannot create " + output);
        for (int voice : new int[] {1, 2}) {
            String text = "السلام عليكم 123 ABC";
            StandaloneAudio.Pcm pcm = StandaloneAudio.render(context, mbrola, voice, text, 1f, 1f, true, () -> false);
            require(pcm != null && pcm.samples.length > 0, "empty Rawi PCM AR" + voice);
            require(energy(pcm.samples) > 1000, "silent Rawi PCM AR" + voice);
            File mp3 = new File(output, "AR" + voice + "-rawi.mp3");
            Mp3Encoder.encode(pcm.samples, pcm.sampleRate, mp3);
            require(mp3.isFile() && mp3.length() > 128 && hasMp3Header(mp3), "invalid Rawi MP3 " + mp3);
        }
        String english = "Hello 123 ABC.";
        StandaloneAudio.Pcm without = StandaloneAudio.render(context, mbrola, 0, english, 1f, 1f, false, () -> false);
        StandaloneAudio.Pcm with = StandaloneAudio.render(context, mbrola, 0, english, 1f, 1f, true, () -> false);
        require(without.sampleRate == with.sampleRate && energy(without.samples)>1000 && energy(with.samples)>1000, "English render failed with a Rawi setting");
        require(Math.abs(without.samples.length-with.samples.length) < Math.max(1000,without.samples.length/20), "Unexpected English duration change");
        note("PASS AR1/AR2 Rawi MP3 and English render with either Rawi setting (native waveform is non-deterministic)");
    }

    private Activity verifyUi(Context context) throws Exception {
        context.getSharedPreferences("retro", Context.MODE_PRIVATE).edit()
                .putBoolean("rawi_enabled", false).putString("engine", "flite").apply();
        Activity activity = startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        Spinner engines = findSpinner(activity.getWindow().getDecorView(), activity.getString(R.string.engine));
        Spinner voices = findSpinner(activity.getWindow().getDecorView(), activity.getString(R.string.voice));
        require(engines != null && voices != null, "engine/voice Spinner missing");
        int mbrolaPosition = -1;
        for (int i = 0; i < engines.getAdapter().getCount(); i++) {
            if (String.valueOf(engines.getAdapter().getItem(i)).toUpperCase(Locale.ROOT).contains("MBROLA")) mbrolaPosition = i;
        }
        require(mbrolaPosition >= 0, "MBROLA Spinner item missing");
        final int enginePosition = mbrolaPosition;
        runOnMainSync(() -> engines.setSelection(enginePosition));
        waitForIdleSync();
        require(waitForVoice(voices, "AR1"), "AR1 voice missing");
        Switch toggle = findSwitch(activity.getWindow().getDecorView());
        require(toggle != null && toggle.getVisibility() == View.VISIBLE, "Rawi toggle not visible for AR1");
        runOnMainSync(() -> voices.setSelection(2));
        waitForIdleSync();
        require(toggle.getVisibility() == View.VISIBLE, "Rawi toggle not visible for AR2");
        runOnMainSync(() -> toggle.setChecked(true));
        waitForIdleSync();
        require(context.getSharedPreferences("retro", Context.MODE_PRIVATE).getBoolean("rawi_enabled", false), "Rawi preference not saved");
        runOnMainSync(() -> voices.setSelection(0));
        waitForIdleSync();
        require(toggle.getVisibility() == View.GONE, "Rawi toggle not hidden for English");
        note("PASS UI MBROLA AR1/AR2 visibility, English hiding, and preference");
        return activity;
    }

    private boolean waitForVoice(Spinner spinner, String id) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
                if (String.valueOf(spinner.getAdapter().getItem(i)).toUpperCase(Locale.ROOT).contains(id.toUpperCase(Locale.ROOT))) {
                    final int position = i;
                    runOnMainSync(() -> spinner.setSelection(position));
                    waitForIdleSync();
                    return true;
                }
            }
            Thread.sleep(25);
        }
        return false;
    }

    private static Spinner findSpinner(View view, String description) {
        if (view instanceof Spinner && description.equals(String.valueOf(view.getContentDescription()))) return (Spinner) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Spinner found = findSpinner(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Switch findSwitch(View view) {
        if (view instanceof Switch) return (Switch) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Switch found = findSwitch(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasArabicMark(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if ((cp >= 0x064B && cp <= 0x065F) || cp == 0x0670 || (cp >= 0x06D6 && cp <= 0x06ED)) return true;
        }
        return false;
    }

    private static String stripMarks(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if (!((cp >= 0x064B && cp <= 0x065F) || cp == 0x0670 || (cp >= 0x06D6 && cp <= 0x06ED))) out.appendCodePoint(cp);
        }
        return out.toString();
    }

    private static long energy(short[] samples) {
        long total = 0;
        for (short sample : samples) total += Math.abs((long) sample);
        return total;
    }

    private static boolean hasMp3Header(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            int a = in.read(), b = in.read(), c = in.read();
            return (a == 'I' && b == 'D' && c == '3') || (a == 0xff && (b & 0xe0) == 0xe0);
        }
    }
}
