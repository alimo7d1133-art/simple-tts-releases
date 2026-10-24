package org.retro.flitetts;

import java.util.concurrent.CancellationException;

public final class HostResamplerTest {
    private static final StandaloneAudio.Cancellation NEVER = () -> false;
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static short[] sine(int rate, double hz, int n, double amplitude) {
        short[] x = new short[n];
        for (int i = 0; i < n; i++) x[i] = (short)Math.round(Math.sin(2 * Math.PI * hz * i / rate) * amplitude);
        return x;
    }
    private static double peakFrequency(short[] x, int rate, double expected) {
        // A local DFT is sufficient for a deterministic host smoke test.
        double best = 0, bestF = 0;
        for (double f = expected - 80; f <= expected + 80; f += 1) {
            double re = 0, im = 0;
            for (int i = 0; i < x.length; i++) {
                double a = 2 * Math.PI * f * i / rate;
                re += x[i] * Math.cos(a); im -= x[i] * Math.sin(a);
            }
            double p = re * re + im * im;
            if (p > best) { best = p; bestF = f; }
        }
        return bestF;
    }
    public static void main(String[] args) {
        for (int rate : new int[] {8000, 10000, 16000, 22050, 32000, 44100, 48000, 96000}) {
            for (double frequency : new double[] {110, 220, 1000}) {
                short[] input = sine(rate, frequency, rate / 4 + 7, 12000);
                short[] converted = AudioResampler.to48k(input, rate, NEVER);
                double durationError = Math.abs((double)converted.length / 48000 - (double)input.length / rate);
                require(durationError <= 0.5 / 48000 + 1e-12, "duration changed at " + rate);
                require(Math.abs(peakFrequency(converted, 48000, frequency) - frequency) <= 1,
                        "frequency changed at " + rate + ": " + frequency);
                if (rate == 48000) require(java.util.Arrays.equals(input, converted), "48k identity");
            }
            System.out.println("PASS rate=" + rate + " frequencies=110,220,1000 Hz duration<=0.5 output sample");
        }
        int sourceRate = 22050, sourceLength = sourceRate * 2;
        short[] sine = sine(sourceRate, 1000, sourceLength, 20000);
        short[] out = AudioResampler.to48k(sine, sourceRate, NEVER);
        require(out.length == 96000, "duration/output length");
        require(Math.abs(peakFrequency(out, 48000, 1000) - 1000) <= 2, "frequency changed");

        short[] dc = new short[4096];
        java.util.Arrays.fill(dc, (short)12000);
        short[] dcOut = AudioResampler.to48k(dc, 16000, NEVER);
        double mean = 0;
        for (short v : dcOut) { require(v != Short.MIN_VALUE && v != Short.MAX_VALUE, "unexpected clipping"); require(Float.isFinite(v), "non-finite"); mean += v; }
        mean /= dcOut.length;
        require(Math.abs(mean - 12000) < 3, "DC not preserved: " + mean);
        require(dcOut.length == 12288, "16k length");

        short[] native48 = new short[] { 1, -2, 3 };
        short[] nativeOut = AudioResampler.to48k(native48, 48000, NEVER);
        require(java.util.Arrays.equals(native48, nativeOut), "native 48k changed");

        final boolean[] cancelled = {false};
        try {
            AudioResampler.to48k(sine, sourceRate, () -> { cancelled[0] = true; return true; });
            throw new AssertionError("cancellation was ignored");
        } catch (CancellationException expected) { }
        require(cancelled[0], "cancellation callback not consulted");
        require(AudioResampler.resample(new short[3], 8000, 48000, NEVER, 18).length == 18, "8k length");
        System.out.println("HostResamplerTest: PASS (frequency, duration, DC, finite/bounds, native48k, cancellation, lengths)");
    }
}
