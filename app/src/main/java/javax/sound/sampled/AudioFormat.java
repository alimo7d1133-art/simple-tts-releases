package javax.sound.sampled;
/** Original minimal Android compatibility value type for FreeTTS's PCM path.
 * No Java Sound playback, decoding, or proprietary JDK implementation is bundled. */
public final class AudioFormat {
 private final float sampleRate,frameRate;private final int bits,channels,frameSize;private final boolean bigEndian;private final Encoding encoding;
 public AudioFormat(float rate,int bits,int channels,boolean signed,boolean bigEndian){this(signed?Encoding.PCM_SIGNED:Encoding.PCM_UNSIGNED,rate,bits,channels,((bits+7)/8)*channels,rate,bigEndian);}
 public AudioFormat(Encoding encoding,float rate,int bits,int channels,int frameSize,float frameRate,boolean bigEndian){this.encoding=encoding;this.sampleRate=rate;this.bits=bits;this.channels=channels;this.frameSize=frameSize;this.frameRate=frameRate;this.bigEndian=bigEndian;}
 public float getSampleRate(){return sampleRate;}public int getSampleSizeInBits(){return bits;}public int getChannels(){return channels;}public boolean isBigEndian(){return bigEndian;}public Encoding getEncoding(){return encoding;}public int getFrameSize(){return frameSize;}public float getFrameRate(){return frameRate;}
 public static class Encoding {public static final Encoding PCM_SIGNED=new Encoding("PCM_SIGNED"),PCM_UNSIGNED=new Encoding("PCM_UNSIGNED");private final String name;public Encoding(String name){this.name=name;}public String toString(){return name;}}
 public String toString(){return encoding+" "+sampleRate+" Hz "+bits+" bit "+channels+" channels "+(bigEndian?"BE":"LE");}
}
