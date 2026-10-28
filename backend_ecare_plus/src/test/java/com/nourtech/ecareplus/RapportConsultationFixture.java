package com.nourtech.ecareplus;
import com.fasterxml.jackson.databind.JsonNode;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class RapportConsultationFixture extends BackendFixture {
    static final String API="/api/v1/mobile/me/rapports-consultation";
    record Compte(UUID id,String token){}
    Compte compte(boolean autonome)throws Exception{
        suffix=UUID.randomUUID().toString().substring(0,8);UUID id=patientId();String token=login();
        if(autonome)jdbc.update("delete from affectation_patient where patient_id=?",id);
        return new Compte(id,token);
    }
    DossierSuivi dossier(Compte c){return em.createQuery("select d from DossierSuivi d where d.patient.id=:id and d.structure is null",DossierSuivi.class).setParameter("id",c.id()).getSingleResult();}
    void mesure(Compte c,TypeMesure type,String value,String secondaire,String unite,String date){tx.executeWithoutResult(s->em.persist(new MesureSante(em.find(Patient.class,c.id()),dossier(c),type,new BigDecimal(value),secondaire==null?null:new BigDecimal(secondaire),unite,Instant.parse(date),null,null,SourceMesure.PATIENT,"SYNCHRONISEE")));}
    Map<String,Object> commande(){return new LinkedHashMap<>(Map.of("sourceRef",UUID.randomUUID().toString(),"du","2026-10-20","au","2026-10-22","fuseau","UTC"));}
    JsonNode generer(Compte c,Map<String,Object> q)throws Exception{return body(mvc.perform(post(API).header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(q))).andExpect(status().isCreated()).andReturn());}
    JsonNode lire(Compte c,String id)throws Exception{return body(mvc.perform(get(API+"/{id}",id).header("Authorization",bearer(c.token()))).andExpect(status().isOk()).andReturn());}
    JsonNode serie(JsonNode rapport,String type,String composante){for(var n:rapport.path("seriesMesures"))if(n.path("typeMesure").asText().equals(type)&&n.path("composante").asText().equals(composante))return n;throw new AssertionError("Serie absente");}

}
