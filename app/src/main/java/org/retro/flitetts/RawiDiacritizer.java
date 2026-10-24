/*
 * Local Rawi v3 Arabic diacritization. The ONNX model and vocabulary are
 * Apache-2.0; see assets/rawi/MODEL-NOTICE.txt. This adapter is part of Old TTS.
 */
package org.retro.flitetts;

import android.content.Context;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Optional, offline Arabic-only Rawi v3 diacritization service.
 * Call {@link #diacritize(Context, String)} from a worker thread. */
public final class RawiDiacritizer {
    private static final String MODEL = "rawi/rawi_v3.int8.onnx";
    private static final String VOCAB = "rawi/rawi_v3.vocab.json";
    private static final int MAX_CHARS = 300;
    private static volatile Runtime runtime;
    private RawiDiacritizer() {}

    /** Runs real Rawi v3 inference; throws if assets/ONNX Runtime are unavailable. */
    public static String diacritize(Context context, String text) throws Exception {
        if (text == null || text.isEmpty()) return text;
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Rawi cancelled");
        Runtime r = getRuntime(context.getApplicationContext());
        StringBuilder out = new StringBuilder(text.length() + text.length() / 4);
        int pos = 0;
        while (pos < text.length()) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Rawi cancelled");
            int end = Math.min(text.length(), pos + MAX_CHARS);
            if (end < text.length()) {
                int boundary = lastBoundary(text, pos, end);
                if (boundary > pos) end = boundary;
            }
            out.append(r.inferChunk(text.substring(pos, end)));
            pos = end;
        }
        return out.toString();
    }

    private static int lastBoundary(String s, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            if (Character.isWhitespace(s.charAt(i)) || isBoundary(s.charAt(i))) return i + 1;
        }
        return end;
    }
    private static boolean isBoundary(char c) { return c == '.' || c == ',' || c == '!' || c == '?' || c == '؛' || c == '،' || c == '؟' || c == '\n'; }

    private static Runtime getRuntime(Context c) throws Exception {
        Runtime r = runtime;
        if (r != null) return r;
        synchronized (RawiDiacritizer.class) {
            r = runtime;
            if (r == null) runtime = r = new Runtime(c);
            return r;
        }
    }

    private static final class Runtime {
        final OrtEnvironment env;
        final OrtSession session;
        final Map<String,Integer> chars = new HashMap<>();
        final String[] diacritics;
        final int unk;
        Runtime(Context c) throws Exception {
            env = OrtEnvironment.getEnvironment();
            env.setTelemetry(false);
            JSONObject vocab = new JSONObject(readAsset(c, VOCAB));
            JSONObject cm = vocab.getJSONObject("char_to_idx");
            for (Iterator<String> it = cm.keys(); it.hasNext();) { String k = it.next(); chars.put(k, cm.getInt(k)); }
            unk = chars.containsKey("<UNK>") ? chars.get("<UNK>") : 1;
            JSONObject dm = vocab.getJSONObject("diac_to_idx");
            diacritics = new String[dm.length()];
            for (Iterator<String> it = dm.keys(); it.hasNext();) { String k = it.next(); diacritics[dm.getInt(k)] = k; }
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                session = env.createSession(readAssetBytes(c, MODEL), options);
            }
        }
        String inferChunk(String original) throws Exception {
            // Run once over the complete chunk so the model sees sentence context.
            int sourceCount = original.codePointCount(0, original.length());
            int[] modelAtSource = new int[sourceCount]; Arrays.fill(modelAtSource, -1);
            boolean[] marked = new boolean[original.length()]; markSuppliedWords(original, marked);
            StringBuilder bare = new StringBuilder(original.length());
            int sourceNo = 0;
            for (int p = 0; p < original.length();) {
                int cp = original.codePointAt(p);
                String nfd = Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFD);
                for (int q = 0; q < nfd.length();) {
                    int b = nfd.codePointAt(q); q += Character.charCount(b); int type = Character.getType(b);
                    if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.OTHER_SYMBOL || type == Character.SURROGATE) continue;
                    if (modelAtSource[sourceNo] < 0) modelAtSource[sourceNo] = bare.codePointCount(0, bare.length());
                    bare.appendCodePoint(b);
                }
                p += Character.charCount(cp); sourceNo++;
            }
            if (bare.length() == 0) return original;
            int nChars = bare.codePointCount(0, bare.length()); long[][] ids = new long[1][nChars]; int n = 0;
            for (int p = 0; p < bare.length();) { int cp = bare.codePointAt(p); p += Character.charCount(cp); Integer id = chars.get(new String(Character.toChars(cp))); ids[0][n++] = id == null ? unk : id; }
            try (OnnxTensor input = OnnxTensor.createTensor(env, ids); OrtSession.Result rr = session.run(mapOf("input", input))) {
                float[][] presence = (float[][]) rr.get("presence").get().getValue(); float[][][] values = (float[][][]) rr.get("value").get().getValue();
                StringBuilder out = new StringBuilder(original.length() + 32); sourceNo = 0;
                for (int p = 0; p < original.length();) {
                    int cp = original.codePointAt(p); out.appendCodePoint(cp); int modelNo = modelAtSource[sourceNo];
                    if (modelNo >= 0 && isArabicLetter(cp) && !marked[p] && presence[0][modelNo] > 0.0f) {
                        int best = 0; for (int k = 1; k < values[0][modelNo].length; k++) if (values[0][modelNo][k] > values[0][modelNo][best]) best = k;
                        if (best < diacritics.length && diacritics[best] != null && !(hasHamzaBearing(cp) && containsHamzaMark(diacritics[best]))) out.append(diacritics[best]);
                    }
                    p += Character.charCount(cp); sourceNo++;
                }
                return out.toString();
            }
        }
        private void markSuppliedWords(String s, boolean[] marked) {
            for (int p = 0; p < s.length();) { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; int end = p; while (end < s.length() && !Character.isWhitespace(s.charAt(end))) end++; if (end > p && hasAnyMark(s.substring(p, end))) for (int i = p; i < end; i++) marked[i] = true; p = end; }
        }
        private Map<String,OnnxTensor> mapOf(String k, OnnxTensor v) { Map<String,OnnxTensor> m = new HashMap<>(); m.put(k, v); return m; }
    }

    private static boolean containsArabic(String s) { for (int i=0;i<s.length();) { int cp=s.codePointAt(i); i+=Character.charCount(cp); if (isArabicLetter(cp)) return true; } return false; }
    private static boolean hasAnyMark(String s) { for (int i=0;i<s.length();) { int cp=s.codePointAt(i); i+=Character.charCount(cp); int t=Character.getType(cp); if (t==Character.NON_SPACING_MARK || t==Character.COMBINING_SPACING_MARK || t==Character.ENCLOSING_MARK) return true; } return false; }
    private static boolean containsHamzaMark(String s) { return s.indexOf('\u0654') >= 0 || s.indexOf('\u0655') >= 0; }
    private static boolean hasHamzaBearing(int cp) { return cp == 0x0621 || cp == 0x0622 || cp == 0x0623 || cp == 0x0624 || cp == 0x0625 || cp == 0x0626 || cp == 0x0671; }
    private static boolean isArabicLetter(int cp) { return (cp >= 0x0600 && cp <= 0x06FF || cp >= 0x0750 && cp <= 0x077F || cp >= 0x08A0 && cp <= 0x08FF) && Character.isLetter(cp); }
    private static String readAsset(Context c, String p) throws Exception { return new String(readAssetBytes(c,p), "UTF-8"); }
    private static byte[] readAssetBytes(Context c, String p) throws Exception { try (InputStream in=c.getAssets().open(p); ByteArrayOutputStream out=new ByteArrayOutputStream()) { byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1) out.write(b,0,n); return out.toByteArray(); } }
}
