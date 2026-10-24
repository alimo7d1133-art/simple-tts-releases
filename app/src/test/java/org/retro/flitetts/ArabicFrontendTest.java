package org.retro.flitetts;
import java.util.*;
public final class ArabicFrontendTest {
 private static int passed;
 private static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);passed++;}
 private static Set<String> phones(String text){Set<String> p=new HashSet<>();for(String line:ArabicFrontend.commands(text,1,1).split("\n"))p.add(line.split(" ")[0]);return p;}
 private static int duration(String cmd){int ms=0;for(String l:cmd.split("\n"))ms+=Integer.parseInt(l.split(" ")[1]);return ms;}
 public static void main(String[] args){
  String[] corpus={"السَّلَامُ عَلَيْكُمْ. مَرْحَبًا.","هَذَا صَوْتٌ عَرَبِيٌّ.","صَ طِ ضُ ظَ صَا طِي ضُو", "إِعْدَادَاتُ الهَاتِفِ", "٢٠٢٦ 123 Old TTS", "مرحبا إعدادات الشاشة", "بْ تْ ثْ جْ حْ خْ دْ ذْ رْ زْ سْ شْ صْ ضْ طْ ظْ عْ غْ فْ قْ كْ لْ مْ نْ هْ وْ يْ ءْ", "ﻻ ﺍﻟﺴﻼﻡ"};
  Set<String> inventory=new HashSet<>(Arrays.asList("_ b t T Z X x d D r z s S s. d. t. z. H G f q k l m n h w j ? a i u aa ii uu a. i. u.".split(" ")));
  for(String text:corpus){check(ArabicFrontend.accepts(text),"Accept: "+text);String cmd=ArabicFrontend.commands(text,1,1);String previous="";
   for(String line:cmd.split("\n")){String[] col=line.split(" ");check(inventory.contains(col[0]),"Unknown phone "+line);check(Integer.parseInt(col[1])>0,"Positive duration");if(col.length>2){check(col.length==6,"Well-formed targets "+line);int hz=Integer.parseInt(col[3]);check(hz>=90&&hz<=130,"Real pitch Hz not ratio "+line);}
    if(Arrays.asList("s.","d.","t.","z.").contains(previous)&&Arrays.asList("a","i","u","aa","ii","uu").contains(col[0]))throw new AssertionError("Uncolored vowel after emphatic");previous=col[0];
   }
  }
  check(phones("بَاب").contains("aa"),"Long alif");check(phones("فِي").contains("ii"),"Long ya");check(phones("نُور").contains("uu"),"Long waw");
  check(!phones("بْتْ").contains("a"),"Sukun respected");check(phones("مَرْحَبًا").contains("n"),"Tanween n");
  check(!ArabicFrontend.accepts("Привет"),"Unsupported script rejected");check(!ArabicFrontend.accepts(""),"Empty rejected");check(!ArabicFrontend.accepts("😀"),"Unsupported emoji-only rejected");
  check(duration(ArabicFrontend.commands(corpus[0],2,1))<duration(ArabicFrontend.commands(corpus[0],1,1)),"Rate shortens duration");
  check(!ArabicFrontend.commands(corpus[0],1,1).equals(ArabicFrontend.commands(corpus[0],1,1.5f)),"Pitch changes target Hz");
  System.out.println("ArabicFrontendTest PASS: "+passed+" assertions; "+corpus.length+" corpus items; Arabic phone inventory, tashkeel, durations, pitch, script rejection.");
 }
}
