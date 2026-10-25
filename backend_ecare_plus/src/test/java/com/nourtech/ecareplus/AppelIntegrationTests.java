package com.nourtech.ecareplus;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.test.context.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@org.springframework.context.annotation.Import(AppelIntegrationTests.Configuration.class)
class AppelIntegrationTests extends BackendFixture {
    @org.springframework.beans.factory.annotation.Autowired FournisseurControle controle;
    @org.springframework.boot.test.context.TestConfiguration static class Configuration {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary FournisseurControle controle(com.nourtech.ecareplus.calling.infrastructure.livekit.FournisseurLiveKit delegate){return new FournisseurControle(delegate);}
    }
    static class FournisseurControle implements com.nourtech.ecareplus.calling.application.port.FournisseurAppel {
        final com.nourtech.ecareplus.calling.application.port.FournisseurAppel delegate;volatile java.util.concurrent.CountDownLatch entree,reprise;volatile long emissionNanos,fermetureNanos;
        FournisseurControle(com.nourtech.ecareplus.calling.application.port.FournisseurAppel delegate){this.delegate=delegate;}
        public boolean configure(){return delegate.configure();}public void creerSalle(String salle){delegate.creerSalle(salle);}
        public AccesAppel autoriserParticipant(String salle,java.util.UUID utilisateur,String type){if(entree!=null){entree.countDown();try{if(!reprise.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Test bloqué");}catch(InterruptedException e){throw new IllegalStateException(e);}}var acces=delegate.autoriserParticipant(salle,utilisateur,type);emissionNanos=System.nanoTime();return acces;}
        public void fermerSalle(String salle,java.util.UUID a,java.util.UUID b){delegate.fermerSalle(salle,a,b);fermetureNanos=System.nanoTime();}
        public void verifierWebhook(String corps,String autorisation){delegate.verifierWebhook(corps,autorisation);}
    }
    static HttpServer fournisseur;static AtomicBoolean panne=new AtomicBoolean();
    @DynamicPropertySource static void configurer(DynamicPropertyRegistry r)throws Exception{
        fournisseur=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);fournisseur.createContext("/twirp/livekit.RoomService/",e->{
            String requete=new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);byte[] reponse=(e.getRequestURI().getPath().endsWith("CreateRoom")?requete:"{}").getBytes();e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(panne.get()?503:200,reponse.length);e.getResponseBody().write(reponse);e.close();});fournisseur.start();
        r.add("app.appels.livekit-url",()->"ws://127.0.0.1:"+fournisseur.getAddress().getPort());r.add("app.appels.api-key",()->"key-test");r.add("app.appels.api-secret",()->"secret-livekit-test-de-plus-de-32-octets");r.add("app.appels.revocation-active",()->"true");r.add("app.traitements-planifies",()->"false");
    }
    @AfterAll static void arreter(){if(fournisseur!=null)fournisseur.stop(0);}
    String conversation(String token)throws Exception{return body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();}
    @Test void seulsParticipantsRejoignentEtAcceptationApresFinEstConflit()throws Exception{
        patientId();var token=login();var conversation=conversation(token);String creation="{\"type\":\"AUDIO\",\"sourceRef\":\"appel-1\"}";
        var appel=body(mvc.perform(post("/api/v1/mobile/conversations/{id}/appels",conversation).header("Authorization",bearer(token)).contentType("application/json").content(creation)).andExpect(status().isCreated()).andExpect(jsonPath("$.statut").value("EN_ATTENTE")).andReturn());
        String id=appel.path("id").asText();
        mvc.perform(post("/api/v1/mobile/conversations/{id}/appels",conversation).header("Authorization",bearer(token)).contentType("application/json").content(creation)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/api/v1/appels/{id}/acceptation",id).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/appels/{id}/acceptation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("ACCEPTE"));
        var acces=body(mvc.perform(get("/api/v1/appels/{id}/connexion",id).header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn());
        var claims=json.readTree(Base64.getUrlDecoder().decode(acces.path("jeton").asText().split("\\.")[1]));assertThat(claims.path("iss").asText()).isEqualTo("key-test");assertThat(claims.path("video").path("roomJoin").asBoolean()).isTrue();
        assertThat(claims.path("video").path("canPublishSources").toString()).doesNotContain("camera");
        mvc.perform(post("/api/v1/appels/{id}/fin",id).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("TERMINE"));
        mvc.perform(post("/api/v1/appels/{id}/acceptation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/appels/{id}/connexion",id).header("Authorization",bearer(token))).andExpect(status().isConflict());
    }
    @Test void fournisseurIndisponibleNeRetournePasDeFauxSucces()throws Exception{
        patientId();var token=login();var conversation=conversation(token);panne.set(true);
        try{mvc.perform(post("/api/v1/mobile/conversations/{id}/appels",conversation).header("Authorization",bearer(token)).contentType("application/json").content("{\"type\":\"VIDEO\",\"sourceRef\":\"panne-1\"}" )).andExpect(status().isServiceUnavailable());}
        finally{panne.set(false);}
    }
    @Test void webhookSigneVerifieCorpsEtRejeuSansDoublerHistorique()throws Exception{
        patientId();var token=login();var conversation=conversation(token);
        var appel=body(mvc.perform(post("/api/v1/mobile/conversations/{id}/appels",conversation).header("Authorization",bearer(token)).contentType("application/json").content("{\"type\":\"AUDIO\",\"sourceRef\":\"webhook\"}" )).andExpect(status().isCreated()).andReturn());String id=appel.path("id").asText();
        mvc.perform(post("/api/v1/appels/{id}/acceptation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        var acces=body(mvc.perform(get("/api/v1/appels/{id}/connexion",id).header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn());
        String salle=json.readTree(Base64.getUrlDecoder().decode(acces.path("jeton").asText().split("\\.")[1])).path("video").path("room").asText();
        String evenement="{\"id\":\"evt-"+suffix+"\",\"event\":\"room_finished\",\"room\":{\"name\":\""+salle+"\"}}";
        String signature=signerWebhook(evenement);
        mvc.perform(post("/api/v1/integrations/appels/livekit").header("Authorization",signature).contentType("application/webhook+json").content(evenement.replace("room_finished","participant_joined"))).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(post("/api/v1/integrations/appels/livekit").header("Authorization",signature).contentType("application/webhook+json").content(evenement)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from evenement_appel where identifiant_externe=?",Integer.class,"evt-"+suffix)).isEqualTo(1);
    }
    @Test void deconnexionTermineAppelEtFermeSalle()throws Exception{
        patientId();var token=login();String c=conversation(token);
        String id=body(mvc.perform(post("/api/v1/mobile/conversations/{id}/appels",c).header("Authorization",bearer(token)).contentType("application/json").content("{\"type\":\"AUDIO\",\"sourceRef\":\"logout-call\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText();
        mvc.perform(post("/api/v1/appels/{id}/acceptation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/deconnexion").header("Authorization",bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/mobile/conversations/{id}/appels",c).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].statut").value("TERMINE")).andExpect(jsonPath("$.content[0].fermetureConfirmee").value(true));
        mvc.perform(get("/api/v1/appels/{id}/connexion",id).header("Authorization",bearer(doctorToken))).andExpect(status().isConflict());
    }
    String signerWebhook(String corps)throws Exception{
        var claims=new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("key-test").expirationTime(Date.from(java.time.Instant.now().plusSeconds(60)))
            .claim("sha256",Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(corps.getBytes(java.nio.charset.StandardCharsets.UTF_8)))).build();
        var jwt=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),claims);jwt.sign(new com.nimbusds.jose.crypto.MACSigner("secret-livekit-test-de-plus-de-32-octets"));return jwt.serialize();
    }
    @Test void emissionJetonNePeutPasDepasserLaRevocationConcurrente()throws Exception{
        patientId();var token=login();var c=conversation(token);String id=body(mvc.perform(post("/api/v1/mobile/conversations/{id}/appels",c).header("Authorization",bearer(token)).contentType("application/json").content("{\"type\":\"AUDIO\",\"sourceRef\":\"race\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();
        mvc.perform(post("/api/v1/appels/{id}/acceptation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        controle.entree=new java.util.concurrent.CountDownLatch(1);controle.reprise=new java.util.concurrent.CountDownLatch(1);controle.emissionNanos=0;controle.fermetureNanos=0;
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var connexion=pool.submit(()->mvc.perform(get("/api/v1/appels/{id}/connexion",id).header("Authorization",bearer(token))).andReturn().getResponse().getStatus());
            assertThat(controle.entree.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var fin=pool.submit(()->mvc.perform(post("/api/v1/appels/{id}/fin",id).header("Authorization",bearer(doctorToken))).andReturn().getResponse().getStatus());
            Thread.sleep(1300);controle.reprise.countDown();int statut=connexion.get(15,java.util.concurrent.TimeUnit.SECONDS);assertThat(fin.get(15,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
            if(statut==200)assertThat(controle.emissionNanos).isLessThan(controle.fermetureNanos);else assertThat(statut).isEqualTo(409);
        }finally{controle.reprise.countDown();controle.entree=null;controle.reprise=null;}
    }
}
