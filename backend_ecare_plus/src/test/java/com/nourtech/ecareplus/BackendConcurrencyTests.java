package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class BackendConcurrencyTests extends BackendFixture {
    @org.springframework.beans.factory.annotation.Autowired com.nourtech.ecareplus.communication.application.service.DistributionNotificationService distribution;
    @org.springframework.beans.factory.annotation.Autowired com.nourtech.ecareplus.messaging.application.service.MessagerieService messagerie;
    record ContexteNotification(UUID patient,String token,String conversation){}
    ContexteNotification contexteNotification()throws Exception{var patient=patientId();var token=login();mvc.perform(put("/api/v1/compte/terminaux/{id}","worker-"+suffix).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"plateforme\":\"WEB\",\"jetonPush\":\"fictif-"+suffix+"\"}" )).andExpect(status().isOk());String conversation=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();return new ContexteNotification(patient,token,conversation);}
    @Test void deuxWorkersNeReserventPasLeMemeEnvoi()throws Exception{
        var contexte=contexteNotification();mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",contexte.conversation()).header("Authorization",bearer(contexte.token())).contentType("application/json").content("{\"contenu\":\"Test worker\"}" )).andExpect(status().isCreated());
        UUID envoi=jdbc.queryForObject("select id from envoi_notification where destinataire_id=?",UUID.class,doctor);var depart=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){Callable<Object> operation=()->{depart.await();return distribution.reclamer(envoi);};var a=pool.submit(operation);var b=pool.submit(operation);depart.countDown();var resultats=Arrays.asList(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));assertThat(resultats.stream().filter(Objects::nonNull).count()).isEqualTo(1);}
        assertThat(jdbc.queryForObject("select tentatives from envoi_notification where id=?",Integer.class,envoi)).isEqualTo(1);
    }
    @Test void rollbackMetierNeProduitPasMessageNiEnvoi()throws Exception{
        var contexte=contexteNotification();org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt.decoder().decode(contexte.token())));
        try{tx.executeWithoutResult(s->{messagerie.envoyer(contexte.patient(),UUID.fromString(contexte.conversation()),"À annuler");s.setRollbackOnly();});}
        finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        assertThat(jdbc.queryForObject("select count(*) from envoi_notification where destinataire_id=?",Integer.class,doctor)).isZero();assertThat(jdbc.queryForObject("select count(*) from message_conversation where conversation_id=?",Integer.class,UUID.fromString(contexte.conversation()))).isZero();
    }
    @Test void confirmationsConcurrentesRejoueesNEnregistrentQuUneReponse()throws Exception{
        var patient=patientId();var date=java.time.LocalDate.now(java.time.ZoneOffset.UTC);tx.executeWithoutResult(s->{var d=em.createQuery("select d from DossierSuivi d where d.patient.id=:id",com.nourtech.ecareplus.care.domain.entity.DossierSuivi.class).setParameter("id",patient).getSingleResult();var t=new com.nourtech.ecareplus.care.domain.entity.Traitement(d,"Concurrent",null,date,date,"Médecin",null);em.persist(t);var m=new com.nourtech.ecareplus.care.domain.entity.Medicament("Exemple","Exemple","comprimé","10","mg");em.persist(m);em.persist(new com.nourtech.ecareplus.care.domain.entity.PriseMedicament(t,m,java.math.BigDecimal.ONE,"comprimé","QUOTIDIEN",java.time.LocalTime.MIDNIGHT,null,"orale",null));});
        var token=login();var occurrence=body(mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du",date.toString()).param("au",date.toString()).header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn()).path("occurrences").get(0);
        String corps="{\"action\":\"PRIS\",\"dateAction\":\""+java.time.Instant.now().minusSeconds(1)+"\",\"sourceRef\":\"concurrent-prise\",\"version\":"+occurrence.path("version").asLong()+"}";var depart=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){Callable<String> operation=()->{depart.await();return mvc.perform(post("/api/v1/mobile/me/prises/{id}/reponse",occurrence.path("id").asText()).header("Authorization",bearer(token)).contentType("application/json").content(corps)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();};var a=pool.submit(operation);var b=pool.submit(operation);depart.countDown();assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(b.get(30,TimeUnit.SECONDS));}
        assertThat(jdbc.queryForObject("select count(*) from reponse_prise_operation where acteur_id=?",Integer.class,patient)).isEqualTo(1);
    }
    @Test void depotsConcurrentsAvecMemeCleNeCreentQuUnDocument()throws Exception{
        var patient=patientId();var token=login();var depart=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<String> operation=()->{depart.await();return body(mvc.perform(multipart("/api/v1/mobile/me/documents").file(new MockMultipartFile("fichier","test.pdf","application/pdf",DocumentMedicalIntegrationTests.PDF))
                .param("typeDocument","AUTRE").param("titre","Concurrent").param("dateDocument","2026-01-01").param("sourceRef","concurrent")
                .header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn()).path("id").asText();};
            var a=pool.submit(operation);var b=pool.submit(operation);depart.countDown();assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(b.get(30,TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("select count(*) from document_medical where patient_id=?",Integer.class,patient)).isEqualTo(1);
    }
    @Test void deuxScansQrConcurrentsNeConsommentQuUneFois()throws Exception{
        var patient=patientId();var token=login();String qr=body(mvc.perform(post("/api/v1/mobile/me/referencements").header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn()).path("jeton").asText();
        String corps="{\"jeton\":\""+qr+"\",\"professionnelId\":\""+doctor+"\",\"versionDossier\":0}";var depart=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Integer> operation=()->{depart.await();return mvc.perform(post("/api/v1/portail/referencements").header("Authorization",bearer(adminToken)).contentType("application/json").content(corps)).andReturn().getResponse().getStatus();};
            var a=pool.submit(operation);var b=pool.submit(operation);depart.countDown();assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from dossier_suivi where patient_id=?",Integer.class,patient)).isEqualTo(1);
    }
}
