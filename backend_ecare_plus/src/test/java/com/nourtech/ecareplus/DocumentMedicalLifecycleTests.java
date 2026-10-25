package com.nourtech.ecareplus;

import com.nourtech.ecareplus.document.application.port.StockagePrive;
import com.nourtech.ecareplus.document.infrastructure.storage.StockageFichierPrive;
import com.nourtech.ecareplus.document.application.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.InputStream;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@Import(DocumentMedicalLifecycleTests.Configuration.class)
class DocumentMedicalLifecycleTests extends BackendFixture {
    @Autowired StockageControle stockage;
    @Autowired DocumentMedicalService documents;
    @Autowired NettoyageDocumentService nettoyage;
    @TestConfiguration static class Configuration {
        @Bean @Primary StockageControle stockageControle(StockageFichierPrive delegate){return new StockageControle(delegate);}
    }
    static class StockageControle implements StockagePrive {
        final StockagePrive delegate;boolean panne;String derniereCle;
        StockageControle(StockagePrive delegate){this.delegate=delegate;}
        public FichierStocke enregistrer(InputStream flux,long limite){var f=delegate.enregistrer(flux,limite);derniereCle=f.cle();return f;}
        public InputStream ouvrir(String cle){return delegate.ouvrir(cle);}
        public java.util.List<String> candidatsNettoyage(java.time.Instant avant){try{return (java.util.List<String>)org.springframework.test.util.ReflectionTestUtils.invokeMethod(delegate,"candidatsNettoyage",avant);}catch(IllegalStateException absent){return java.util.List.of();}}
        public void supprimer(String cle){if(panne)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);delegate.supprimer(cle);}
    }
    @Test void suppressionReessayableApresPanneStockage() throws Exception {
        var patient=patientId();var token=login();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.decoder().decode(token)));
        try {
            var document=documents.deposer(patient,patient,commande("nettoyage"));
            documents.supprimer(patient,document.id());stockage.panne=true;
            nettoyage.nettoyer();
            assertThat(jdbc.queryForObject("select fichier_supprime from document_medical where id=?",Boolean.class,document.id())).isFalse();
            stockage.panne=false;nettoyage.nettoyer();
            assertThat(jdbc.queryForObject("select fichier_supprime from document_medical where id=?",Boolean.class,document.id())).isTrue();
            assertThatThrownBy(()->documents.contenu(patient,patient,document.id())).isInstanceOf(com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException.class);
        }finally{stockage.panne=false;SecurityContextHolder.clearContext();}
    }
    @Test void rollbackSqlRetireFichierEtAuditSansContenuMedical() throws Exception {
        var patient=patientId();var token=login();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.decoder().decode(token)));
        try {
            tx.executeWithoutResult(status->{documents.deposer(patient,patient,commande("rollback"));status.setRollbackOnly();});
            assertThat(jdbc.queryForObject("select count(*) from document_medical where patient_id=?",Integer.class,patient)).isZero();
            assertThatThrownBy(()->stockage.ouvrir(stockage.derniereCle)).isInstanceOf(ResponseStatusException.class);
        }finally{SecurityContextHolder.clearContext();}
    }
    private DocumentMedicalService.DepotDocumentCommande commande(String ref){return new DocumentMedicalService.DepotDocumentCommande(
        new MockMultipartFile("fichier","ordonnance.pdf","application/pdf",DocumentMedicalIntegrationTests.PDF),"ORDONNANCE","Titre confidentiel",LocalDate.of(2026,1,1),ref);}
    @Test void nettoyageOrphelinNeSupprimePasUnDocumentReference() throws Exception {
        var patient=patientId();var token=login();SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.decoder().decode(token)));
        try{
            documents.deposer(patient,patient,commande("conserver"));String reference=stockage.derniereCle;
            var orphelin=stockage.enregistrer(new java.io.ByteArrayInputStream(DocumentMedicalIntegrationTests.PDF),5242880).cle();
            var racine=java.nio.file.Path.of("target/test-private-documents");var ancien=java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(2*86400));
            java.nio.file.Files.setLastModifiedTime(racine.resolve(reference),ancien);java.nio.file.Files.setLastModifiedTime(racine.resolve(orphelin),ancien);
            nettoyage.nettoyer();
            assertThat(java.nio.file.Files.exists(racine.resolve(reference))).isTrue();assertThat(java.nio.file.Files.exists(racine.resolve(orphelin))).isFalse();
        }finally{SecurityContextHolder.clearContext();}
    }
}
