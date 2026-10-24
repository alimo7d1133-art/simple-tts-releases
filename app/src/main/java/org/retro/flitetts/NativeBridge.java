package org.retro.flitetts;
final class NativeBridge {
 static { System.loadLibrary("fliteretro"); }
 static synchronized native short[] render(String text,int voice,float rate,float pitch);
 static synchronized native int sampleRate(int voice);
 static synchronized native boolean loadExtraVoices(String directory);
}
