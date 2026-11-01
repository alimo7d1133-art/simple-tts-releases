/* Simple TTS experimental tone-sequence engine. It generates tones, not speech. */
package org.retro.flitetts;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * Generates a sequence of sine tones from frequency values in Hz or kHz.
 * Example input: "440, 660, 1.2kHz". No text is spoken or interpreted.
 */
public final class KilohertzPlugin implements EnginePlugin {
    private static final int SAMPLE_RATE = 48000;
    private static final int MIN_FREQUENCY_HZ = 40;
    private static final int MAX_FREQUENCY_HZ = 5000;
    private static final int MAX_TONES = 128;
    private static final double TONE_SECONDS = 0.22;
    private static final double GAP_SECONDS = 0.045;
    private static final double AMPLITUDE = 0.18;
    private static final String[] IDS = {"sine-sequence"};
    private static final String[] LABELS = {"Sine tone sequence (not speech)"};

    @Override public String id() { return "kilohertz"; }
    @Override public String label() { return "Kilohertz — experimental tones"; }
    @Override public Locale locale() { return Locale.US; }
    @Override public String[] voiceIds() { return IDS.clone(); }
    @Override public String[] voiceLabels() { return LABELS.clone(); }
    @Override public String sampleText() { return "440, 660, 880, 1.2kHz"; }
    @Override public String[] noticeAssets() { return new String[0]; }
    @Override public String languageNotice() {
        return "Experimental sine-tone generator only; it does not speak or produce intelligible speech.";
    }
    @Override public int languageAvailability(String language, String country, String variant) {
        return TextToSpeech.LANG_AVAILABLE;
    }
    @Override public boolean acceptsText(String text) { return parseFrequencies(text) != null; }
    @Override public boolean acceptsText(String text, int voice) {
        return voice == 0 && acceptsText(text);
    }
    @Override public int sampleRate(int voice) { return SAMPLE_RATE; }
    @Override public boolean synthesizeWholeText() { return true; }

    @Override public short[] synthesize(String text, int voice, float rate, float pitch) {
        if (voice != 0) throw new IllegalArgumentException("Unknown Kilohertz voice");
        double[] frequencies = parseFrequencies(text);
        if (frequencies == null) throw new IllegalArgumentException("Enter 1–128 valid frequencies in Hz or kHz");

        double speed = clamp(rate, 0.25, 4.0);
        double pitchScale = clamp(pitch, 0.5, 2.0);
        int toneSamples = Math.max(1, (int) Math.round(SAMPLE_RATE * TONE_SECONDS / speed));
        int gapSamples = (int) Math.round(SAMPLE_RATE * GAP_SECONDS / speed);
        int totalSamples = frequencies.length * toneSamples + Math.max(0, frequencies.length - 1) * gapSamples;
        short[] output = new short[totalSamples];
        int fadeSamples = Math.max(1, Math.min(toneSamples / 2, SAMPLE_RATE / 125));
        double peak = AMPLITUDE * Short.MAX_VALUE;
        int out = 0;

        for (int n = 0; n < frequencies.length; n++) {
            double frequency = frequencies[n] * pitchScale;
            for (int i = 0; i < toneSamples; i++) {
                if ((i & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                    throw new java.util.concurrent.CancellationException();
                }
                double envelope = Math.min(1.0, Math.min((double) i / fadeSamples,
                        (double) (toneSamples - 1 - i) / fadeSamples));
                double sample = peak * envelope * Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE);
                output[out++] = (short) Math.round(sample);
            }
            if (n + 1 < frequencies.length) out += gapSamples;
        }
        return output;
    }

    /** Returns null for malformed input; bare values are Hz, and kHz is explicit. */
    static double[] parseFrequencies(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        StringBuilder normalized = new StringBuilder(text.length());
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == 0x066B) {
                normalized.append('.'); // Arabic decimal separator.
            } else if (cp == 0x066C || cp == 0x060C || cp == 0x061B) {
                normalized.append(' '); // Arabic thousands/list separators.
            } else if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)
                    || cp == ',' || cp == ';') {
                normalized.append(' ');
            } else {
                int digit = Character.digit(cp, 10);
                if (digit >= 0) normalized.append((char) ('0' + digit));
                else normalized.appendCodePoint(cp);
            }
        }

        String trimmed = normalized.toString().trim();
        if (trimmed.isEmpty()) return null;
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0 || tokens.length > MAX_TONES) return null;
        double[] values = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            double multiplier = 1.0;
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.endsWith("khz")) {
                multiplier = 1000.0;
                token = token.substring(0, token.length() - 3);
            } else if (lower.endsWith("hz")) {
                token = token.substring(0, token.length() - 2);
            }
            if (!isDecimal(token)) return null;
            double frequency;
            try { frequency = Double.parseDouble(token) * multiplier; }
            catch (NumberFormatException invalid) { return null; }
            if (!Double.isFinite(frequency) || frequency < MIN_FREQUENCY_HZ || frequency > MAX_FREQUENCY_HZ) return null;
            values[i] = frequency;
        }
        return values;
    }

    private static boolean isDecimal(String value) {
        if (value.isEmpty()) return false;
        boolean dot = false, digit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') digit = true;
            else if (c == '.' && !dot && i > 0 && i < value.length() - 1) dot = true;
            else return false;
        }
        return digit;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
