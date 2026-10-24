package org.retro.flitetts;
import java.io.*;
import java.util.*;
public class NativeTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void le(DataOutputStream out,int v,int bytes)throws Exception{for(int i=0;i<bytes;i++)out.writeByte(v>>(i*8));}
 static void wav(File path,short[] data,int rate)throws Exception{try(DataOutputStream out=new DataOutputStream(new FileOutputStream(path))){out.writeBytes("RIFF");le(out,36+data.length*2,4);out.writeBytes("WAVEfmt ");le(out,16,4);le(out,1,2);le(out,1,2);le(out,rate,4);le(out,rate*2,4);le(out,2,2);le(out,16,2);out.writeBytes("data");le(out,data.length*2,4);for(short v:data)le(out,v,2);}}
 public static void main(String[] args)throws Exception{
  String[] names={"kal","kal16","awb","rms","slt"};new File(args[0]).mkdirs();
  for(int i=0;i<5;i++){
   long start=System.nanoTime();int rate=NativeBridge.sampleRate(i);
   short[] normal=NativeBridge.render("Hello. This is the classic Flite voice. Welcome to the old school.",i,1f,1f);
   check(normal!=null&&normal.length>rate,"No audio: "+names[i]);
   long energy=0;for(short s:normal)energy+=Math.abs((int)s);check(energy>100000,"Silent audio");
   short[] fast=NativeBridge.render("Hello. This is the classic Flite voice. Welcome to the old school.",i,2f,1f);
   check(fast.length<normal.length,"Rate not effective: "+names[i]);
   short[] high=NativeBridge.render("Hello. This is the classic Flite voice. Welcome to the old school.",i,1f,1.5f);
   check(!Arrays.equals(normal,high),"Pitch not effective: "+names[i]);
   wav(new File(args[0],names[i]+".wav"),normal,rate);
   System.out.printf(Locale.US,"PASS %s: rate=%d, normal=%d, fast=%d, mean_abs=%.1f, elapsed=%.3fs%n",names[i],rate,normal.length,fast.length,energy/(double)normal.length,(System.nanoTime()-start)/1e9);
  }
  for(int i=0;i<30;i++){short[] s=NativeBridge.render("Test number "+i+".",i%5,1f,1f);check(s!=null&&s.length>0,"Repeated synthesis failed");}
  short[] empty=NativeBridge.render("",0,1f,1f);check(empty!=null,"Empty text failed");
  System.out.println("PASS 30 repeated utterances and empty text; all five voices generated PCM; speed and pitch affect each voice.");
 }
}
