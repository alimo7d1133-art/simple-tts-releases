package org.retro.flitetts;

import java.io.File;
import java.io.IOException;

/** Offline MP3 encoder for mono 16-bit PCM, backed by the vendored LGPL LAME library. */
public final class Mp3Encoder {
    static { System.loadLibrary("mp3encoder"); }
    private Mp3Encoder() {}

    /** Encodes all samples as a mono, 64 kbps MP3. The destination is replaced atomically. */
    public static void encode(short[] pcm, int sampleRate, File destination) throws IOException {
        if (pcm == null) throw new NullPointerException("pcm");
        if (destination == null) throw new NullPointerException("destination");
        if (sampleRate <= 0) throw new IllegalArgumentException("sampleRate");
        if (Thread.currentThread().isInterrupted()) throw new IOException("MP3 encoding interrupted");
        int result = encodeNative(pcm, sampleRate, destination.getAbsolutePath());
        if (result == -2) throw new IOException("MP3 encoding interrupted");
        if (result != 0) throw new IOException("MP3 encoding failed (code " + result + ")");
    }

    private static native int encodeNative(short[] pcm, int sampleRate, String destination);
}
