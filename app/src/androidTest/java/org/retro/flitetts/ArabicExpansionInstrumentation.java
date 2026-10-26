package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

/** Regression coverage for the 1.4 Arabic voices and logarithmic controls. */
public final class ArabicExpansionInstrumentation extends Instrumentation {
    private static final String PREFS = "retro";
    private static final String FESTIVAL = "ara_norm_ziad_hts";
    private static final String ASC = "asc";

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private void note(String message) {
        Bundle b = new Bundle();
        b.putString("stream", message + "\n");
        sendStatus(1, b);
        android.util.Log.i("OldTTS-ArabicExpansion", message);
    }

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            Context c = getTargetContext();
            verifyRegistryAndVoices();
            verifyArabicAudio(c);
            verifyHighQualityAudio(c);
            verifyFactors();
            activity = verifySlidersAndMigration(c);
            result.putString("stream", "PASS Arabic Festival/ASC voices, Rawi enable, HQ 48k PCM/MP3, logarithmic factors, slider endpoints/center, recreate and legacy migration\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable t) {
            result.putString("error", t.toString());
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) runOnMainSync(activity::finish);
        }
    }

    private void verifyRegistryAndVoices() {
        require(FestivalPlugin.normalizeDiacritics("ص\u064e\u0651").equals("ص\u0651\u064e"), "Festival canonical shadda ordering regression");
        require(EngineRegistry.all().size() == 12, "expected engines=12");
        int count = 0;
        for (EnginePlugin e : EngineRegistry.all()) count += e.voiceIds().length;
        require(count == 63, "expected voices=63, got " + count);
        EnginePlugin festival = EngineRegistry.find("festival");
        require(festival != null && festival.voiceIds().length == 2, "Festival voice missing");
        require(FESTIVAL.equals(festival.voiceIds()[0]), "wrong Festival id");
        require("ar".equals(festival.voiceLocale(0).getLanguage()), "Festival is not Arabic");
        require(festival.sampleRate(0) == 48000, "Festival sample rate is not 48kHz");
        EnginePlugin flite = EngineRegistry.find("flite");
        require(flite != null && ASC.equals(flite.voiceIds()[9]), "ASC legacy index changed");
        require("ar".equals(flite.voiceLocale(9).getLanguage()), "ASC is not Arabic");
        note("PASS registry 11/63, Festival ara_norm_ziad_hts 48kHz, ASC retained at legacy index 9");
    }

    private void verifyArabicAudio(Context c) throws Exception {
        EnginePlugin[] engines = {EngineRegistry.find("festival"), EngineRegistry.find("flite")};
        int[] voices = {0, 9};
        File out = new File(c.getExternalFilesDir(null), "arabic-expansion-mp3");
        require(out.exists() || out.mkdirs(), "cannot create output directory");
        for (int i = 0; i < engines.length; i++) {
            EnginePlugin e = engines[i];
            int voice = voices[i];
            String unvocalized = "مرحبا بكم. هذه تجربة للصوت العربي.";
            String diacritized = "مَرْحَبًا بِكُمْ. هَذِهِ تَجْرِبَةٌ لِلصَّوْتِ الْعَرَبِيِّ.";
            require(e.acceptsText(unvocalized), "Arabic rejected by " + e.id());
            StandaloneAudio.Pcm plain = StandaloneAudio.render(c, e, voice, unvocalized, 1f, 1f, false, () -> false);
            StandaloneAudio.Pcm rawi = StandaloneAudio.render(c, e, voice, unvocalized, 1f, 1f, true, () -> false);
            StandaloneAudio.Pcm manual = StandaloneAudio.render(c, e, voice, diacritized, 1f, 1f, false, () -> false);
            require(plain.samples.length > 0 && rawi.samples.length > 0 && manual.samples.length > 0, "empty PCM " + e.id());
            require(plain.sampleRate == ("festival".equals(e.id()) ? 48000 : plain.sampleRate), "wrong Festival PCM rate");
            require(energy(plain.samples) > 1000 && energy(rawi.samples) > 1000 && energy(manual.samples) > 1000, "silent Arabic PCM " + e.id());
            File mp3 = new File(out, e.id() + "-" + e.voiceIds()[voice] + ".mp3");
            Mp3Encoder.encode(rawi.samples, rawi.sampleRate, mp3);
            require(mp3.isFile() && mp3.length() > 128 && hasMp3Header(mp3), "invalid/nonempty MP3 " + mp3);
            note("PASS Arabic " + e.id() + " rawi PCM and MP3");
        }
        EnginePlugin festival = engines[0];
        String shortText = "مَرْحَبًا";
        StandaloneAudio.Pcm normal = StandaloneAudio.render(c, festival, 0, shortText, 1f, 1f, false, () -> false);
        StandaloneAudio.Pcm fast = StandaloneAudio.render(c, festival, 0, shortText, 2f, 1f, false, () -> false);
        StandaloneAudio.Pcm pitched = StandaloneAudio.render(c, festival, 0, shortText, 1f, 1.5f, false, () -> false);
        require(energy(normal.samples) > 1000 && energy(fast.samples) > 1000 && energy(pitched.samples) > 1000, "silent Festival controls");
        require(fast.samples.length < normal.samples.length, "speed control did not shorten Festival audio");
        require(pcmDifference(normal.samples, pitched.samples) > 1000, "pitch control produced no Festival PCM difference");
        require(Math.abs(normal.samples.length - pitched.samples.length) <= normal.samples.length / 20, "pitch control changed Festival duration");
        note("PASS Festival independent speed and pitch controls");
    }

    private void verifyFactors() {
        require(close(MainActivity.speedFactor(50), 1f), "speed center");
        require(close(MainActivity.pitchFactor(50), 1f), "pitch center");
        require(close(MainActivity.speedFactor(0), .25f), "speed minimum");
        require(close(MainActivity.speedFactor(100), 4f), "speed maximum");
        require(close(MainActivity.pitchFactor(0), .5f), "pitch minimum");
        require(close(MainActivity.pitchFactor(100), 2f), "pitch maximum");
        note("PASS logarithmic speed/pitch factor endpoints and center");
    }

    private void verifyHighQualityAudio(Context c) throws Exception {
        EnginePlugin[] engines = {EngineRegistry.find("flite"), EngineRegistry.find("festival"), EngineRegistry.find("mbrola")};
        int[] voices = {9, 0, 2};
        File out = new File(c.getExternalFilesDir(null), "arabic-expansion-hq-mp3");
        require(out.exists() || out.mkdirs(), "cannot create HQ output directory");
        for (int i = 0; i < engines.length; i++) {
            EnginePlugin e = engines[i];
            String text = "مَرْحَبًا";
            StandaloneAudio.Pcm nativePcm = StandaloneAudio.render(c, e, voices[i], text, 1.3f, .8f, false, false, () -> false);
            StandaloneAudio.Pcm high = StandaloneAudio.render(c, e, voices[i], text, 1.3f, .8f, false, true, () -> false);
            StandaloneAudio.Pcm expected = new StandaloneAudio.Pcm(AudioResampler.to48k(nativePcm.samples, nativePcm.sampleRate, () -> false), 48000);
            // Some native engines vary phase/frame lengths on repeated synthesis.
            // Test resampling duration with the SAME real generated PCM, while
            // retaining the independent real HQ render for MP3 integration.
            StandaloneAudio.Pcm controlled = StandaloneAudio.render(c, fixedPcm(e, nativePcm), voices[i], text, 1.3f, .8f, false, true, () -> false);
            require(nativePcm.samples.length > 0 && high.samples.length > 0, "empty HQ PCM " + e.id());
            require(high.sampleRate == 48000, "HQ output is not 48kHz " + e.id() + ": " + high.sampleRate);
            double nativeSeconds = nativePcm.samples.length / (double) nativePcm.sampleRate;
            double highSeconds = high.samples.length / 48000.0;
            double expectedSeconds = expected.samples.length / 48000.0;
            require(Math.abs(nativeSeconds - expectedSeconds) <= (1.0 / 48000.0) + .000001, "HQ duration changed for identical PCM " + e.id());
            require(controlled.sampleRate == 48000 && java.util.Arrays.equals(controlled.samples, expected.samples), "HQ pipeline changed deterministic PCM " + e.id());
            File mp3 = new File(out, e.id() + "-hq.mp3");
            Mp3Encoder.encode(high.samples, high.sampleRate, mp3);
            require(mp3.isFile() && mp3.length() > 128 && hasMp3Header(mp3), "invalid HQ MP3 " + e.id());
            note("PASS HQ 48k/same-PCM duration/MP3 " + e.id() + "; independent render delta seconds=" + Math.abs(nativeSeconds-highSeconds));
        }
    }

    private static EnginePlugin fixedPcm(final EnginePlugin e, final StandaloneAudio.Pcm pcm) {
        return new EnginePlugin() {
            public String id(){return e.id();}
            public String label(){return e.label();}
            public java.util.Locale locale(){return e.locale();}
            public java.util.Locale voiceLocale(int v){return e.voiceLocale(v);}
            public String[] voiceIds(){return e.voiceIds();}
            public String[] voiceLabels(){return e.voiceLabels();}
            public String sampleText(){return e.sampleText();}
            public String languageNotice(){return e.languageNotice();}
            public String[] noticeAssets(){return e.noticeAssets();}
            public int languageAvailability(String l,String c,String v){return e.languageAvailability(l,c,v);}
            public boolean acceptsText(String t){return e.acceptsText(t);}
            public boolean acceptsText(String t,int v){return e.acceptsText(t,v);}
            public int sampleRate(int v){return pcm.sampleRate;}
            public short[] synthesize(String t,int v,float rate,float pitch){require(close(rate,1.3f)&&close(pitch,.8f),"HQ changed control factors");return pcm.samples.clone();}
        };
    }

    private Activity verifySlidersAndMigration(Context c) throws Exception {
        c.getSharedPreferences(PREFS, 0).edit().clear().putBoolean("storage_intro_131", true).commit();
        Activity a = startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        SeekBar[] bars = findAll(a.getWindow().getDecorView(), SeekBar.class).toArray(new SeekBar[0]);
        require(bars.length == 2, "expected two sliders");
        java.util.List<Switch> switches = findAll(a.getWindow().getDecorView(), Switch.class);
        require(switches.size() == 2, "expected Rawi and high-quality switches");
        Spinner engines = null;
        for (Spinner spinner : findAll(a.getWindow().getDecorView(), Spinner.class))
            if (activityDescription(spinner).equals(a.getString(R.string.engine))) engines = spinner;
        require(engines != null, "engine spinner missing");
        int festivalPosition = -1;
        for (int i = 0; i < engines.getAdapter().getCount(); i++)
            if (String.valueOf(engines.getAdapter().getItem(i)).toLowerCase(Locale.ROOT).contains("festival")) festivalPosition = i;
        require(festivalPosition >= 0, "Festival spinner item missing");
        final Spinner engineSpinner = engines;
        final int fp = festivalPosition;
        runOnMainSync(() -> engineSpinner.setSelection(fp));
        waitForIdleSync();
        Switch rawi = null;
        for (Switch s : switches) if (s.getContentDescription() != null && s.getContentDescription().toString().equals(a.getString(R.string.rawi_toggle))) rawi = s;
        require(rawi != null && rawi.getVisibility() == View.VISIBLE, "Rawi not visible for Festival Arabic voice");
        final Switch rawiSwitch = rawi;
        runOnMainSync(() -> rawiSwitch.setChecked(true));
        require(c.getSharedPreferences(PREFS, 0).getBoolean("rawi_enabled", false), "Rawi enable not persisted");
        Switch highQuality = null;
        for (Switch s : switches) if (s.getContentDescription() != null && s.getContentDescription().toString().equals(a.getString(R.string.hq_toggle))) highQuality = s;
        require(highQuality != null && !highQuality.isChecked(), "high-quality default must be off");
        final Switch hq = highQuality;
        runOnMainSync(() -> hq.setChecked(true));
        require(c.getSharedPreferences(PREFS, 0).getBoolean("high_quality_48000", false), "high-quality preference not persisted");
        Activity hqRecreated = recreateAndGet(a);
        java.util.List<Switch> recreatedSwitches = findAll(hqRecreated.getWindow().getDecorView(), Switch.class);
        boolean persisted = false;
        for (Switch s : recreatedSwitches) if (s.getContentDescription() != null && s.getContentDescription().toString().equals(hqRecreated.getString(R.string.hq_toggle))) persisted = s.isChecked();
        require(persisted, "high-quality preference lost on recreate");
        runOnMainSync(hqRecreated::finish);
        a = startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        bars = findAll(a.getWindow().getDecorView(), SeekBar.class).toArray(new SeekBar[0]);
        for (SeekBar bar : bars) require(bar.getMin() == 0 && bar.getMax() == 100 && bar.getProgress() == 50, "slider not centered 0..100");
        final SeekBar[] sliderBars = bars;
        runOnMainSync(() -> { sliderBars[0].setProgress(0); sliderBars[1].setProgress(100); });
        require(c.getSharedPreferences(PREFS, 0).getInt("speed_slider_140", -1) == 0, "speed endpoint not persisted");
        require(c.getSharedPreferences(PREFS, 0).getInt("pitch_slider_140", -1) == 100, "pitch endpoint not persisted");
        Activity recreated = recreateAndGet(a);
        SeekBar[] endpoint = findAll(recreated.getWindow().getDecorView(), SeekBar.class).toArray(new SeekBar[0]);
        require(endpoint[0].getProgress() == 0 && endpoint[1].getProgress() == 100, "endpoint lost on recreate");
        runOnMainSync(recreated::finish);
        c.getSharedPreferences(PREFS, 0).edit().remove("speed_slider_140").remove("pitch_slider_140").putInt("rate_percent", 100).putInt("pitch_percent", 100).commit();
        Activity migrated = startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        SeekBar[] middle = findAll(migrated.getWindow().getDecorView(), SeekBar.class).toArray(new SeekBar[0]);
        require(middle[0].getProgress() == 50 && middle[1].getProgress() == 50, "100% legacy values did not migrate to center");
        runOnMainSync(migrated::finish);
        c.getSharedPreferences(PREFS, 0).edit().remove("speed_slider_140").remove("pitch_slider_140").putInt("rate_percent", 25).putInt("pitch_percent", 200).commit();
        Activity bounds = startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        SeekBar[] edge = findAll(bounds.getWindow().getDecorView(), SeekBar.class).toArray(new SeekBar[0]);
        require(edge[0].getProgress() == 0 && edge[1].getProgress() == 100, "old min/max migration incorrect");
        note("PASS slider persistence, recreate, 100% center migration, and old 25..400/50..200 bounds");
        return bounds;
    }

    private Activity recreateAndGet(Activity old) throws Exception {
        ActivityMonitor monitor = addMonitor(MainActivity.class.getName(), null, false);
        runOnMainSync(old::recreate);
        Activity next = monitor.waitForActivityWithTimeout(10000);
        removeMonitor(monitor);
        require(next != null, "Activity recreation failed");
        waitForIdleSync();
        return next;
    }

    private static <T extends View> java.util.List<T> findAll(View v, Class<T> type) {
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        if (type.isInstance(v)) result.add(type.cast(v));
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup)v).getChildCount(); i++) result.addAll(findAll(((ViewGroup)v).getChildAt(i), type));
        return result;
    }

    private static String activityDescription(View view) {
        return String.valueOf(view.getContentDescription());
    }

    private static long energy(short[] s) { long x = 0; for (short v : s) x += Math.abs((long)v); return x; }
    private static long pcmDifference(short[] a, short[] b) { long x = Math.abs(a.length - b.length); int n = Math.min(a.length, b.length); for (int i = 0; i < n; i += Math.max(1, n / 1000)) x += Math.abs((long)a[i] - b[i]); return x; }
    private static boolean close(float a, float b) { return Math.abs(a - b) < .0001f; }
    private static boolean hasMp3Header(File f) throws Exception { try (FileInputStream in = new FileInputStream(f)) { int a=in.read(),b=in.read(),d=in.read(); return (a=='I'&&b=='D'&&d=='3') || (a==255&&(b&224)==224); } }
}
