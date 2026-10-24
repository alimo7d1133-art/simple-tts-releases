/* High-quality PCM16 resampling for the standalone audio pipeline. */
package org.retro.flitetts;

import java.util.concurrent.CancellationException;

/**
 * Pure-Java mono PCM16 resampler.  The output rate is intentionally fixed at
 * 48 kHz for the high-quality mode.  A windowed-sinc low-pass kernel is
 * normalized for every output sample, so DC is preserved even at the edges.
 */
public final class AudioResampler {
    public static final int TARGET_SAMPLE_RATE = 48000;
    public static final int MAX_OUTPUT_SAMPLES = 12000000;
    private static final int HALF_TAPS = 24;
    private static final int TAP_COUNT = HALF_TAPS * 2;
    private static final double PI = Math.PI;

    private AudioResampler() {}

    public static short[] to48k(short[] input, int inputRate,
                                StandaloneAudio.Cancellation cancel) {
        return resample(input, inputRate, TARGET_SAMPLE_RATE, cancel,
                MAX_OUTPUT_SAMPLES);
    }

    /** Resamples with a normalized Blackman-windowed sinc kernel. */
    public static short[] resample(short[] input, int inputRate, int outputRate,
                                   StandaloneAudio.Cancellation cancel,
                                   int maxOutputSamples) {
        if (input == null) throw new NullPointerException("input");
        if (inputRate <= 0 || outputRate <= 0) throw new IllegalArgumentException("sample rate");
        if (maxOutputSamples < 0) throw new IllegalArgumentException("maxOutputSamples");
        if (inputRate == outputRate) {
            if (input.length > maxOutputSamples) throw new IllegalArgumentException("output too long");
            short[] copy = input.clone();
            check(cancel);
            return copy;
        }
        long length = ((long) input.length * outputRate + inputRate / 2L) / inputRate;
        if (length > maxOutputSamples || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("output too long");
        }
        int outLength = (int) length;
        short[] output = new short[outLength];
        // For downsampling, reduce the cutoff before sampling to avoid aliasing.
        final double cutoff = Math.min(1.0, (double) outputRate / inputRate) * 0.96;
        // There are only outputRate/gcd(inputRate, outputRate) distinct fractional
        // positions.  Building those kernels once removes all trig from the hot
        // loop (6 phases for 8k->48k, 3 for 16k->48k, 320 for 22050->48k).
        final int gcd = gcd(inputRate, outputRate);
        final int phaseCount = outputRate / gcd;
        final double[] kernels = new double[phaseCount * TAP_COUNT];
        for (int phase = 0; phase < phaseCount; phase++) {
            double fraction = (double) phase / phaseCount;
            int offset = phase * TAP_COUNT;
            for (int tap = 0; tap < TAP_COUNT; tap++) {
                double distance = fraction - (tap - HALF_TAPS + 1);
                double x = distance * cutoff;
                double sinc = Math.abs(x) < 1.0e-12 ? 1.0 : Math.sin(PI * x) / (PI * x);
                double windowPosition = distance / HALF_TAPS;
                // Blackman window; zero outside the finite support.
                double window = Math.abs(windowPosition) >= 1.0 ? 0.0
                        : 0.42 + 0.5 * Math.cos(PI * windowPosition)
                        + 0.08 * Math.cos(2.0 * PI * windowPosition);
                kernels[offset + tap] = cutoff * sinc * window;
            }
        }

        // Advance source position using integer quotient/remainder arithmetic.
        // This avoids cumulative floating-point drift in both phase selection and
        // the input index, while retaining the existing rounded output length.
        final int sourceStep = inputRate / outputRate;
        final int remainderStep = inputRate % outputRate;
        int source = 0;
        long remainder = 0;
        for (int out = 0; out < outLength; out++) {
            if ((out & 255) == 0) check(cancel);
            int center = source;
            int phase = (int) (remainder / gcd);
            int kernelOffset = phase * TAP_COUNT;
            double sum = 0.0;
            double weightSum = 0.0;
            int firstTap = center - HALF_TAPS + 1;
            for (int tapIndex = 0; tapIndex < TAP_COUNT; tapIndex++) {
                int tap = firstTap + tapIndex;
                double weight = kernels[kernelOffset + tapIndex];
                if (tap >= 0 && tap < input.length) {
                    sum += input[tap] * weight;
                    weightSum += weight;
                }
            }
            // Renormalizing the retained edge taps preserves a constant signal.
            double value = weightSum == 0.0 ? 0.0 : sum / weightSum;
            if (value > 32767.0) value = 32767.0;
            else if (value < -32768.0) value = -32768.0;
            output[out] = (short) Math.round(value);

            source += sourceStep;
            remainder += remainderStep;
            if (remainder >= outputRate) {
                remainder -= outputRate;
                source++;
            }
        }
        check(cancel);
        return output;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    private static void check(StandaloneAudio.Cancellation cancel) {
        if ((cancel != null && cancel.cancelled()) || Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }
}
