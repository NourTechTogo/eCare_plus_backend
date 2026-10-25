package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.*;
import com.nourtech.ecareplus.auth.infrastructure.security.JwtService;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real requests and committed transactions: no Mockito or bypassed security filters. */
@SpringBootTest @AutoConfigureMockMvc
abstract class BackendFixture {
 @Autowired MockMvc mvc;@Autowired EntityManager em;@Autowired TransactionTemplate tx;
 @Autowired JdbcTemplate jdbc;@Autowired JwtService jwt;@Autowired MotDePasseService passwords;
 final ObjectMapper json=new ObjectMapper();
 String adminToken,doctorToken,otherToken,suffix;UUID doctor,other,structure;
 final String password="SecretIntegration2026";
 @BeforeEach void seed(){tx.executeWithoutResult(status->{suffix=UUID.randomUUID().toString().substring(0,8);
  var s=new StructureSante("Centre "+suffix,"CLINIQUE","Rue","Lomé","Maritime",suffix,null,"AUT-"+suffix);s.activer();em.persist(s);structure=s.getId();
  var a=new AdministrateurStructure("Admin","Test",Sexe.NON_RENSEIGNE,null,"ADM-"+suffix,"admin-"+suffix+"@example.test",passwords.hacher(password),"fr","Direction","STRUCTURE",s);a.activerCompte();em.persist(a);
  var d=doctor(s,"D1-"+suffix);var o=doctor(s,"D2-"+suffix);doctor=d.getId();other=o.getId();
  adminToken=jwt.emettre(a).jeton();doctorToken=jwt.emettre(d).jeton();otherToken=jwt.emettre(o).jeton();
 });}
 Medecin doctor(StructureSante s,String n){var d=new Medecin(n,n,"Médecin",n,Sexe.NON_RENSEIGNE,null,n,n+"@example.test",passwords.hacher(password),"fr","Médecine","Suivi",LocalDate.now(),s);d.activerCompte();em.persist(d);return d;}
 JsonNode create(boolean consent)throws Exception{return body(mvc.perform(post("/api/v1/portail/patients").header("Authorization",bearer(adminToken)).contentType("application/json").content(patientJson(consent,doctor))).andExpect(status().isCreated()).andExpect(jsonPath("$.dossierId").isNotEmpty()).andExpect(jsonPath("$.motDePasse").doesNotExist()).andExpect(jsonPath("$.allergies").isEmpty()).andReturn());}
 String patientJson(boolean consent,UUID pro)throws Exception{Map<String,Object> r=new LinkedHashMap<>();r.put("nom","Patient");r.put("prenoms","Test");r.put("sexe","FEMININ");r.put("dateNaissance","1990-02-03");r.put("telephone","PAT-"+suffix);r.put("email","patient-"+suffix+"@example.test");r.put("motDePasse",password);r.put("personneAContacter","Proche");r.put("telephoneUrgence","URG-"+suffix);r.put("professionnelId",pro.toString());r.put("ville","Lomé");r.put("tailleCm",167);r.put("poidsKg",67);r.put("allergies","Pénicilline");r.put("pathologie","diabete_hypertension");r.put("consentementAccepte",consent);r.put("versionDocument","2026-1");return json.writeValueAsString(r);}
 String login()throws Exception{return body(mvc.perform(post("/api/v1/auth/connexion").contentType("application/json").content(json.writeValueAsString(Map.of("identifiant","patient-"+suffix+"@example.test","motDePasse",password)))).andExpect(status().isOk()).andReturn()).path("jeton").asText();}

 JsonNode body(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
 String bearer(String token){return "Bearer "+token;}
 UUID patientId() throws Exception { return UUID.fromString(create(true).path("id").asText()); }
}
