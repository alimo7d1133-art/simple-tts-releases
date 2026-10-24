package org.retro.flitetts;
import java.nio.*;import java.nio.file.*;import java.util.*;
public class FliteExtraVoicesTest{
 public static void main(String[] args)throws Exception{
  if(!NativeBridge.loadExtraVoices(args[0]))throw new AssertionError("No real extra model files");
  int voice=Integer.parseInt(args[1]);String id=new String[]{"BDL","CLB","JMK","KSP"}[voice-5];
  String text="Hello. This is the "+id+" voice in Old TTS. I hope you enjoy listening.";
  int hz=NativeBridge.sampleRate(voice);short[] a=NativeBridge.render(text,voice,1,1),fast=NativeBridge.render(text,voice,2,1),high=NativeBridge.render(text,voice,1,1.5f);
  if(a==null||a.length<500||fast==null||fast.length>=a.length||high==null||Arrays.equals(a,high))throw new AssertionError("Bad real model synthesis, rate or pitch for "+id);
  long energy=0;for(short x:a)energy+=Math.abs((int)x);if(energy<10000)throw new AssertionError("Silent model");
  ByteBuffer b=ByteBuffer.allocate(44+a.length*2).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(36+a.length*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(hz).putInt(hz*2).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(a.length*2);for(short x:a)b.putShort(x);Files.write(Paths.get(args[2]),b.array());System.out.println("PASS real Flite "+id+" Hz="+hz+" samples="+a.length+" fast="+fast.length+" energy="+energy+" pitch differs; text="+text+" WAV="+args[2]);
 }
}
