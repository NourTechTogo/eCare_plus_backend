package com.nourtech.ecareplus;
import com.fasterxml.jackson.databind.JsonNode;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TracabiliteAlerteIntegrationTests extends BackendFixture {
    record Cas(UUID patient,String token,UUID regle,JsonNode regleVue,UUID alerte){}
    JsonNode action(UUID id,String action)throws Exception{return body(mvc.perform(patch("/api/v1/regles-alerte/{id}/"+action,id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andReturn());}
    JsonNode mesure(String token,int valeur)throws Exception{return body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content(json.writeValueAsString(Map.of("typeMesure","POIDS","valeurPrincipale",valeur,"unite","kg","dateMesure",Instant.now().minusSeconds(1).toString(),"sourceRef",UUID.randomUUID().toString())))).andExpect(status().isCreated()).andReturn());}
    Cas preparer()throws Exception{
        UUID patient=patientId();String token=login();
        var regle=body(mvc.perform(post("/api/v1/regles-alerte").header("Authorization",bearer(doctorToken)).contentType("application/json").content(json.writeValueAsString(Map.of("nom","Regle fictive de recette","typeMesure","POIDS","operateur",">=","seuilMin",100,"nombreOccurrences",1,"periodeObservation",60,"niveauPriorite","ELEVE","patientId",patient.toString(),"unite","kg","composante","PRINCIPALE")))).andExpect(status().isCreated()).andReturn());
        UUID id=UUID.fromString(regle.path("id").asText());action(id,"validation");var active=action(id,"activation");
        var m=mesure(token,110);UUID alerte=jdbc.queryForObject("select id from alerte where regle_id=? and mesure_id=?",UUID.class,id,UUID.fromString(m.path("id").asText()));
        return new Cas(patient,token,id,active,alerte);
    }
    JsonNode detail(Cas c)throws Exception{return body(mvc.perform(get("/api/v1/portail/alertes/{id}",c.alerte()).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andReturn());}
    JsonNode modifier(Cas c)throws Exception{return body(mvc.perform(patch("/api/v1/regles-alerte/{id}",c.regle()).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"operateur\":\">=\",\"seuilMin\":130,\"nombreOccurrences\":1,\"periodeObservation\":60}")).andExpect(status().isOk()).andReturn());}

    @Test void ancienneAlerteGardeLeSeuilAppliqueMalgreModificationDeLaRegle()throws Exception{
        var c=preparer();assertThat(detail(c).path("seuilMin").decimalValue()).isEqualByComparingTo("100");
        modifier(c);
        var historique=detail(c);assertThat(historique.path("seuilMin").decimalValue()).isEqualByComparingTo("100");
        assertThat(historique.path("regleAppliquee").path("version").asLong(-1)).isEqualTo(c.regleVue().path("version").asLong());
        assertThat(historique.path("regleAppliquee").path("sourceValidation").asText()).isEqualTo("MEDECIN");
        assertThat(historique.path("regleAppliquee").path("validateurId").asText()).isEqualTo(doctor.toString());
    }
    @Test void modificationEffaceDateAuteurEtSourceEtImposeUneNouvelleValidation()throws Exception{
        var c=preparer();var modification=modifier(c);
        assertThat(modification.path("dateValidation").isNull()).isTrue();
        assertThat(modification.path("validateurId").isNull()).isTrue();
        assertThat(modification.path("sourceValidation").isNull()).isTrue();
        mvc.perform(patch("/api/v1/regles-alerte/{id}/activation",c.regle()).header("Authorization",bearer(doctorToken))).andExpect(status().isConflict());
        action(c.regle(),"validation");JsonNode active=action(c.regle(),"activation");JsonNode m=mesure(c.token(),140);
        UUID id=jdbc.queryForObject("select id from alerte where mesure_id=?",UUID.class,UUID.fromString(m.path("id").asText()));
        var nouvelle=body(mvc.perform(get("/api/v1/portail/alertes/{id}",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andReturn());
        assertThat(nouvelle.path("regleAppliquee").path("seuilMin").decimalValue()).isEqualByComparingTo("130");
        assertThat(nouvelle.path("regleAppliquee").path("version").asLong(-1)).isEqualTo(active.path("version").asLong());
    }
    @Test void patientAutonomeVoitOrigineDemonstrationSansPartageProfessionnel()throws Exception{
        UUID patient=patientId();String token=login();jdbc.update("delete from affectation_patient where patient_id=?",patient);
        UUID regle=tx.execute(s->{var r=new RegleAlerte("Exemple fictif personnel",null,TypeMesure.POIDS,">=",new BigDecimal("100"),null,1,60,NiveauPriorite.ELEVE);r.definirPortee(patient,null,"PRINCIPALE","kg");r.validerRegle(Instant.now());r.tracerValidation(null,"DEMONSTRATION");r.activerRegle();em.persist(r);return r.getId();});
        mesure(token,110);
        mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].regleAppliquee.sourceValidation").value("DEMONSTRATION"));
        UUID alerte=jdbc.queryForObject("select id from alerte where regle_id=?",UUID.class,regle);
        mvc.perform(get("/api/v1/portail/alertes/{id}",alerte).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
    }
    @Test void ancienneAlerteSansCaptureNePretendPasConnaitreLeSeuilHistorique()throws Exception{
        var c=preparer();
        jdbc.update("update alerte set capture_regle_version=null,capture_regle_nom=null,capture_regle_type_mesure=null,capture_regle_operateur=null,capture_regle_seuil_min=null,capture_regle_seuil_max=null,capture_regle_nombre_occurrences=null,capture_regle_periode_observation=null,capture_regle_niveau_priorite=null,capture_regle_composante=null,capture_regle_unite=null,capture_regle_contexte_mesure=null,capture_regle_date_validation=null,capture_regle_validateur_id=null,capture_regle_source_validation=null,capture_regle_actif=null where id=?",c.alerte());
        modifier(c);
        var historique=detail(c);assertThat(historique.path("regleAppliquee").isNull()).isTrue();
        assertThat(historique.path("seuilMin").isNull()).isTrue();assertThat(historique.path("operateur").isNull()).isTrue();
        assertThat(historique.path("titre").asText()).isEqualTo("Regle fictive de recette");
    }
    @Test void reponseGeneriqueExposeAussiLaCaptureSansChangerLesDroits()throws Exception{
        var c=preparer();mvc.perform(get("/api/v1/alertes/{id}",c.alerte()).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.regleAppliquee.seuilMin").value(100));
        mvc.perform(get("/api/v1/alertes/{id}",c.alerte()).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
    }
}
