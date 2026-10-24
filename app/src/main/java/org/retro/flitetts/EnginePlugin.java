/* Old TTS engine plugin contract. New code, under the bundled CMU Flite license. */
package org.retro.flitetts;
import java.util.Locale;

public interface EnginePlugin {
 String id();
 String label();
 Locale locale();
 String[] voiceIds();
 String[] voiceLabels();
 String sampleText();
 default String sampleText(int voice){return sampleText();}
 default void initialize(android.content.Context context) {}
 String languageNotice();
 String[] noticeAssets();
 default Locale voiceLocale(int voice){return locale();}
 int languageAvailability(String language,String country,String variant);
 boolean acceptsText(String text);
 default boolean acceptsText(String text,int voice){return acceptsText(text);}
 int sampleRate(int voice);
 short[] synthesize(String text,int voice,float rate,float pitch);
}
