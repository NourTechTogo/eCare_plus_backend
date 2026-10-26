package com.nourtech.ecareplus.messaging.application.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Bounded, structural PCM WAV validation. Duration never comes from client metadata. */
final class AudioWav {
    private AudioWav() {}
    static boolean signature(byte[] data) { return data.length>=12 && tag(data,0).equals("RIFF") && tag(data,8).equals("WAVE"); }
    static long dureeMs(byte[] data) {
        if(!signature(data) || u32(data,4)+8!=data.length) throw invalide();
        boolean format=false, samples=false;
        int channels=0,rate=0,block=0;long byteRate=0,size=0;
        int pos=12;
        while(pos<data.length) {
            if(data.length-pos<8)throw invalide();
            String type=tag(data,pos);long length=u32(data,pos+4);
            long end=(long)pos+8+length;
            if(end>data.length)throw invalide();
            if(type.equals("fmt ")) {
                if(format || length<16)throw invalide();
                int base=pos+8;
                channels=u16(data,base+2);rate=(int)u32(data,base+4);
                byteRate=u32(data,base+8);block=u16(data,base+12);
                if(u16(data,base)!=1 || channels<1 || channels>2 || rate<8000 || rate>48000 || u16(data,base+14)!=16 || block!=channels*2 || byteRate!=(long)rate*block)throw invalide();
                format=true;
            } else if(type.equals("data")) {
                if(!format || samples || length==0)throw invalide();
                samples=true;size=length;
            }
            long next=end+(length&1);
            if(next>data.length)throw invalide();
            pos=(int)next;
        }
        if(!format || !samples || size%block!=0 || size*1000<byteRate*250 || size*1000>byteRate*120000)throw invalide();
        return (size*1000+byteRate-1)/byteRate;
    }
    private static long u32(byte[] b,int at){return Integer.toUnsignedLong(ByteBuffer.wrap(b,at,4).order(ByteOrder.LITTLE_ENDIAN).getInt());}
    private static int u16(byte[] b,int at){return Short.toUnsignedInt(ByteBuffer.wrap(b,at,2).order(ByteOrder.LITTLE_ENDIAN).getShort());}
    private static String tag(byte[] b,int at){return new String(b,at,4,StandardCharsets.US_ASCII);}
    private static IllegalArgumentException invalide(){return new IllegalArgumentException("Message vocal WAV PCM 16 bits valide attendu (250 ms à 2 minutes)");}
}
