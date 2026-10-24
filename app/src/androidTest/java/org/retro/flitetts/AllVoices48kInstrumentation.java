package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Deterministic 1.5.0 regression coverage for HQ 48 kHz across all 63 voices. */
public final class AllVoices48kInstrumentation extends Instrumentation {
    private static final String TAG = "OldTTS-AllVoices48k";
    private static final float RATE = 1.25f;
    private static final float PITCH = .80f;

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private void note(String message) {
        Bundle b = new Bundle(); b.putString("stream", message + "\n"); sendStatus(1, b);
        android.util.Log.i(TAG, message);
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Activity activity = null;
        try {
            Context c = getTargetContext();
            File root = new File(c.getFilesDir(), "all-voices-48k");
            require(root.exists() || root.mkdirs(), "cannot create " + root);
            verifyPassthrough();
            List<String> records = verifyAllVoices(c, root);
            activity = verifyUi(c);
            writeJson(new File(root, "all-voices-48k.json"), records);
            note("PASS all-voices 48k instrumentation; JSON=" + new File(root, "all-voices-48k.json"));
            Bundle result = new Bundle();
            result.putString("stream", "PASS all 63 voices native off/on, deterministic PCM, HQ WAV/MP3, UI persistence; JSON=" + new File(root, "all-voices-48k.json") + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            note("FAIL " + failure);
            Bundle result = new Bundle(); result.putString("error", failure.toString());
            finish(Activity.RESULT_CANCELED, result);
        } finally { if (activity != null) runOnMainSync(activity::finish); }
    }

    private List<String> verifyAllVoices(Context c, File root) throws Exception {
        require(EngineRegistry.all().size() == 11, "expected 11 engines");
        int totalVoices = 0; for (EnginePlugin e : EngineRegistry.all()) totalVoices += e.voiceIds().length;
        require(totalVoices == 63, "expected 63 voices, got " + totalVoices);
        File wav = new File(root, "wav"); File mp3 = new File(root, "mp3");
        require(wav.mkdirs() || wav.isDirectory(), "cannot create WAV directory");
        require(mp3.mkdirs() || mp3.isDirectory(), "cannot create MP3 directory");
        List<String> records = new ArrayList<>(); int count = 0;
        for (EnginePlugin engine : EngineRegistry.all()) for (int voice = 0; voice < engine.voiceIds().length; voice++) {
            String id = engine.id() + "-" + engine.voiceIds()[voice];
            String text = engine.sampleText(voice); require(text != null && !text.trim().isEmpty(), "empty fixture " + id);
            RecordingPlugin recording = new RecordingPlugin(engine);
            StandaloneAudio.Pcm off = StandaloneAudio.render(c, recording, voice, text, RATE, PITCH, false, false, () -> false);
            short[] captured = recording.joined();
            require(Arrays.equals(off.samples, captured), "native off changed captured PCM " + id);
            require(off.sampleRate == engine.sampleRate(voice), "native rate mismatch " + id);
            require(off.samples.length > 0 && energy(off.samples) > 1000, "empty/silent native PCM " + id);
            require(recording.rateOk && recording.pitchOk, "engine parameters changed in native mode " + id);
            writeWav(new File(wav, id + "-native.wav"), off.samples, off.sampleRate);

            FixedPlugin fixed = new FixedPlugin(engine, recording.parts, recording.sampleRate);
            StandaloneAudio.Pcm deterministicHq = StandaloneAudio.render(c, fixed, voice, text, RATE, PITCH, false, true, () -> false);
            short[] expected = AudioResampler.to48k(off.samples, off.sampleRate, () -> false);
            require(deterministicHq.sampleRate == 48000, "HQ rate is not 48kHz " + id);
            require(Arrays.equals(deterministicHq.samples, expected), "HQ PCM differs from same-input conversion " + id);
            require(deterministicHq.samples.length == expected.length, "HQ length mismatch " + id);
            require(deterministicHq.samples.length > 0 && energy(deterministicHq.samples) > 1000, "empty/silent HQ PCM " + id);
            require(recording.rateOk && recording.pitchOk, "engine parameters changed when HQ enabled " + id);
            require(Math.abs(deterministicHq.samples.length / 48000.0 - off.samples.length / (double)off.sampleRate) <= 0.5 / 48000.0 + 1e-12, "HQ duration changed " + id);
            writeWav(new File(wav, id + "-48k.wav"), deterministicHq.samples, 48000);

            // Independent real HQ render exercises the production integration and MP3 path.
            RecordingPlugin realRecording = new RecordingPlugin(engine);
            StandaloneAudio.Pcm realHq = StandaloneAudio.render(c, realRecording, voice, text, RATE, PITCH, false, true, () -> false);
            require(realRecording.rateOk && realRecording.pitchOk, "real HQ engine parameters changed " + id);
            File nativeMp3 = new File(mp3, id + "-native.mp3"); Mp3Encoder.encode(off.samples, off.sampleRate, nativeMp3);
            // MPEG Layer III cannot encode 10000 Hz; LAME resamples Klatt to 11025 Hz.
            requireMp3Rate(nativeMp3, off.sampleRate == 10000 ? 11025 : off.sampleRate);
            require(realHq.sampleRate == 48000 && realHq.samples.length > 0 && energy(realHq.samples) > 1000, "real HQ output invalid " + id);
            File out = new File(mp3, id + "-48k.mp3"); Mp3Encoder.encode(realHq.samples, 48000, out);
            require(out.isFile() && out.length() > 128 && hasMp3Header(out), "invalid 48k MP3 " + id);
            requireMp3Rate(out, 48000);
            records.add(jsonRecord(engine, voice, off, deterministicHq, realHq, out, recording)); count++;
            note("PASS " + id + " native=" + off.sampleRate + "Hz/" + off.samples.length + " HQ=48000Hz/" + realHq.samples.length + " MP3=" + out.length());
        }
        require(count == 63, "rendered " + count + " voices");
        return records;
    }

    private void verifyPassthrough() {
        short[] input = new short[] {0, 1, -2, 300, -400, 32767, -32768};
        require(Arrays.equals(input, AudioResampler.to48k(input, 48000, () -> false)), "48k passthrough is not bit exact");
        note("PASS 48k passthrough bit exact");
    }

    private Activity verifyUi(Context c) throws Exception {
        c.getSharedPreferences("retro", 0).edit().putBoolean("storage_intro_131", true).putBoolean("high_quality_48000", false).commit();
        Activity a = startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); waitForIdleSync();
        List<Switch> switches = findAll(a.getWindow().getDecorView(), Switch.class);
        require(switches.size() == 2, "expected Rawi and HQ switches");
        Switch hq = null; for (Switch s : switches) if (String.valueOf(s.getContentDescription()).equals(a.getString(R.string.hq_toggle))) hq = s;
        require(hq != null && !hq.isChecked() && hq.getVisibility() == View.VISIBLE, "HQ switch missing/hidden");
        final Switch hqFinal = hq; runOnMainSync(() -> hqFinal.setChecked(true));
        require(c.getSharedPreferences("retro", 0).getBoolean("high_quality_48000", false), "HQ preference not persisted");
        require(countButtons(a.getWindow().getDecorView()) >= 6, "buttons missing before engine switch");
        Spinner engineSpinner = null, voiceSpinner = null;
        for (Spinner sp : findAll(a.getWindow().getDecorView(), Spinner.class)) {
            if (String.valueOf(sp.getContentDescription()).equals(a.getString(R.string.engine))) engineSpinner = sp;
            if (String.valueOf(sp.getContentDescription()).equals(a.getString(R.string.voice))) voiceSpinner = sp;
        }
        require(engineSpinner != null && voiceSpinner != null, "missing engine/voice controls");
        final Spinner es = engineSpinner, vs = voiceSpinner;
        for (int e = 0; e < es.getAdapter().getCount(); e++) {
            final int ei = e; runOnMainSync(() -> es.setSelection(ei)); waitForIdleSync();
            for (int v = 0; v < vs.getAdapter().getCount(); v++) {
                final int vi = v; runOnMainSync(() -> vs.setSelection(vi)); waitForIdleSync();
                require(hqFinal.getVisibility() == View.VISIBLE && hqFinal.isChecked(), "HQ lost on engine/voice switch");
                require(countButtons(a.getWindow().getDecorView()) >= 6, "buttons hidden after engine/voice switch");
            }
        }
        runOnMainSync(a::finish); waitForIdleSync();
        Activity restarted = startActivitySync(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); waitForIdleSync();
        boolean persisted = false;
        for (Switch sw : findAll(restarted.getWindow().getDecorView(), Switch.class)) {
            if (String.valueOf(sw.getContentDescription()).equals(restarted.getString(R.string.hq_toggle))) {
                persisted = sw.isChecked(); final Switch toggle = sw; runOnMainSync(() -> toggle.setChecked(false));
            }
        }
        require(persisted, "HQ preference lost on restart");
        require(!c.getSharedPreferences("retro", 0).getBoolean("high_quality_48000", true), "HQ off not persisted");
        note("PASS UI persistence on restart and HQ visible across all 30 engine/voice choices"); return restarted;
    }

    private static String jsonRecord(EnginePlugin e, int voice, StandaloneAudio.Pcm off, StandaloneAudio.Pcm converted, StandaloneAudio.Pcm realHq, File mp3, RecordingPlugin r) {
        return "{\"engine\":\"" + esc(e.id()) + "\",\"voice\":\"" + esc(e.voiceIds()[voice]) + "\",\"nativeRate\":" + off.sampleRate + ",\"nativeSamples\":" + off.samples.length + ",\"convertedRate\":" + converted.sampleRate + ",\"convertedSamples\":" + converted.samples.length + ",\"mp3RenderSamples\":" + realHq.samples.length + ",\"mp3Bytes\":" + mp3.length() + ",\"nativeEnergy\":" + energy(off.samples) + ",\"convertedEnergy\":" + energy(converted.samples) + ",\"rate\":" + RATE + ",\"pitch\":" + PITCH + ",\"parameterCapture\":true,\"f0Measured\":false}";
    }
    private static void writeJson(File file, List<String> records) throws IOException { try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) { w.write("{\"format\":\"Old-TTS 1.5.0 all voices 48k\",\"voiceCount\":"); w.write(Integer.toString(records.size())); w.write(",\"records\":[\n"); for (int i=0;i<records.size();i++) { if (i>0) w.write(",\n"); w.write(records.get(i)); } w.write("\n]}\n"); } }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static long energy(short[] x) { long n=0; for (short v:x) n += Math.abs((long)v); return n; }
    private static void requireMp3Rate(File file, int expected) throws IOException {
        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        try {
            extractor.setDataSource(file.getAbsolutePath());
            require(extractor.getTrackCount() > 0, "MP3 has no audio track " + file);
            int actual = extractor.getTrackFormat(0).getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE);
            require(actual == expected, "MP3 sample rate " + actual + " expected " + expected + " " + file);
        } finally { extractor.release(); }
    }
    private static boolean hasMp3Header(File f) throws IOException { try (FileInputStream in=new FileInputStream(f)) { int a=in.read(),b=in.read(),c=in.read(); return (a=='I'&&b=='D'&&c=='3') || (a==255&&(b&224)==224); } }
    private static void writeWav(File f, short[] pcm, int rate) throws IOException { try (FileOutputStream o=new FileOutputStream(f)) { int data=pcm.length*2; ByteBuffer h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN); h.put(new byte[]{'R','I','F','F'}).putInt(36+data).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(rate).putInt(rate*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(data); o.write(h.array()); ByteBuffer b=ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN); for (short s:pcm) { if (b.remaining()<2) {o.write(b.array(),0,b.position()); b.clear();} b.putShort(s); } if (b.position()>0)o.write(b.array(),0,b.position()); } }
    private static int countButtons(View v) { int n=(v instanceof Button && !(v instanceof Switch))?1:0; if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=countButtons(((ViewGroup)v).getChildAt(i)); return n; }
    private static <T extends View> List<T> findAll(View v, Class<T> type) { List<T> r=new ArrayList<>(); if(type.isInstance(v))r.add(type.cast(v)); if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)r.addAll(findAll(((ViewGroup)v).getChildAt(i),type)); return r; }

    private static final class RecordingPlugin implements EnginePlugin {
        final EnginePlugin delegate; final List<short[]> parts=new ArrayList<>(); int sampleRate; boolean rateOk=true,pitchOk=true;
        RecordingPlugin(EnginePlugin d){delegate=d;}
        public String id(){return delegate.id();} public String label(){return delegate.label();} public Locale locale(){return delegate.locale();} public String[] voiceIds(){return delegate.voiceIds();} public String[] voiceLabels(){return delegate.voiceLabels();} public String sampleText(){return delegate.sampleText();} public String sampleText(int v){return delegate.sampleText(v);} public void initialize(Context c){delegate.initialize(c);} public String languageNotice(){return delegate.languageNotice();} public String[] noticeAssets(){return delegate.noticeAssets();} public Locale voiceLocale(int v){return delegate.voiceLocale(v);} public int languageAvailability(String l,String c,String v){return delegate.languageAvailability(l,c,v);} public boolean acceptsText(String t){return delegate.acceptsText(t);} public boolean acceptsText(String t,int v){return delegate.acceptsText(t,v);} public int sampleRate(int v){sampleRate=delegate.sampleRate(v);return sampleRate;} public short[] synthesize(String t,int v,float rate,float pitch){rateOk &= Math.abs(rate-RATE)<.0001f; pitchOk &= Math.abs(pitch-PITCH)<.0001f; short[] p=delegate.synthesize(t,v,rate,pitch); require(p!=null&&p.length>0,"engine returned empty PCM "+id()); parts.add(p.clone()); return p;}
        short[] joined(){int n=0;for(short[] p:parts)n+=p.length;short[] r=new short[n];int at=0;for(short[] p:parts){System.arraycopy(p,0,r,at,p.length);at+=p.length;}return r;}
    }
    private static final class FixedPlugin implements EnginePlugin {
        final EnginePlugin d; final List<short[]> parts; final int rate; int index; FixedPlugin(EnginePlugin d,List<short[]> p,int r){this.d=d;parts=p;rate=r;}
        public String id(){return d.id();} public String label(){return d.label();} public Locale locale(){return d.locale();} public String[] voiceIds(){return d.voiceIds();} public String[] voiceLabels(){return d.voiceLabels();} public String sampleText(){return d.sampleText();} public String sampleText(int v){return d.sampleText(v);} public String languageNotice(){return d.languageNotice();} public String[] noticeAssets(){return d.noticeAssets();} public Locale voiceLocale(int v){return d.voiceLocale(v);} public int languageAvailability(String l,String c,String v){return d.languageAvailability(l,c,v);} public boolean acceptsText(String t){return d.acceptsText(t);} public boolean acceptsText(String t,int v){return d.acceptsText(t,v);} public int sampleRate(int v){return rate;} public short[] synthesize(String t,int v,float r,float p){require(Math.abs(r-RATE)<.0001f&&Math.abs(p-PITCH)<.0001f,"HQ changed engine parameters"); require(index<parts.size(),"HQ chunk count changed"); return parts.get(index++).clone();}
    }
}
