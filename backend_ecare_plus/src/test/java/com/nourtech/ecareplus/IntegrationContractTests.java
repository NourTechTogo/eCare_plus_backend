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
class IntegrationContractTests {
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
 @Test void parcoursCompletPersistantEtSessionRevoquee()throws Exception{
  var p=create(false);String patient=p.path("id").asText();String token=login();
  mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.ville").value("Lomé")).andExpect(jsonPath("$.allergies").value("Pénicilline")).andExpect(jsonPath("$.consentementAccepte").value(false));
  mvc.perform(get("/api/v1/mobile/me/mesures").header("Authorization",bearer(token))).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization",bearer(token)).contentType("application/json").content("{\"versionDocument\":\"2026-1\",\"cguLues\":true,\"confidentialiteLue\":true,\"accepte\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.consentementAccepte").value(true));
  mvc.perform(get("/api/v1/mobile/me/professionnels").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  String measure="{\"typeMesure\":\"GLYCEMIE\",\"valeurPrincipale\":1.1,\"unite\":\"g/L\",\"dateMesure\":\""+Instant.now().minusSeconds(5)+"\",\"sourceRef\":\"app-entry-1\"}";
  var m=body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content(measure)).andExpect(status().isCreated()).andExpect(jsonPath("$.valeurPrincipale").value(110)).andExpect(jsonPath("$.unite").value("mg/dL")).andReturn());
  mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content(measure)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(m.path("id").asText()));
  mvc.perform(get("/api/v1/portail/patients/{id}/mesures",patient).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(post("/api/v1/portail/patients/{id}/observations",patient).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"contenu\":\"Contrôle à prévoir\",\"professionnelId\":\""+other+"\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.professionnelId").value(doctor.toString()));
  var c=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"patientId\":\""+patient+"\"}")).andExpect(status().isCreated()).andReturn());String conversation=c.path("id").asText();
  mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token)).contentType("application/json").content("{\"contenu\":\"Bonjour docteur\"}")).andExpect(status().isCreated());
  mvc.perform(get("/api/v1/mobile/conversations").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$[0].nonLus").value(1)).andExpect(jsonPath("$[0].dernierMessage.contenu").value("Bonjour docteur"));
  mvc.perform(post("/api/v1/mobile/conversations/{id}/lecture",conversation).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
  mvc.perform(post("/api/v1/auth/deconnexion").header("Authorization",bearer(token))).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(token))).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(login()))).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select count(*) from pathologie where dossier_id = ?",Integer.class,UUID.fromString(p.path("dossierId").asText()))).isEqualTo(2);
 }
 @Test void medecinNonAffecteEtAdminSansDroitsCliniques()throws Exception{var p=create(true);String id=p.path("id").asText();mvc.perform(get("/api/v1/portail/patients/{id}",id).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());mvc.perform(get("/api/v1/portail/patients/{id}/mesures",id).header("Authorization",bearer(adminToken))).andExpect(status().isForbidden());mvc.perform(get("/api/v1/portail/patients/{id}",id).header("Authorization",bearer(adminToken))).andExpect(status().isOk()).andExpect(jsonPath("$.tailleCm").isEmpty());mvc.perform(get("/api/v1/portail/patients").header("Authorization",bearer(otherToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(otherToken)).contentType("application/json").content("{\"patientId\":\""+id+"\"}")).andExpect(status().isForbidden());}
 @Test void mauvaisProfessionnelAnnuleTouteAdmission()throws Exception{long before=jdbc.queryForObject("select count(*) from patient",Long.class);mvc.perform(post("/api/v1/portail/patients").header("Authorization",bearer(adminToken)).contentType("application/json").content(patientJson(true,UUID.randomUUID()))).andExpect(status().isNotFound());assertThat(jdbc.queryForObject("select count(*) from patient",Long.class)).isEqualTo(before);}
 @Test void lectureDesDeuxDocumentsRequiseEtRetraitImmediat()throws Exception{var p=create(false);String t=login();mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization",bearer(t)).contentType("application/json").content("{\"versionDocument\":\"v1\",\"cguLues\":true,\"confidentialiteLue\":false,\"accepte\":true}")).andExpect(status().isBadRequest());mvc.perform(get("/api/v1/portail/patients/{id}/observations",p.path("id").asText()).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());}
 @Test void comptePatchEtPasswordInvalidentAnciennesSessions()throws Exception{create(true);String first=login(),second=login();mvc.perform(patch("/api/v1/compte").header("Authorization",bearer(first)).contentType("application/json").content("{\"ville\":\"Kara\",\"tailleCm\":168}")).andExpect(status().isOk()).andExpect(jsonPath("$.ville").value("Kara"));mvc.perform(post("/api/v1/compte/mot-de-passe").header("Authorization",bearer(first)).contentType("application/json").content("{\"motDePasseActuel\":\""+password+"\",\"nouveauMotDePasse\":\"SecretModifie2026\"}")).andExpect(status().isNoContent());mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(first))).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(second))).andExpect(status().isUnauthorized());}
 @Test void profilProEtPatientOntAccesAuCompteEtMotDePasseFaibleRefuse()throws Exception{mvc.perform(get("/api/v1/compte").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.specialite").value("Médecine"));mvc.perform(post("/api/v1/auth/inscription").contentType("application/json").content("{\"nom\":\"A\",\"prenoms\":\"B\",\"email\":\"weak@example.test\",\"telephone\":\"123\",\"motDePasse\":\"abcdefghijklmnop\"}")).andExpect(status().isBadRequest());}
 JsonNode body(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
 @Test void diastoliqueDeclencheUneSeuleAlerteEtCloturePersistante() throws Exception {
  var p=create(true);
  UUID patientId=UUID.fromString(p.path("id").asText());
  tx.executeWithoutResult(status->{
   var r=new com.nourtech.ecareplus.alert.domain.entity.RegleAlerte("Règle test diastolique", "Test seulement",
           com.nourtech.ecareplus.care.domain.enumeration.TypeMesure.TENSION_ARTERIELLE, ">=", new java.math.BigDecimal("100"),null,1,60,
           com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE);
   r.definirPortee(patientId,structure,"DIASTOLIQUE","mmHg");
   r.validerRegle(Instant.now());r.activerRegle();em.persist(r);
  });
  String token=login();
  String payload="{\"typeMesure\":\"TENSION_ARTERIELLE\",\"valeurPrincipale\":120,\"valeurSecondaire\":110,\"unite\":\"mmHg\",\"dateMesure\":\""+Instant.now().minusSeconds(3)+"\",\"sourceRef\":\"tension-entry-1\"}";
  var m=body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content(payload)).andExpect(status().isCreated()).andReturn());
  mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content(payload)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(m.path("id").asText()));
  mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content(payload.replace("\"valeurSecondaire\":110","\"valeurSecondaire\":111"))).andExpect(status().isConflict());
  var a=body(mvc.perform(get("/api/v1/portail/alertes").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].composante").value("DIASTOLIQUE")).andExpect(jsonPath("$.content[0].seuil").value(100)).andReturn());
  mvc.perform(get("/api/v1/portail/patients/{id}/seuils",patientId).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/api/v1/mobile/me/notifications").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  String alerte=a.path("content").get(0).path("id").asText();
  mvc.perform(post("/api/v1/portail/alertes/{id}/cloture",alerte).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"motifCloture\":\"Contact patient et contrôle programmé\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("CLOTUREE"));
  mvc.perform(get("/api/v1/portail/alertes/{id}",alerte).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.motifCloture").value("Contact patient et contrôle programmé"));
  assertThat(jdbc.queryForObject("select count(*) from alerte where patient_id = ?",Integer.class,patientId)).isEqualTo(1);
  assertThat(jdbc.queryForObject("select count(*) from mesure_sante where patient_id = ?",Integer.class,patientId)).isEqualTo(1);
 }
 @Test void profilMapValideTypesEmailLongueursEtDatesSansMutation()throws Exception {
  create(true);String token=login();
  for(String input:List.of("{\"nom\":null}","{\"dateNaissance\":\"date-invalide\"}","{\"dateNaissance\":\"2099-01-01\"}","{\"email\":\"pas-email\"}","{\"poidsKg\":\"66\"}","{\"ville\":{\"objet\":true}}",json.writeValueAsString(Map.of("nom","X".repeat(101))))) {
   mvc.perform(patch("/api/v1/mobile/me").header("Authorization",bearer(token)).contentType("application/json").content(input)).andExpect(status().isBadRequest());
  }
  mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.nom").value("Patient")).andExpect(jsonPath("$.poidsKg").value(67));
 }
 String bearer(String t){return "Bearer "+t;}
 @Test void facadeJournalisePrincipalRessourceEtSuccesSeulementApresCommit() throws Exception {
  UUID patientId=UUID.fromString(create(true).path("id").asText());
  UUID adminId=UUID.fromString(jwt.decoder().decode(adminToken).getSubject());
  verifierJournal(adminId,"CREER_PATIENT","PATIENT",patientId,"SUCCES",1);
  String contenu="INFORMATION_MEDICALE_NE_PAS_JOURNALISER";
  mvc.perform(patch("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content(json.writeValueAsString(Map.of("allergies",contenu))))
      .andExpect(status().isOk());
  verifierJournal(doctor,"MODIFIER_PATIENT","PATIENT",patientId,"SUCCES",1);
  mvc.perform(get("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(doctorToken)))
      .andExpect(status().isOk());
  verifierJournal(doctor,"CONSULTER_PATIENT","PATIENT",patientId,"SUCCES",1);
  JsonNode note=body(mvc.perform(post("/api/v1/portail/patients/{id}/observations",patientId)
      .header("Authorization",bearer(doctorToken)).contentType("application/json")
      .content(json.writeValueAsString(Map.of("contenu",contenu)))).andExpect(status().isCreated()).andReturn());
  verifierJournal(doctor,"OBSERVER_OBSERVATION_SUIVI","OBSERVATION_SUIVI",UUID.fromString(note.path("id").asText()),"SUCCES",1);
  UUID alerte=tx.execute(status->{
   var a=new com.nourtech.ecareplus.alert.domain.entity.Alerte(em.find(Patient.class,patientId),null,null,
       com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte.AUTRE,"Test audit",null,
       com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.FAIBLE);
   em.persist(a);return a.getId();
  });
  mvc.perform(post("/api/v1/portail/alertes/{id}/cloture",alerte).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content(json.writeValueAsString(Map.of("motifCloture",contenu))))
      .andExpect(status().isOk());
  verifierJournal(doctor,"CLOTURER_ALERTE","ALERTE",alerte,"SUCCES",1);
  mvc.perform(patch("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content("{\"nom\":null}"))
      .andExpect(status().isBadRequest());
  verifierJournal(doctor,"MODIFIER_PATIENT","PATIENT",patientId,"SUCCES",1);
  verifierJournal(doctor,"MODIFIER_PATIENT","PATIENT",patientId,"ECHEC",1);
  mvc.perform(patch("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content(json.writeValueAsString(Map.of("email","D2-"+suffix+"@example.test"))))
      .andExpect(status().isConflict());
  verifierJournal(doctor,"MODIFIER_PATIENT","PATIENT",patientId,"SUCCES",1);
  assertThat(jdbc.queryForList("select * from journal_activite where utilisateur_id = ?",doctor).toString()).doesNotContain(contenu);
 }
 void verifierJournal(UUID user,String action,String ressource,UUID id,String resultat,int count) {
  assertThat(jdbc.queryForObject("select count(*) from journal_activite where utilisateur_id=? and action=? and ressource=? and ressource_id=? and resultat=?",
      Integer.class,user,action,ressource,id,resultat)).isEqualTo(count);
 }
 @Test void clotureEnregistreLeVraiAuteurSansConfondreLeProfessionnelAffecte() throws Exception {
  UUID patientId=UUID.fromString(create(true).path("id").asText());
  UUID alerteId=tx.execute(status->{
   var patient=em.find(Patient.class,patientId);
   var owner=em.find(Medecin.class,doctor);
   var closer=em.find(Medecin.class,other);
   em.persist(new com.nourtech.ecareplus.organization.domain.entity.AffectationPatient(patient,closer,owner.getStructure(),
       com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel.MEDECIN_SECONDAIRE,LocalDate.now(),false));
   var a=new com.nourtech.ecareplus.alert.domain.entity.Alerte(patient,null,null,
       com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte.AUTRE,"À vérifier","Test auteur",
       com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.MOYEN);
   a.affecter(owner);em.persist(a);return a.getId();
  });
  mvc.perform(post("/api/v1/portail/alertes/{id}/cloture",alerteId).header("Authorization",bearer(otherToken))
      .contentType("application/json").content("{\"motifCloture\":\"Patient contacté\",\"auteurClotureId\":\""+doctor+"\"}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.auteurClotureId").value(other.toString()))
      .andExpect(jsonPath("$.auteurClotureNom").value("D2-"+suffix+" Médecin"))
      .andExpect(jsonPath("$.professionnelNom").value("D1-"+suffix+" Médecin"));
  mvc.perform(post("/api/v1/portail/alertes/{id}/cloture",alerteId).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content("{\"motifCloture\":\"Tentative de remplacement\"}"))
      .andExpect(status().isConflict());
  mvc.perform(get("/api/v1/portail/alertes/{id}",alerteId).header("Authorization",bearer(doctorToken)))
      .andExpect(status().isOk()).andExpect(jsonPath("$.auteurClotureId").value(other.toString()));
  assertThat(jdbc.queryForObject("select cloturee_par_id from alerte where id=?",UUID.class,alerteId)).isEqualTo(other);
  mvc.perform(patch("/api/v1/alertes/{id}/reouverture",alerteId).header("Authorization",bearer(doctorToken)))
      .andExpect(status().isOk());
  mvc.perform(get("/api/v1/portail/alertes/{id}",alerteId).header("Authorization",bearer(doctorToken)))
      .andExpect(status().isOk()).andExpect(jsonPath("$.auteurClotureId").isEmpty());
  mvc.perform(patch("/api/v1/alertes/{id}/cloture",alerteId).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content("{\"motifCloture\":\"Nouvelle vérification\"}"))
      .andExpect(status().isOk());
  mvc.perform(get("/api/v1/portail/alertes/{id}",alerteId).header("Authorization",bearer(otherToken)))
      .andExpect(status().isOk()).andExpect(jsonPath("$.auteurClotureId").value(doctor.toString()));
 }
 @Test void pathologieExistanteConservePreuvesEtPatientNePeutPasLaDesactiver() throws Exception {
  var patient=create(true);UUID patientId=UUID.fromString(patient.path("id").asText());
  UUID dossierId=UUID.fromString(patient.path("dossierId").asText());
  UUID clinicalId=tx.execute(status->{
   em.createQuery("delete from Pathologie p where p.dossier.id=:dossier").setParameter("dossier",dossierId).executeUpdate();
   var d=em.find(com.nourtech.ecareplus.care.domain.entity.DossierSuivi.class,dossierId);
   var known=new com.nourtech.ecareplus.care.domain.entity.Pathologie(d,"Diabète confirmé","diabete","Diagnostic documenté",
       LocalDate.of(2020,3,4),com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque.ELEVE);
   em.persist(known);return known.getId();
  });
  mvc.perform(patch("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(doctorToken))
      .contentType("application/json").content("{\"pathologie\":\"diabete_hypertension\"}"))
      .andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select count(*) from pathologie where dossier_id=? and code='diabete'",Integer.class,dossierId)).isEqualTo(1);
  assertThat(jdbc.queryForObject("select date_diagnostic_declaree from pathologie where id=?",LocalDate.class,clinicalId)).isEqualTo(LocalDate.of(2020,3,4));
  assertThat(jdbc.queryForObject("select niveau_risque from pathologie where id=?",String.class,clinicalId)).isEqualTo("ELEVE");
  assertThat(jdbc.queryForObject("select description from pathologie where id=?",String.class,clinicalId)).isEqualTo("Diagnostic documenté");
  mvc.perform(patch("/api/v1/mobile/me").header("Authorization",bearer(login()))
      .contentType("application/json").content("{\"pathologie\":\"hypertension\"}"))
      .andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select statut from pathologie where id=?",String.class,clinicalId)).isEqualTo("ACTIVE");
  assertThat(jdbc.queryForObject("select count(*) from pathologie where dossier_id=? and statut='ACTIVE'",Integer.class,dossierId)).isEqualTo(2);
 }
 @Test void reaffectationNeModifiePasUnAutreTenantEtMemeReferentEstIdempotent() throws Exception {
  UUID patientId=UUID.fromString(create(true).path("id").asText());
  UUID foreignAssignment=tx.execute(status->{
   var s=new StructureSante("Autre centre "+suffix,"CLINIQUE","Rue","Kara","Kara","B-"+suffix,null,"AUT-B-"+suffix);
   s.activer();em.persist(s);
   var d=doctor(s,"B-"+suffix);
   var link=new com.nourtech.ecareplus.organization.domain.entity.AffectationPatient(
           em.find(Patient.class,patientId),d,s,
           com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel.MEDECIN_REFERENT,LocalDate.now(),true);
   em.persist(link);return link.getId();
  });
  for(int i=0;i<2;i++) {
   mvc.perform(patch("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(adminToken))
       .contentType("application/json").content(json.writeValueAsString(Map.of("professionnelId",doctor.toString()))))
       .andExpect(status().isOk()).andExpect(jsonPath("$.professionnelId").value(doctor.toString()));
  }
  assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id = ?",Integer.class,patientId)).isEqualTo(2);
  mvc.perform(patch("/api/v1/portail/patients/{id}",patientId).header("Authorization",bearer(adminToken))
      .contentType("application/json").content(json.writeValueAsString(Map.of("professionnelId",other.toString()))))
      .andExpect(status().isOk()).andExpect(jsonPath("$.professionnelId").value(other.toString()));
  assertThat(jdbc.queryForObject("select statut from affectation_patient where id = ?",String.class,foreignAssignment)).isEqualTo("ACTIVE");
  assertThat(jdbc.queryForObject("select professionnel_principal from affectation_patient where id = ?",Boolean.class,foreignAssignment)).isTrue();
  assertThat(jdbc.queryForObject("select statut from affectation_patient where patient_id = ? and professionnel_id = ?",String.class,patientId,doctor)).isEqualTo("TERMINEE");
  assertThat(jdbc.queryForObject("select count(*) from affectation_patient where patient_id = ? and statut='ACTIVE'",Integer.class,patientId)).isEqualTo(2);
 }
}
