package com.nourtech.ecareplus;
import com.nourtech.ecareplus.document.application.port.StockagePrive;
import com.nourtech.ecareplus.document.infrastructure.storage.StockageFichierPrive;
import com.nourtech.ecareplus.messaging.application.service.PieceJointeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.concurrent.*;
import java.io.InputStream;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@Import(PieceJointeConcurrenceTests.Configuration.class)
class PieceJointeConcurrenceTests extends BackendFixture {
    @Autowired StockageControle stockage;@Autowired PieceJointeService pieces;
    @TestConfiguration static class Configuration {@Bean @Primary StockageControle controle(StockageFichierPrive delegate){return new StockageControle(delegate);}}
    static class StockageControle implements StockagePrive {
        final StockagePrive delegate;volatile String cible;final CountDownLatch entree=new CountDownLatch(1),reprise=new CountDownLatch(1);
        StockageControle(StockagePrive d){delegate=d;}
        public FichierStocke enregistrer(InputStream flux,long limite){return delegate.enregistrer(flux,limite);}public InputStream ouvrir(String cle){return delegate.ouvrir(cle);}
        public void supprimer(String cle){if(cle.equals(cible)){entree.countDown();try{if(!reprise.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Test bloqué");}catch(InterruptedException e){throw new IllegalStateException(e);}}delegate.supprimer(cle);}
    }
    @Test void nettoyageEtRattachementNePeuventPasPerdreUnBinaireValide()throws Exception {
        patientId();var token=login();String c=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();
        String id=body(mvc.perform(multipart("/api/v1/mobile/conversations/{id}/pieces-jointes",c).file(new MockMultipartFile("fichier","test.pdf","application/pdf",DocumentMedicalIntegrationTests.PDF)).param("sourceRef","nettoyage-race").header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn()).path("id").asText();
        stockage.cible=jdbc.queryForObject("select cle_stockage from piece_jointe where id=?",String.class,UUID.fromString(id));jdbc.update("update piece_jointe set date_creation=? where id=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(2*86400)),UUID.fromString(id));
        try(var pool=Executors.newFixedThreadPool(2)){
            var nettoyage=pool.submit(()->{try{pieces.nettoyer();return null;}catch(RuntimeException e){return e;}});assertThat(stockage.entree.await(5,TimeUnit.SECONDS)).isTrue();
            var rattachement=pool.submit(()->mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",c).header("Authorization",bearer(token)).contentType("application/json").content("{\"contenu\":\"Pièce\",\"piecesJointes\":[\""+id+"\"]}" )).andReturn().getResponse().getStatus());
            Thread.sleep(500);stockage.reprise.countDown();int statut=rattachement.get(15,TimeUnit.SECONDS);var erreur=nettoyage.get(15,TimeUnit.SECONDS);
            assertThat(statut).isIn(201,403,409);if(statut==201)mvc.perform(get("/api/v1/mobile/conversations/{id}/pieces-jointes/{piece}/contenu",c,id).header("Authorization",bearer(token))).andExpect(status().isOk());assertThat(erreur).isNull();
        }finally{stockage.reprise.countDown();stockage.cible=null;}
    }
}
