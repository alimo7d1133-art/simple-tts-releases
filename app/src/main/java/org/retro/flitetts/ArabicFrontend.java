/* Original Old TTS Arabic rule frontend, AGPL-3.0-or-later.
 * Inventory: original AR1/AR2 README and official .pho examples. No external G2P.
 * Not a statistical diacritizer: unknown unvocalized words are heuristic. */
package org.retro.flitetts;
import java.text.Normalizer;
import java.util.*;

public final class ArabicFrontend {
 private ArabicFrontend(){}
 private static final Map<Character,String> PHONES=new HashMap<>();
 private static final Map<String,String> WORDS=new HashMap<>();
 private static final String[] DIGITS={"صِفْر","وَاحِد","اِثْنَان","ثَلَاثَة","أَرْبَعَة","خَمْسَة","سِتَّة","سَبْعَة","ثَمَانِيَة","تِسْعَة"};
 private static final String[] LATIN={"إِي","بِي","سِي","دِي","إِي","إِف","جِي","إِتْش","آي","جَيْ","كَيْ","إِل","إِم","إِن","أُو","بِي","كْيُو","آر","إِس","تِي","يُو","فِي","دَبْلُو","إِكْس","وَاي","زِد"};
 static {
  String[] values={"b","t","T","Z","X","x","d","D","r","z","s","S","s.","d.","t.","z.","H","G","f","q","k","l","m","n","h","w","j","?"};
  String letters="بتثجحخدذرزسشصضطظعغفقكلمنهويء";
  for(int i=0;i<letters.length();i++)PHONES.put(letters.charAt(i),values[i]);
  PHONES.put('أ',"?");PHONES.put('إ',"?");PHONES.put('ؤ',"?");PHONES.put('ئ',"?");PHONES.put('ک',"k");PHONES.put('ی',"j");
  String[] lex={"السَّلَام","عَلَيْكُمْ","مَرْحَبًا","بِكَ","يَا","عَلِيّ","هَذَا","هَذِهِ","صَوْت","عَرَبِيّ","شُكْرًا","نَعَم","لَا","اللَّه","أَهْلًا","العَالَم","تَحْوِيل","النَّص","إِلَى","كَلَام","إِعْدَادَات","إِعْدَاد","الهَاتِف","رَئِيسِيَّة","الرَّئِيسِيَّة","زِرّ","قَائِمَة","رُجُوع","إِلْغَاء","مُوَافِق","تَمّ","فَتْح","إِغْلَاق","حَذْف","تَحْرِير","بَحْث","تَشْغِيل","إِيقَاف","تَجْرِبَة","الصَّوْت","صُورَة","رِسَالَة","رَسَائِل","وَاحِد","اِثْنَان","ثَلَاثَة","أَرْبَعَة","خَمْسَة","سِتَّة","سَبْعَة","ثَمَانِيَة","تِسْعَة","صِفْر","بَطَّارِيَّة","الشَّاشَة","قَارِئ","إِمْكَانِيَّة","الوُصُول","المَزِيد","مُحَرِّك","اخْتِيَار","السُّرْعَة","لُغَة","العَرَبِيَّة","إِنْجِلِيزِيّ","مَلَفّ","مَحَلِّيّ","شَبَكَة","مِائَة","بِالمِائَة","مُحَدَّد","غَيْر","مُتَاح","نُقْطَة","فَاصِلَة","شَرْطَة","عَلَامَة","اسْتِفْهَام","نِسْبَة","أَلْف","مِلْيُون"};
  for(String word:lex)WORDS.put(bare(word),word);
 }
 private static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFKC).replace("\u0640","").replace('\u0671','ا').replaceAll("[\u200B-\u200F\u202A-\u202E\u2066-\u2069]","");}
 private static boolean mark(char c){return c>='\u064B'&&c<='\u065F'||c=='\u0670';}
 private static String bare(String s){return s.replaceAll("[\u064B-\u065F\u0670]","");}
 private static boolean supported(char c){return PHONES.containsKey(c)||"اةىآ".indexOf(c)>=0;}
 public static boolean accepts(String text){
  if(text==null||text.trim().isEmpty())return false;
  String s=normalize(text);boolean content=false;
  for(int i=0;i<s.length();i++){char c=s.charAt(i);
   if(supported(c)||Character.isDigit(c)||c>='a'&&c<='z'||c>='A'&&c<='Z'){content=true;continue;}
   if(mark(c)||Character.isWhitespace(c)||Character.getType(c)==Character.FORMAT)continue;
   if(Character.isLetter(c)||Character.isSurrogate(c)||Character.isISOControl(c))return false;
  }
  return content;
 }
 public static String commands(String text,float rate,float pitch){
  if(!accepts(text))throw new IllegalArgumentException("Unsupported or empty Arabic input");
  rate=Float.isFinite(rate)?Math.max(.25f,Math.min(4f,rate)):1f;
  pitch=Float.isFinite(pitch)?Math.max(.5f,Math.min(2f,pitch)):1f;
  List<Phone> phones=new ArrayList<>();phones.add(new Phone("_",35));
  String s=normalize(text);StringBuilder word=new StringBuilder();
  for(int i=0;i<=s.length();i++){
   char c=i==s.length()?' ':s.charAt(i);
   if(supported(c)||mark(c)){word.append(c);continue;}
   if(word.length()>0){appendWord(phones,word.toString());word.setLength(0);}
   int digit=Character.digit(c,10);
   if(digit>=0){appendWord(phones,DIGITS[digit]);phones.add(new Phone("_",22));}
   else if(c>='a'&&c<='z'||c>='A'&&c<='Z'){appendWord(phones,LATIN[Character.toLowerCase(c)-'a']);phones.add(new Phone("_",25));}
   else if(".,!?;:،؛؟\n".indexOf(c)>=0)phones.add(new Phone("_",".!?؟\n".indexOf(c)>=0?180:90));
   else if(Character.isWhitespace(c)&&!phones.get(phones.size()-1).name.equals("_"))phones.add(new Phone("_",30));
   else if(c=='%')appendWord(phones,"بِالمِائَة");
   else if(c=='@')appendWord(phones,"آت");
   else if(c=='+' )appendWord(phones,"زَائِد");
   else if(c=='-')appendWord(phones,"شَرْطَة");
  }
  phones.add(new Phone("_",65));StringBuilder out=new StringBuilder();
  for(int i=0;i<phones.size();i++){
   Phone p=phones.get(i);out.append(p.name).append(' ').append(Math.max(8,Math.round(p.ms/rate)));
   if(!p.name.equals("_")){float hz=115f*pitch*(1f-.1f*i/Math.max(1,phones.size()-1));out.append(" 0 ").append(Math.round(hz)).append(" 100 ").append(Math.round(hz*.98f));}
   out.append('\n');
  }
  return out.toString();
 }
 private static class Phone{String name;int ms;Phone(String n,int d){name=n;ms=d;}}
 private static class Letter{char c;String marks="";Letter(char c){this.c=c;}}
 private static void appendWord(List<Phone> out,String original){
  boolean explicit=hasMarks(original);String w=original;
  if(!explicit){String known=WORDS.get(bare(w));if(known!=null){w=known;explicit=true;}}
  ArrayList<Letter> letters=new ArrayList<>();
  for(int i=0;i<w.length();i++){char c=w.charAt(i);if(mark(c)){if(!letters.isEmpty())letters.get(letters.size()-1).marks+=c;}else if(supported(c))letters.add(new Letter(c));}
  boolean article=letters.size()>2&&letters.get(0).c=='ا'&&letters.get(1).c=='ل';
  boolean sun=article&&"تثدذرزسشصضطظلن".indexOf(letters.get(2).c)>=0;
  for(int i=0;i<letters.size();i++){
   Letter l=letters.get(i);char c=l.c;String m=l.marks;
   if(sun&&i==1)continue;
   if(c=='ا'||c=='ى'||c=='آ'){
    if(i==letters.size()-1&&i>0&&letters.get(i-1).marks.indexOf('\u064B')>=0)continue;
    if(c=='آ'){out.add(new Phone("?",65));out.add(new Phone("aa",150));}
    else if(i==0){if(!article)out.add(new Phone("?",45));out.add(new Phone(vowel(m)==null?"a":vowel(m),65));}
    else out.add(new Phone("aa",155));continue;
   }
   String ph=PHONES.get(c);
   if(c=='ة')ph=m.isEmpty()?"h":"t";
   if(ph==null)continue;
   boolean emph=ph.endsWith(".");
   boolean shadda=m.indexOf('\u0651')>=0||sun&&i==2;
   out.add(new Phone(ph,shadda?145:75));
   String v=vowel(m);boolean tanween=m.indexOf('\u064B')>=0||m.indexOf('\u064C')>=0||m.indexOf('\u064D')>=0;
   if(v==null&&!explicit&&i+1<letters.size()&&c!='و'&&c!='ي'){
    char next=letters.get(i+1).c;v=next=='ي'?"i":next=='و'?"u":"a";
   }
   if(v==null&&c=='إ')v="i";else if(v==null&&c=='أ'&&!explicit)v="a";
   boolean longV=m.indexOf('\u0670')>=0;
   if(v!=null&&!tanween&&i+1<letters.size()){
    Letter next=letters.get(i+1);
    if(v.equals("a")&&(next.c=='ا'||next.c=='ى')||v.equals("i")&&next.c=='ي'&&next.marks.isEmpty()||v.equals("u")&&next.c=='و'&&next.marks.isEmpty()){longV=true;i++;}
   }
   if(longV&&v==null)v="a";
   if(v!=null){String phone=emph?v+".":longV?v+v:v;out.add(new Phone(phone,longV?155:65));}
   if(tanween)out.add(new Phone("n",65));
  }
 }
 private static boolean hasMarks(String s){for(int i=0;i<s.length();i++)if(mark(s.charAt(i)))return true;return false;}
 private static String vowel(String m){if(m.indexOf('\u064E')>=0||m.indexOf('\u064B')>=0)return "a";if(m.indexOf('\u0650')>=0||m.indexOf('\u064D')>=0)return "i";if(m.indexOf('\u064F')>=0||m.indexOf('\u064C')>=0)return "u";return null;}
}
