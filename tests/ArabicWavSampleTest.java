package org.retro.flitetts;
import java.nio.*;import java.nio.file.*;import java.util.*;
public class ArabicWavSampleTest{
 public static void main(String[] args)throws Exception{
  int v=Integer.parseInt(args[1]);if(v!=1&&v!=2)throw new IllegalArgumentException("Arabic voice index");
  if(!NativeMbrola.initialize(args[0],null))throw new AssertionError("MBROLA init");
  String text="مَرْحَبًا. هَذَا صَوْتٌ عَرَبِيٌّ.";
  short[] a=NativeMbrola.renderPhones(ArabicFrontend.commands(text,1,1),v),fast=NativeMbrola.renderPhones(ArabicFrontend.commands(text,2,1),v),high=NativeMbrola.renderPhones(ArabicFrontend.commands(text,1,1.5f),v);
  if(a==null||a.length<1000||fast==null||fast.length>=a.length||high==null||Arrays.equals(a,high))throw new AssertionError("Arabic generation, rate or pitch");
  long energy=0;for(short x:a)energy+=Math.abs((int)x);if(energy<10000)throw new AssertionError("Silent Arabic");
  int hz=v==2?22050:16000;ByteBuffer b=ByteBuffer.allocate(44+a.length*2).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(36+a.length*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(hz).putInt(hz*2).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(a.length*2);for(short x:a)b.putShort(x);Files.write(Paths.get(args[2]),b.array());System.out.println("PASS real MBROLA AR"+v+" Hz="+hz+" samples="+a.length+" fast="+fast.length+" energy="+energy+" pitch differs; text="+text+" WAV="+args[2]);
 }
}
