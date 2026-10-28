package com.nourtech.ecareplus;

import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportConsultationPdfIntegrationTests extends RapportConsultationFixture {
    @Test void telechargementPriveEtDonneesFigees() throws Exception {
        var c=compte(true);
        mesure(c,TypeMesure.POIDS,"81.25",null,"kg","2026-10-20T08:00:00Z");
        var r=generer(c,commande());String url=API+"/"+r.path("id").asText()+"/pdf";
        var response=mvc.perform(get(url).header("Authorization",bearer(c.token())))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")))
            .andReturn().getResponse();
        assertThat(new String(response.getContentAsByteArray(),0,5,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        mesure(c,TypeMesure.POIDS,"95",null,"kg","2026-10-21T08:00:00Z");
        var apres=mvc.perform(get(url).header("Authorization",bearer(c.token()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var avantDoc=org.apache.pdfbox.pdmodel.PDDocument.load(response.getContentAsByteArray());var apresDoc=org.apache.pdfbox.pdmodel.PDDocument.load(apres)){
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(apresDoc)).isEqualTo(new org.apache.pdfbox.text.PDFTextStripper().getText(avantDoc));
        }
        assertThat(lire(c,r.path("id").asText())).isEqualTo(r);
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        var autre=compte(true);
        mvc.perform(get(url).header("Authorization",bearer(autre.token()))).andExpect(status().isForbidden());
        mvc.perform(get(url).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization",bearer(c.token())).contentType("application/json").content("{\"accepte\":false}")).andExpect(status().isOk());
        mvc.perform(get(url).header("Authorization",bearer(c.token()))).andExpect(status().isForbidden());
    }
    @Test void limiteDeRenduRepondSansPdfPartiel()throws Exception{
        var c=compte(true);var rapport=generer(c,commande());
        // Seed an oversized snapshot to exercise the real HTTP error contract.
        ((com.fasterxml.jackson.databind.node.ObjectNode)rapport).set("avertissements",json.valueToTree(java.util.Collections.nCopies(20000,"Long texte fictif pour vérifier la limite du document PDF sans résultat partiel.")));
        var id=java.util.UUID.fromString(rapport.path("id").asText());
        jdbc.update("update rapport_suivi set contenu_synthese=? where id=?",json.writeValueAsString(rapport),id);
        mvc.perform(get(API+"/"+id+"/pdf").header("Authorization",bearer(c.token())))
            .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PDF trop volumineux")));
    }
}
