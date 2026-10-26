package com.nourtech.ecareplus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VoiceMessageIntegrationTests extends BackendFixture {
    static byte[] wav(int milliseconds) {
        int data=Math.toIntExact(32000L*milliseconds/1000);
        var b=ByteBuffer.allocate(44+data).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(36+data).put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        b.putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16);
        b.put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(data);
        return b.array();
    }
    record Contexte(String token,String conversation){}
    Contexte contexte()throws Exception {
        patientId();var token=login();
        var id=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText();
        return new Contexte(token,id);
    }
    @Test void vocalPriveDureeReelleEtReessaisIdempotents()throws Exception {
        var c=contexte();var bytes=wav(1000);var source="vocal-"+UUID.randomUUID();
        String piece=null;
        for(int i=0;i<2;i++) {
            var response=body(mvc.perform(multipart("/api/v1/mobile/conversations/{id}/pieces-jointes",c.conversation()).file(new MockMultipartFile("fichier","vocal.wav","audio/wav",bytes))
                .param("sourceRef",source).param("dureeMs","9999999").header("Authorization",bearer(c.token())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dureeMs").value(1000)).andExpect(jsonPath("$.typeMime").value("audio/wav")).andExpect(jsonPath("$.cleStockage").doesNotExist()).andReturn());
            if(piece!=null)org.assertj.core.api.Assertions.assertThat(response.path("id").asText()).isEqualTo(piece);
            piece=response.path("id").asText();
        }
        var url="/api/v1/mobile/conversations/"+c.conversation()+"/pieces-jointes/"+piece+"/contenu";
        mvc.perform(get(url).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        var messageSource=UUID.randomUUID();String message=null;
        for(int i=0;i<2;i++) {
            var response=body(mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",c.conversation()).header("Authorization",bearer(c.token())).contentType("application/json")
                .content("{\"contenu\":\"\",\"piecesJointes\":[\""+piece+"\"],\"sourceRef\":\""+messageSource+"\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.piecesJointes[0].dureeMs").value(1000)).andReturn());
            if(message!=null)org.assertj.core.api.Assertions.assertThat(response.path("id").asText()).isEqualTo(message);
            message=response.path("id").asText();
        }
        mvc.perform(get(url).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(content().contentType("audio/wav")).andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(bytes));
        mvc.perform(get(url).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages",c.conversation()).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].piecesJointes[0].dureeMs").value(1000));
    }
    @Test void fauxFormatsDureesExcessivesEtEntetesTronquesRefuses()throws Exception {
        var c=contexte();var forged=wav(1000);forged[28]=1;
        for(byte[] data:new byte[][]{new byte[]{82,73,70,70},wav(100),wav(120001),forged,java.util.Arrays.copyOf(wav(1000),100)}) {
            mvc.perform(multipart("/api/v1/mobile/conversations/{id}/pieces-jointes",c.conversation()).file(new MockMultipartFile("fichier","vocal.wav","audio/wav",data))
                .param("sourceRef",UUID.randomUUID().toString()).header("Authorization",bearer(c.token()))).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/v1/mobile/conversations/{id}/pieces-jointes",c.conversation()).file(new MockMultipartFile("fichier","fake.png","image/png",wav(1000)))
            .param("sourceRef",UUID.randomUUID().toString()).header("Authorization",bearer(c.token()))).andExpect(status().isBadRequest());
    }
}
