package org.retro.flitetts;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Writes mono integer PCM to a standard RIFF/WAVE file. */
public final class WavEncoder {
    private static final int[] SUPPORTED_BITS = {8, 16, 24, 32};
    private static final byte[] PCM_SUBFORMAT_GUID = {
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00,
            (byte) 0x80, 0x00, 0x00, (byte) 0xaa, 0x00, 0x38, (byte) 0x9b, 0x71
    };

    private WavEncoder() {}

    public static int[] supportedBitDepths() {
        return SUPPORTED_BITS.clone();
    }

    public static void encode(short[] pcm, int sampleRate, int containerBits, File destination)
            throws IOException {
        if (pcm == null) throw new NullPointerException("pcm");
        if (destination == null) throw new NullPointerException("destination");
        if (sampleRate <= 0) throw new IllegalArgumentException("sampleRate");
        if (!isSupported(containerBits)) throw new IllegalArgumentException("containerBits");
        if (pcm.length == 0) throw new IllegalArgumentException("empty PCM");

        final int bytesPerSample = containerBits / 8;
        final int blockAlign = bytesPerSample;
        final long dataBytes = (long) pcm.length * bytesPerSample;
        final boolean extensible = containerBits > 16;
        final int fmtBytes = extensible ? 40 : 16;
        final long riffBytes = dataBytes + 4L + 8L + fmtBytes + 8L;
        final long byteRate = (long) sampleRate * blockAlign;
        if (dataBytes > 0xffffffffL || riffBytes > 0xffffffffL || byteRate > 0xffffffffL) {
            throw new IOException("WAV is too large");
        }

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            ascii(out, "RIFF");
            u32(out, riffBytes);
            ascii(out, "WAVE");
            ascii(out, "fmt ");
            u32(out, fmtBytes);
            u16(out, extensible ? 0xfffe : 1);
            u16(out, 1); // mono
            u32(out, sampleRate);
            u32(out, byteRate);
            u16(out, blockAlign);
            u16(out, containerBits);
            if (extensible) {
                u16(out, 22); // WAVEFORMATEXTENSIBLE extension size
                u16(out, containerBits); // The output container width is the advertised PCM sample width.
                u32(out, 0x4); // SPEAKER_FRONT_CENTER for mono.
                out.write(PCM_SUBFORMAT_GUID);
            }
            ascii(out, "data");
            u32(out, dataBytes);

            for (int i = 0; i < pcm.length; i++) {
                if ((i & 8191) == 0 && Thread.currentThread().isInterrupted()) {
                    throw new IOException("WAV encoding interrupted");
                }
                int sample = pcm[i];
                switch (containerBits) {
                    case 8:
                        // RIFF/WAVE 8-bit PCM is unsigned; 16-bit source maps linearly to 0..255.
                        out.write((sample >> 8) + 128);
                        break;
                    case 16:
                        out.write(sample & 0xff);
                        out.write((sample >>> 8) & 0xff);
                        break;
                    case 24:
                        int pcm24 = sample << 8;
                        out.write(pcm24 & 0xff);
                        out.write((pcm24 >>> 8) & 0xff);
                        out.write((pcm24 >>> 16) & 0xff);
                        break;
                    case 32:
                        int pcm32 = sample << 16;
                        out.write(pcm32 & 0xff);
                        out.write((pcm32 >>> 8) & 0xff);
                        out.write((pcm32 >>> 16) & 0xff);
                        out.write((pcm32 >>> 24) & 0xff);
                        break;
                    default:
                        throw new AssertionError("Unsupported PCM width");
                }
            }
        }
    }

    private static boolean isSupported(int bits) {
        for (int supported : SUPPORTED_BITS) if (supported == bits) return true;
        return false;
    }

    private static void ascii(OutputStream out, String value) throws IOException {
        for (int i = 0; i < value.length(); i++) out.write((byte) value.charAt(i));
    }

    private static void u16(OutputStream out, int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >>> 8) & 0xff);
    }

    private static void u32(OutputStream out, long value) throws IOException {
        out.write((int) (value & 0xff));
        out.write((int) ((value >>> 8) & 0xff));
        out.write((int) ((value >>> 16) & 0xff));
        out.write((int) ((value >>> 24) & 0xff));
    }
}
