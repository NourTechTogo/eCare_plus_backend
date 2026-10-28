package com.nourtech.ecareplus;

import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.application.service.*;
import com.nourtech.ecareplus.communication.application.service.DistributionNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RappelTraitementIntegrationTests extends BackendFixture {
    @Autowired ApplicationContext context;
    @Autowired ObservanceService observance;
    @Autowired PlanningHistoriqueService historique;
    @Autowired DistributionNotificationService distribution;
    record Cas(UUID patient, UUID prise, UUID traitement, String token) {}
    Cas preparer(String fuseau, Instant due, String frequence) throws Exception {
        suffix=UUID.randomUUID().toString().substring(0,8);UUID patient=patientId();String token=login();
        // Historical due instants require a consent already effective at that instant.
        jdbc.update("update consentement set date_acceptation=? where patient_id=?",java.sql.Timestamp.from(due.minusSeconds(86400)),patient);
        // Personal dossier deliberately has no professional assignment requirement.
        return tx.execute(s->{
            var d=em.createQuery("select d from DossierSuivi d where d.patient.id=:id and d.structure is null",DossierSuivi.class).setParameter("id",patient).getSingleResult();
            var jour=due.atZone(ZoneId.of(fuseau));
            var t=new Traitement(d,"Exemple fictif",null,jour.toLocalDate().minusDays(2),null,null,null);em.persist(t);
            var m=new Medicament("Exemple fictif","Exemple","Comprimé","10","mg");em.persist(m);
            var p=new PriseMedicament(t,m,BigDecimal.ONE,"comprimé",frequence,jour.toLocalTime(),null,null,null);em.persist(p);
            if(frequence.equals("QUOTIDIEN"))p.configurerPlanning(fuseau,Set.of(1,2,3,4,5,6,7));
            return new Cas(patient,p.getId(),t.getId(),token);
        });
    }
    int traiter(Cas c, Instant now) throws Exception {
        assertThat(context.containsBean("rappelTraitementService")).as("Le serveur doit générer les rappels sans consultation du planning").isTrue();
        Object service=context.getBean("rappelTraitementService");
        return (Integer)service.getClass().getMethod("traiter",UUID.class,Instant.class).invoke(service,c.patient(),now);
    }
    UUID occurrence(Cas c,Instant due){return jdbc.queryForObject("select id from confirmation_prise where prise_medicament_id=? and date_initiale=?",UUID.class,c.prise(),java.sql.Timestamp.from(due));}
    void repondre(Cas c,UUID id,String action,Instant report,long version)throws Exception{
        mvc.perform(post("/api/v1/mobile/me/prises/{id}/reponse",id).header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(reponse(action,report,version)))).andExpect(status().isOk());
    }
    Map<String,Object> reponse(String action,Instant report,long version){
        Map<String,Object> body=new HashMap<>();body.put("action",action);body.put("reporteeAu",report==null?null:report.toString());body.put("dateAction",Instant.now().toString());body.put("sourceRef",UUID.randomUUID().toString());body.put("version",version);return body;
    }
    int nombre(UUID occurrence){return jdbc.queryForObject("select count(*) from notification where type_ressource='PRISE' and ressource_id=?",Integer.class,occurrence);}

    @Test void genereSansLectureDuPlanningUneSeuleNotificationPatient() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("Africa/Lome",due,"QUOTIDIEN");
        jdbc.update("delete from affectation_patient where patient_id=?",c.patient());
        assertThat(traiter(c,due.minusSeconds(1))).isZero();
        assertThat(traiter(c,due.plusSeconds(1))).isEqualTo(1);
        assertThat(traiter(c,due.plusSeconds(2))).isZero();
        UUID id=occurrence(c,due);assertThat(nombre(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select utilisateur_id from notification where ressource_id=?",UUID.class,id)).isEqualTo(c.patient());
        assertThat(jdbc.queryForObject("select statut from confirmation_prise where id=?",String.class,id)).isEqualTo("ATTENTE");
    }
    @Test void neRattrapePasLesJoursPrecedentsEtRespecteLeFuseau() throws Exception {
        Instant due=Instant.parse("2026-10-27T23:30:00Z");
        var c=preparer("Pacific/Kiritimati",due,"QUOTIDIEN");
        assertThat(traiter(c,due.minusSeconds(1))).isZero();
        assertThat(traiter(c,due.plusSeconds(1))).isEqualTo(1);
        assertThat(nombre(occurrence(c,due))).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notification n join confirmation_prise c on c.id=n.ressource_id where c.prise_medicament_id=?",Integer.class,c.prise())).isEqualTo(1);
    }
    @Test void frequenceImpreciseEtConsentementRetireNeDeclenchentRien() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"SELON_BESOIN");assertThat(traiter(c,due.plusSeconds(1))).isZero();
        var autre=preparer("UTC",due,"QUOTIDIEN");
        jdbc.update("update consentement set statut='RETIRE' where patient_id=?",autre.patient());
        assertThat(traiter(autre,due.plusSeconds(1))).isZero();
    }
    @Test void reponseEtReportInvalidentAncienneNotification() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");assertThat(traiter(c,due.plusSeconds(1))).isEqualTo(1);
        UUID id=occurrence(c,due);
        long version=jdbc.queryForObject("select version from confirmation_prise where id=?",Long.class,id);
        repondre(c,id,"REPORTER",due.plusSeconds(60),version);
        assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id=? and date_suppression is null",Integer.class,id)).isZero();
        assertThat(traiter(c,due.plusSeconds(59))).isZero();
        assertThat(traiter(c,due.plusSeconds(61))).isEqualTo(1);
        version=jdbc.queryForObject("select version from confirmation_prise where id=?",Long.class,id);
        repondre(c,id,"PRIS",null,version);
        assertThat(traiter(c,due.plusSeconds(62))).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id=? and date_suppression is null",Integer.class,id)).isZero();
    }
    @Test void suspensionMasqueLeRappelMemeSiEcheanceEstPassee() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");assertThat(traiter(c,due.plusSeconds(1))).isEqualTo(1);
        tx.executeWithoutResult(s->{var t=em.find(Traitement.class,c.traitement());historique.avant(t);t.suspendre();historique.apres(t);});
        assertThat(traiter(c,due.plusSeconds(2))).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id=? and date_suppression is null",Integer.class,occurrence(c,due))).isZero();
    }
    @Test void notificationPriveeEtEnvoiSontInvalidesApresOubli() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");
        mvc.perform(put("/api/v1/compte/terminaux/test-prise-"+suffix).header("Authorization",bearer(c.token())).contentType("application/json").content("{\"plateforme\":\"ANDROID\",\"jetonPush\":\"push-prise-test\"}")).andExpect(status().isOk());
        assertThat(traiter(c,Instant.now())).isEqualTo(1);UUID id=occurrence(c,due);
        UUID n=jdbc.queryForObject("select id from notification where ressource_id=?",UUID.class,id);
        UUID e=jdbc.queryForObject("select id from envoi_notification where evenement_id=?",UUID.class,n);
        assertThat(distribution.reclamer(e)).isNotNull();
        mvc.perform(get("/api/v1/notifications/{id}",n).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        long v=jdbc.queryForObject("select version from confirmation_prise where id=?",Long.class,id);
        repondre(c,id,"IGNORER",null,v);
        assertThat(jdbc.queryForObject("select statut from envoi_notification where id=?",String.class,e)).isEqualTo("ANNULE");
        assertThat(distribution.reclamer(e)).isNull();
    }
    @Test void tachePlanifieeGenereLesRappelsSansClient() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");
        Object worker=context.getBean("traitementRappelWorker");worker.getClass().getMethod("traiter").invoke(worker);
        assertThat(nombre(occurrence(c,due))).isEqualTo(1);
    }
    @Test void ouvertureOccurrenceReserveeAuPatient() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");traiter(c,Instant.now());UUID id=occurrence(c,due);
        mvc.perform(get("/api/v1/mobile/me/prises/{id}",id).header("Authorization",bearer(c.token()))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/api/v1/mobile/me/prises/{id}",id).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
    }
    @Test void ancienCheminDeConfirmationInvalideAussiLeRappel() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");
        UUID dossier=jdbc.queryForObject("select id from dossier_suivi where patient_id=? and structure_id=?",UUID.class,c.patient(),structure);
        jdbc.update("update traitement set dossier_id=? where id=?",dossier,c.traitement());
        traiter(c,Instant.now());UUID id=occurrence(c,due);
        mvc.perform(patch("/api/v1/prises-medicament/confirmations/{id}/confirmation",id).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from notification where ressource_id=? and date_suppression is null",Integer.class,id)).isZero();
    }
    @Test void rappelInvalideNePeutPasEtreRestaureCommeSuppressionUtilisateur() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");traiter(c,Instant.now());UUID id=occurrence(c,due);
        UUID n=jdbc.queryForObject("select id from notification where ressource_id=?",UUID.class,id);
        long v=jdbc.queryForObject("select version from confirmation_prise where id=?",Long.class,id);
        repondre(c,id,"IGNORER",null,v);
        mvc.perform(post("/api/v1/compte/notifications/{id}/restauration",n).header("Authorization",bearer(c.token()))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from notification where id=? and date_suppression is null",Integer.class,n)).isZero();
    }
    @Test void rappelSupprimePuisInvalideNePeutPasEtreRestaure() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");traiter(c,Instant.now());UUID id=occurrence(c,due);
        UUID n=jdbc.queryForObject("select id from notification where ressource_id=?",UUID.class,id);
        mvc.perform(delete("/api/v1/compte/notifications/{id}",n).header("Authorization",bearer(c.token()))).andExpect(status().isNoContent());
        long v=jdbc.queryForObject("select version from confirmation_prise where id=?",Long.class,id);
        repondre(c,id,"IGNORER",null,v);
        mvc.perform(post("/api/v1/compte/notifications/{id}/restauration",n).header("Authorization",bearer(c.token()))).andExpect(status().isConflict());
    }
    @Test void reportDuneOccurrenceAncienneEstRappeleALaNouvelleEcheance() throws Exception {
        Instant due=Instant.parse("2026-10-26T20:00:00Z");var c=preparer("UTC",due,"QUOTIDIEN");
        tx.executeWithoutResult(s->{var p=em.find(PriseMedicament.class,c.prise());var o=new ConfirmationPrise(p,due);o.reporter(Instant.parse("2026-10-27T09:00:00Z"));em.persist(o);});
        assertThat(traiter(c,Instant.parse("2026-10-27T10:00:00Z"))).isEqualTo(1);
        assertThat(nombre(occurrence(c,due))).isEqualTo(1);
    }
    @Test void concurrenceNeDoublePasLesOccurrencesNiNotifications() throws Exception {
        Instant due=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(1);
        var c=preparer("UTC",due,"QUOTIDIEN");var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->{start.await();return traiter(c,due.plusSeconds(1));});var b=pool.submit(()->{start.await();return traiter(c,due.plusSeconds(1));});start.countDown();assertThat(a.get(30,TimeUnit.SECONDS)+b.get(30,TimeUnit.SECONDS)).isEqualTo(1);}finally{pool.shutdownNow();}
        assertThat(nombre(occurrence(c,due))).isEqualTo(1);
    }
}
