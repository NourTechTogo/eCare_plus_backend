package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ApiControllerIntegrationTests {

    // Contract tests isolate authorization; SecurityRegressionTests exercises real security and transactions.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources autorisations;

    @org.junit.jupiter.api.BeforeEach
    void conserverLesReponsesDesTestsDeContrat() {
        org.mockito.Mockito.when(autorisations.filtrer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> call.getArgument(1));
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void creationPatientRetourne201SansExposerLeMotDePasse() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson("PAT-API-001", "patient.api1@example.test")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/patients/")))
                .andExpect(jsonPath("$.numeroPatient").value("PAT-API-001"))
                .andExpect(jsonPath("$.utilisateur.email").value("patient.api1@example.test"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist())
                .andExpect(jsonPath("$.motDePasseHash").doesNotExist())
                .andExpect(jsonPath("$.utilisateur.motDePasseHash").doesNotExist());
    }

    @Test
    void validationInvalideRetourneUnProblemDetail400() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroPatient": "",
                                  "nom": "",
                                  "prenoms": "",
                                  "sexe": "MASCULIN",
                                  "motDePasse": "court",
                                  "languePreferee": "fr"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"))
                .andExpect(jsonPath("$.erreurs").isArray());
    }

    @Test
    void patientInconnuRetourne404() throws Exception {
        mockMvc.perform(get("/api/v1/patients/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    void numeroPatientDupliqueRetourne409() throws Exception {
        String corps = patientJson("PAT-API-002", "patient.api2@example.test");
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit métier"));
    }

    @Test
    void parcoursCompletSuiviMedicalViaApi() throws Exception {
        UUID structureId = creer("/api/v1/structures", """
                {
                  "nom": "Centre API Test",
                  "typeStructure": "CENTRE_MEDICAL",
                  "adresse": "1 avenue des Tests",
                  "ville": "Lomé",
                  "region": "Maritime",
                  "telephone": "+22822000099",
                  "email": "centre.api@example.test",
                  "numeroAutorisation": "AUT-API-001"
                }
                """, "$.id");

        UUID patientId = creer("/api/v1/patients", """
                {
                  "numeroPatient": "PAT-API-100",
                  "nom": "Test",
                  "prenoms": "Suivi Complet",
                  "sexe": "FEMININ",
                  "dateNaissance": "1988-05-12",
                  "telephone": "+22890000100",
                  "email": "patient.suivi.api@example.test",
                  "motDePasse": "MotDePasseTresSolide!2026",
                  "languePreferee": "fr"
                }
                """, "$.utilisateur.id");

        UUID medecinId = creer("/api/v1/professionnels-sante/medecins", """
                {
                  "numeroProfessionnel": "PRO-API-100",
                  "numeroOrdre": "ORDRE-API-100",
                  "nom": "Mensah",
                  "prenoms": "Koffi API",
                  "sexe": "MASCULIN",
                  "dateNaissance": "1975-04-02",
                  "telephone": "+22890000101",
                  "email": "medecin.api@example.test",
                  "motDePasse": "MotDePasseTresSolide!2026",
                  "languePreferee": "fr",
                  "specialite": "Médecine interne",
                  "domaineSpecialisation": "Diabétologie",
                  "datePriseFonction": "2020-01-01",
                  "structureId": "%s"
                }
                """.formatted(structureId), "$.professionnel.utilisateur.id");

        UUID dossierId = creer("/api/v1/dossiers-suivi", """
                {
                  "patientId": "%s",
                  "structureId": "%s",
                  "frequenceSuivi": "MENSUELLE"
                }
                """.formatted(patientId, structureId), "$.id");

        creer("/api/v1/dossiers-suivi/%s/pathologies".formatted(dossierId), """
                {
                  "nom": "Diabète de type 2",
                  "code": "E11",
                  "description": "Diagnostic déclaré par le patient",
                  "dateDiagnosticDeclaree": "2024-01-15",
                  "niveauRisque": "MODERE"
                }
                """, "$.id");

        UUID mesureId = creer("/api/v1/dossiers-suivi/%s/mesures".formatted(dossierId), """
                {
                  "patientId": "%s",
                  "typeMesure": "TENSION_ARTERIELLE",
                  "valeurPrincipale": 125,
                  "valeurSecondaire": 80,
                  "unite": "mmHg",
                  "dateMesure": "2026-01-10T08:00:00Z",
                  "contexteMesure": "Au repos",
                  "source": "PATIENT"
                }
                """.formatted(patientId), "$.id");

        UUID traitementId = creer("/api/v1/dossiers-suivi/%s/traitements".formatted(dossierId), """
                {
                  "libelle": "Metformine 500mg",
                  "description": "Traitement de fond",
                  "dateDebut": "2026-01-01",
                  "prescripteur": "Dr Mensah",
                  "consignesGenerales": "Prendre au cours des repas"
                }
                """, "$.id");

        UUID medicamentId = creer("/api/v1/medicaments", """
                {
                  "nomCommercial": "Glucophage",
                  "denomination": "Metformine",
                  "forme": "Comprimé",
                  "dosage": "500",
                  "uniteDosage": "mg"
                }
                """, "$.id");

        UUID priseId = creer("/api/v1/traitements/%s/prises".formatted(traitementId), """
                {
                  "medicamentId": "%s",
                  "quantite": 1,
                  "unite": "comprimé",
                  "frequence": "2 fois par jour",
                  "heurePrevue": "08:00:00",
                  "momentPrise": "MATIN",
                  "voieAdministration": "Orale"
                }
                """.formatted(medicamentId), "$.id");

        creer("/api/v1/prises-medicament/%s/confirmations".formatted(priseId), """
                {
                  "datePrevue": "2026-01-11T08:00:00Z"
                }
                """, "$.id");

        creer("/api/v1/declarations-symptomes", """
                {
                  "patientId": "%s",
                  "symptome": "Vertiges",
                  "intensite": "MODEREE",
                  "description": "Vertiges en fin de journée",
                  "persistant": false
                }
                """.formatted(patientId), "$.id");

        creer("/api/v1/dossiers-suivi/%s/observations".formatted(dossierId), """
                {
                  "professionnelId": "%s",
                  "contenu": "Patient observant bien son traitement",
                  "typeObservation": "SUIVI",
                  "confidentielle": false
                }
                """.formatted(medecinId), "$.id");

        creer("/api/v1/dossiers-suivi/%s/rapports".formatted(dossierId), """
                {
                  "titre": "Bilan trimestriel",
                  "dateDebut": "2026-01-01",
                  "dateFin": "2026-03-31",
                  "resume": "Evolution stable",
                  "format": "PDF"
                }
                """, "$.id");

        UUID regleId = creer("/api/v1/regles-alerte", """
                {
                  "nom": "Hyperglycémie répétée",
                  "description": "Détecte une glycémie élevée répétée",
                  "typeMesure": "GLYCEMIE",
                  "operateur": ">",
                  "seuilMin": 180,
                  "nombreOccurrences": 2,
                  "periodeObservation": 24,
                  "niveauPriorite": "ELEVE"
                }
                """, "$.id");

        UUID alerteId = creer("/api/v1/alertes", """
                {
                  "patientId": "%s",
                  "mesureId": "%s",
                  "typeAlerte": "AUTRE",
                  "titre": "Tension élevée détectée",
                  "description": "Mesure au-dessus du seuil configuré",
                  "niveauPriorite": "ELEVE"
                }
                """.formatted(patientId, mesureId), "$.id");

        mockMvc.perform(get("/api/v1/alertes/%s".formatted(alerteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origine").value("MANUELLE"));

        creer("/api/v1/alertes/%s/interventions".formatted(alerteId), """
                {
                  "typeIntervention": "Appel téléphonique",
                  "description": "Contact du patient",
                  "dateIntervention": "2026-01-12T09:00:00Z"
                }
                """, "$.id");

        UUID rappelId = creer("/api/v1/rappels", """
                {
                  "patientId": "%s",
                  "priseMedicamentId": "%s",
                  "typeRappel": "PRISE_MEDICAMENT",
                  "titre": "Prise de Metformine",
                  "message": "N'oubliez pas votre comprimé du matin",
                  "dateHeurePrevue": "2026-01-13T08:00:00Z",
                  "frequence": "QUOTIDIEN",
                  "canal": "SMS"
                }
                """.formatted(patientId, priseId), "$.id");

        UUID rendezVousId = creer("/api/v1/rendez-vous", """
                {
                  "patientId": "%s",
                  "motif": "Consultation de suivi",
                  "dateHeure": "2026-02-01T09:00:00Z",
                  "lieu": "Centre API Test",
                  "typeRendezVous": "CONSULTATION"
                }
                """.formatted(patientId), "$.id");

        creer("/api/v1/notifications", """
                {
                  "utilisateurId": "%s",
                  "titre": "Nouveau rapport disponible",
                  "message": "Votre bilan trimestriel est prêt",
                  "typeNotification": "RAPPORT",
                  "canal": "EMAIL",
                  "sensible": false
                }
                """.formatted(patientId), "$.id");

        UUID contenuId = creer("/api/v1/contenus-educatifs", """
                {
                  "auteurId": "%s",
                  "titre": "Bien vivre avec le diabète",
                  "resume": "Conseils pratiques",
                  "contenu": "Contenu détaillé sur l'alimentation",
                  "typeContenu": "ARTICLE",
                  "langue": "fr",
                  "pathologieCible": "DIABETE"
                }
                """.formatted(medecinId), "$.id");

        // Consultation (GET) : le patient créé au début de ce parcours doit être relisible.
        mockMvc.perform(get("/api/v1/rappels/%s".formatted(rappelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PLANIFIE"));

        // Transition (PATCH) : le cycle de vie du contenu éducatif jusqu'à la publication.
        mockMvc.perform(patch("/api/v1/contenus-educatifs/%s/validation".formatted(contenuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutValidation").value("VALIDE"));

        mockMvc.perform(patch("/api/v1/contenus-educatifs/%s/publication".formatted(contenuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutValidation").value("PUBLIE"))
                .andExpect(jsonPath("$.datePublication").isNotEmpty());

        mockMvc.perform(patch("/api/v1/rendez-vous/%s/confirmation".formatted(rendezVousId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONFIRME"));
    }

    @Test
    void refuseUneTensionArterielleSansValeurDiastoliqueViaApi() throws Exception {
        UUID structureId = creer("/api/v1/structures", """
                {
                  "nom": "Centre API Test 400",
                  "typeStructure": "CENTRE_MEDICAL",
                  "adresse": "2 avenue des Tests",
                  "ville": "Lomé",
                  "region": "Maritime",
                  "telephone": "+22822000098",
                  "email": "centre.api400@example.test",
                  "numeroAutorisation": "AUT-API-400"
                }
                """, "$.id");
        UUID patientId = creer("/api/v1/patients", """
                {
                  "numeroPatient": "PAT-API-400",
                  "nom": "Test",
                  "prenoms": "Erreur Mesure",
                  "sexe": "MASCULIN",
                  "dateNaissance": "1990-01-01",
                  "telephone": "+22890000400",
                  "email": "patient.erreur400@example.test",
                  "motDePasse": "MotDePasseTresSolide!2026",
                  "languePreferee": "fr"
                }
                """, "$.utilisateur.id");
        UUID dossierId = creer("/api/v1/dossiers-suivi", """
                {
                  "patientId": "%s",
                  "structureId": "%s",
                  "frequenceSuivi": "MENSUELLE"
                }
                """.formatted(patientId, structureId), "$.id");

        mockMvc.perform(post("/api/v1/dossiers-suivi/%s/mesures".formatted(dossierId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "typeMesure": "TENSION_ARTERIELLE",
                                  "valeurPrincipale": 125,
                                  "unite": "mmHg",
                                  "dateMesure": "2026-01-10T08:00:00Z",
                                  "source": "PATIENT"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.detail").value("La valeur diastolique est obligatoire pour une tension artérielle"));
    }

    @Test
    void validationInvalideSurUneRegleAlerteRetourneUnProblemDetail400() throws Exception {
        mockMvc.perform(post("/api/v1/regles-alerte")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "",
                                  "typeMesure": "GLYCEMIE",
                                  "operateur": ">",
                                  "seuilMin": 180,
                                  "nombreOccurrences": 2,
                                  "periodeObservation": 24,
                                  "niveauPriorite": "ELEVE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"))
                .andExpect(jsonPath("$.erreurs").isArray());
    }

    @Test
    void alerteInconnueRetourne404ViaApi() throws Exception {
        mockMvc.perform(get("/api/v1/alertes/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    void activerUneRegleAlerteNonValideeRetourne409ViaApi() throws Exception {
        UUID regleId = creer("/api/v1/regles-alerte", """
                {
                  "nom": "Règle non validée",
                  "typeMesure": "POIDS",
                  "operateur": ">",
                  "seuilMin": 120,
                  "nombreOccurrences": 1,
                  "periodeObservation": 24,
                  "niveauPriorite": "FAIBLE"
                }
                """, "$.id");

        mockMvc.perform(patch("/api/v1/regles-alerte/%s/activation".formatted(regleId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit métier"))
                .andExpect(jsonPath("$.detail").value("La règle doit être validée avant activation"));
    }

    @Test
    void genereUneAlerteAutomatiqueQuandLeSeuilRepeteEstAtteint() throws Exception {
        UUID structureId = creer("/api/v1/structures", """
                {
                  "nom": "Centre Alertes Automatiques",
                  "typeStructure": "CENTRE_MEDICAL",
                  "adresse": "3 avenue des Alertes",
                  "ville": "Lomé",
                  "region": "Maritime",
                  "telephone": "+22822000701",
                  "email": "alertes.auto@example.test",
                  "numeroAutorisation": "AUT-ALERTE-701"
                }
                """, "$.id");
        UUID patientId = creer("/api/v1/patients", patientJson(
                "PAT-ALERTE-701", "patient.alerte701@example.test"
        ), "$.utilisateur.id");
        UUID dossierId = creer("/api/v1/dossiers-suivi", """
                {
                  "patientId": "%s",
                  "structureId": "%s",
                  "frequenceSuivi": "MENSUELLE"
                }
                """.formatted(patientId, structureId), "$.id");
        UUID regleId = creer("/api/v1/regles-alerte", """
                {
                  "nom": "Hyperglycémie répétée automatique",
                  "description": "Deux mesures supérieures à 180 sur 24 heures",
                  "typeMesure": "GLYCEMIE",
                  "operateur": ">",
                  "seuilMin": 180,
                  "nombreOccurrences": 2,
                  "periodeObservation": 1440,
                  "niveauPriorite": "ELEVE"
                }
                """, "$.id");

        mockMvc.perform(patch("/api/v1/regles-alerte/%s/validation".formatted(regleId)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/regles-alerte/%s/activation".formatted(regleId)))
                .andExpect(status().isOk());

        creer("/api/v1/dossiers-suivi/%s/mesures".formatted(dossierId), mesureGlycemieJson(
                patientId, "2026-09-29T08:00:00Z", 190
        ), "$.id");
        mockMvc.perform(get("/api/v1/alertes/patients/%s".formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        UUID secondeMesureId = creer("/api/v1/dossiers-suivi/%s/mesures".formatted(dossierId), mesureGlycemieJson(
                patientId, "2026-09-29T09:00:00Z", 200
        ), "$.id");
        mockMvc.perform(get("/api/v1/alertes/patients/%s".formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origine").value("AUTOMATIQUE"))
                .andExpect(jsonPath("$[0].regleId").value(regleId.toString()))
                .andExpect(jsonPath("$[0].mesureId").value(secondeMesureId.toString()));

        mockMvc.perform(post("/api/v1/alertes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": "%s",
                                  "mesureId": "%s",
                                  "regleId": "%s",
                                  "typeAlerte": "SEUIL_MESURE",
                                  "titre": "Doublon manuel",
                                  "niveauPriorite": "ELEVE"
                                }
                                """.formatted(patientId, secondeMesureId, regleId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit métier"));
    }

    private UUID creer(String uri, String corps, String cheminId) throws Exception {
        MvcResult resultat = mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated())
                .andReturn();
        String champ = cheminId.startsWith("$.") ? cheminId.substring(2) : cheminId;
        var noeud = objectMapper.readTree(resultat.getResponse().getContentAsString());
        for (String segment : champ.split("\\.")) {
            noeud = noeud.get(segment);
        }
        return UUID.fromString(noeud.asText());
    }

    private static String patientJson(String numeroPatient, String email) {
        return """
                {
                  "numeroPatient": "%s",
                  "nom": "Test",
                  "prenoms": "Patient API",
                  "sexe": "NON_RENSEIGNE",
                  "dateNaissance": "1990-01-01",
                  "telephone": "+221700000001",
                  "email": "%s",
                  "motDePasse": "MotDePasseTresSolide!2026",
                  "languePreferee": "fr"
                }
                """.formatted(numeroPatient, email);
    }

    private static String mesureGlycemieJson(UUID patientId, String dateMesure, int valeur) {
        return """
                {
                  "patientId": "%s",
                  "typeMesure": "GLYCEMIE",
                  "valeurPrincipale": %d,
                  "unite": "mg/dL",
                  "dateMesure": "%s",
                  "source": "PATIENT"
                }
                """.formatted(patientId, valeur, dateMesure);
    }
}
