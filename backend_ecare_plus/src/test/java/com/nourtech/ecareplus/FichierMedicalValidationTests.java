package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
class FichierMedicalValidationTests {
    @Test void refuseDimensionsExcessivesAvantDecodage()throws Exception{
        var sortie=new ByteArrayOutputStream();var flux=new DataOutputStream(sortie);flux.write(new byte[]{(byte)137,80,78,71,13,10,26,10});
        var contenu=new ByteArrayOutputStream();var header=new DataOutputStream(contenu);header.writeInt(6000);header.writeInt(4000);header.write(new byte[]{8,2,0,0,0});chunk(flux,"IHDR",contenu.toByteArray());chunk(flux,"IEND",new byte[0]);
        assertThatThrownBy(()->com.nourtech.ecareplus.document.application.service.DocumentMedicalService.typeReel(sortie.toByteArray())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dimensions");
    }
    void chunk(DataOutputStream flux,String type,byte[] data)throws Exception{byte[] nom=type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);var crc=new java.util.zip.CRC32();crc.update(nom);crc.update(data);flux.writeInt(data.length);flux.write(nom);flux.write(data);flux.writeInt((int)crc.getValue());}
}
