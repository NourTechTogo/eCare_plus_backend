package com.nourtech.ecareplus.portail.application;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.*;
import com.nourtech.ecareplus.identity.infrastructure.persistence.*;
import com.nourtech.ecareplus.identity.application.service.*;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.care.infrastructure.persistence.*;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository;
import com.nourtech.ecareplus.organization.domain.entity.*;
import com.nourtech.ecareplus.organization.domain.enumeration.*;
import com.nourtech.ecareplus.organization.infrastructure.persistence.AffectationPatientRepository;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.infrastructure.persistence.AlerteRepository;
import com.nourtech.ecareplus.shared.application.exception.*;
import lombok.RequiredArgsConstructor;
import com.nourtech.ecareplus.audit.infrastructure.aop.JournaliserPortail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PortailService  {
    private final AutorisationRessources auth;
    private final PatientRepository patients;
    private final ProfessionnelSanteRepository professionnels;
    private final UtilisateurRepository users;
    private final DossierSuiviRepository dossiers;
    private final AffectationPatientRepository affectations;
    private final ConsentementRepository consentements;
    private final PathologieRepository pathologies;
    private final ObservationSuiviRepository observations;
    private final MesureSanteRepository mesures;
    private final AlerteRepository alertes;
    private final MotDePasseService passwords;
    private final ProfilValidation validation;
    private final com.nourtech.ecareplus.alert.infrastructure.persistence.RegleAlerteRepository regles;
    private final com.nourtech.ecareplus.care.application.service.DossierPersonnelService dossiersPersonnels;
    public Utilisateur acteur() {
        return auth.acteur();
    }
    public Patient patient(UUID id) {
        return patients.findById(id).orElseThrow(()->new RessourceIntrouvableException("Patient",id));
    }
    public Patient autoriser(UUID id,boolean clinique) {
        var p=patient(id);
        var u=acteur();
        require((u instanceof ProfessionnelSante || u instanceof AdministrateurStructure) && (!clinique || u instanceof ProfessionnelSante) && actif(u) && auth.patientAccessible(p,u,clinique));
        return p;
    }
    public Patient soi(UUID id,boolean clinique) {
        var u=acteur();
        require(u instanceof Patient && u.getId().equals(id));
        Patient p=patient(id);
        if(clinique) {
            require(consenti(p));
            // Personal follow-up does not depend on a professional assignment.
        }
        return p;
    }
    @JournaliserPortail("PATIENT")
    public Page<Map<String,Object>> patients(Pageable page) {
        var u=acteur();
        require(u instanceof ProfessionnelSante || u instanceof AdministrateurStructure);
        require(actif(u));
        UUID structureId=structure(u).getId();boolean clinique=u instanceof ProfessionnelSante;
        return patients.findAll((r,q,cb)->PorteePatientSql.accessible(r.get("id"),q,cb,structureId,clinique?u.getId():null,clinique),ordonner(page,Sort.by("nom","prenoms","id"))).map(p->vue(p,clinique));
    }
    @JournaliserPortail("PATIENT")
    public Map<String,Object> consulter(UUID id) {
        var u=acteur();
        return vue(autoriser(id,u instanceof ProfessionnelSante),u instanceof ProfessionnelSante);
    }
    public Page<Map<String,Object>> professionnels(Pageable p) {
        var u=acteur();
        var s=structure(u);
        return professionnels.findAllByStructureIdAndStatutCompteAndStatutProfessionnel(s.getId(),StatutCompte.ACTIF,"ACTIF",ordonner(p,Sort.by("nom","prenoms","id"))).map(this::proVue);
    }
    public List<Map<String,Object>> professionnelsSoi(UUID id) {
        var p=soi(id,false);
        return liens(p).stream().map(AffectationPatient::getProfessionnel).filter(this::actif).distinct().map(this::proVue).toList();
    }
    private Map<String,Object> proVue(ProfessionnelSante p) {
        return Map.of("id",p.getId(),"nom",p.getNom(),"prenoms",p.getPrenoms(),"specialite",p.getSpecialite());
    }
    private StructureSante structure(Utilisateur u) {
        require(actif(u));
        if(u instanceof AdministrateurStructure a)return a.getStructure();
        if(u instanceof ProfessionnelSante p)return p.getStructure();
        throw new AccessDeniedException("Structure requise");
    }
    private boolean actif(Utilisateur u) {
        return u.getStatutCompte()==StatutCompte.ACTIF && (!(u instanceof ProfessionnelSante p)||("ACTIF".equals(p.getStatutProfessionnel())&&p.getStructure().getStatut()==StatutStructure.ACTIVE)) && (!(u instanceof AdministrateurStructure a)||a.getStructure().getStatut()==StatutStructure.ACTIVE);
    }
    public boolean consenti(Patient p) {
        return consentements.findAllByPatientIdAndStatut(p.getId(),StatutConsentement.ACCORDE).stream().anyMatch(c->c.estValideA(Instant.now()));
    }
    private List<AffectationPatient> liens(Patient p) {
        LocalDate d=LocalDate.now(ZoneOffset.UTC);
        return affectations.findAllByPatientIdAndStatut(p.getId(),StatutAffectation.ACTIVE).stream().filter(a->!a.getDateDebut().isAfter(d)&&(a.getDateFin()==null||!a.getDateFin().isBefore(d))).sorted(Comparator.comparing(AffectationPatient::isProfessionnelPrincipal).reversed()).toList();
    }
    public DossierSuivi dossier(Patient p) {
        var u=acteur();
        UUID structureId;
        if (u instanceof Patient) {
            var lien = liens(p).stream().filter(a -> actif(a.getProfessionnel())).findFirst();
            // Preserve the assigned-patient path; a personal space is the fallback.
            if (lien.isEmpty()) return dossiersPersonnels.obtenir(p.getId());
            structureId = lien.get().getStructure().getId();
        } else {
            structureId = structure(u).getId();
        }
        return dossiers.findFirstByPatientIdAndStructureIdAndStatut(p.getId(),structureId,StatutDossier.ACTIF)
                .orElseGet(() -> {
                    if (u instanceof Patient) return dossiersPersonnels.obtenir(p.getId());
                    throw new RessourceIntrouvableException("Dossier actif",p.getId());
                });
    }
    public Map<String,Object> vue(Patient p,boolean clinique) {
        Map<String,Object> v=new LinkedHashMap<>();
        v.put("id",p.getId());
        v.put("numeroPatient",p.getNumeroPatient());
        v.put("nom",p.getNom());
        v.put("prenoms",p.getPrenoms());
        v.put("sexe",p.getSexe());
        v.put("dateNaissance",p.getDateNaissance());
        v.put("telephone",p.getTelephone());
        v.put("email",p.getEmail());
        v.put("languePreferee",p.getLanguePreferee());
        v.put("adresse",p.getAdresse());
        v.put("ville",p.getVille());
        v.put("pays",p.getPays());
        v.put("personneAContacter",p.getPersonneAContacter());
        v.put("telephoneUrgence",p.getTelephoneUrgence());
        v.put("groupeSanguin",clinique?p.getGroupeSanguin():null);
        v.put("tailleCm",clinique?p.getTailleCm():null);
        v.put("poidsKg",clinique?p.getPoidsKg():null);
        v.put("allergies",clinique?p.getAllergies():null);
        v.put("antecedents",clinique?p.getAntecedents():null);
        v.put("pathologie",clinique?p.getPathologie():null);
        var actor=acteur();
        var d=actor instanceof Patient?dossier(p):dossiers.findFirstByPatientIdAndStructureIdAndStatut(p.getId(),structure(actor).getId(),StatutDossier.ACTIF).orElse(null);
        v.put("dossierId",d==null?null:d.getId());
        v.put("structureId",d==null||d.getStructure()==null?null:d.getStructure().getId());
        var a=liens(p).stream().filter(l->actor instanceof Patient||l.getStructure().getId().equals(structure(actor).getId())).findFirst().orElse(null);
        v.put("professionnelId",a==null?null:a.getProfessionnel().getId());
        v.put("professionnelNom",a==null?null:nom(a.getProfessionnel()));
        v.put("dateCreation",p.getDateCreation());
        v.put("consentementAccepte",consenti(p));
        return v;
    }
    @JournaliserPortail("PATIENT")
    @Transactional public Map<String,Object> creer(Map<String,Object> r,String ip) {
        validation.verifier(r);
        var u=acteur();
        require(u instanceof AdministrateurStructure);
        var s=structure(u);
        String password=texte(r,"motDePasse",null);
        validerPassword(password);
        UUID proId=UUID.fromString(obligatoire(r,"professionnelId"));
        var pro=professionnels.findById(proId).orElseThrow(()->new RessourceIntrouvableException("Professionnel",proId));
        require(pro.getStructure().getId().equals(s.getId())&&actif(pro));
        obligatoire(r,"personneAContacter");
        obligatoire(r,"telephoneUrgence");
        String email=texte(r,"email",null),tel=texte(r,"telephone",null);
        if((email!=null&&users.existsByEmailIgnoreCase(email))||(tel!=null&&users.existsByTelephone(tel)))throw new ConflitMetierException("Email ou téléphone déjà utilisé");
        Patient p=new Patient("PAT-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT),obligatoire(r,"nom"),obligatoire(r,"prenoms"),Sexe.NON_RENSEIGNE,null,tel,email,passwords.hacher(password),texte(r,"languePreferee","fr"));
        mettreAJour(p,r,true);
        p.activerCompte();
        p.activerSuivi();
        patients.saveAndFlush(p);
        var d=dossiers.save(new DossierSuivi(p,s,"Selon prescription"));
        dossiersPersonnels.obtenir(p.getId());
        affectations.save(new AffectationPatient(p,pro,s,pro instanceof Medecin?RoleProfessionnel.MEDECIN_REFERENT:RoleProfessionnel.INFIRMIER_REFERENT,LocalDate.now(ZoneOffset.UTC),true));
        synchroniserPathologie(p,d);
        if(Boolean.TRUE.equals(r.get("consentementAccepte")))enregistrerConsentement(p,obligatoire(r,"versionDocument"),ip,"Consentement recueilli lors de l'admission par "+u.getId());
        return vue(p,false);
    }
    @JournaliserPortail("PATIENT")
    @Transactional public Map<String,Object> modifier(UUID id,Map<String,Object> r) {
        var u=acteur();
        var p=autoriser(id,u instanceof ProfessionnelSante);
        boolean clinique=u instanceof ProfessionnelSante;
        if(!clinique && java.util.stream.Stream.of("groupeSanguin","tailleCm","poidsKg","allergies","antecedents","pathologie").anyMatch(r::containsKey))throw new AccessDeniedException("Modification clinique réservée au professionnel affecté");
        mettreAJour(p,r,clinique);
        if(r.containsKey("professionnelId")) {
            require(u instanceof AdministrateurStructure);
            var s=structure(u);
            UUID proId=UUID.fromString(obligatoire(r,"professionnelId"));
            var pro=professionnels.findById(proId).orElseThrow(()->new RessourceIntrouvableException("Professionnel",proId));
            require(pro.getStructure().getId().equals(s.getId())&&actif(pro));
            var locaux = liens(p).stream()
                    .filter(a -> a.getStructure().getId().equals(s.getId())).toList();
            boolean dejaPrincipal = locaux.stream().anyMatch(a -> a.isProfessionnelPrincipal()
                    && a.getProfessionnel().getId().equals(proId));
            if (!dejaPrincipal) {
                locaux.stream().filter(AffectationPatient::isProfessionnelPrincipal)
                        .forEach(a -> a.terminer(LocalDate.now(ZoneOffset.UTC)));
                affectations.flush();
                var existante = locaux.stream().filter(a -> a.getProfessionnel().getId().equals(proId))
                        .findFirst();
                if (existante.isPresent()) {
                    existante.get().definirProfessionnelPrincipal(true);
                } else {
                    affectations.save(new AffectationPatient(p,pro,s,
                            pro instanceof Medecin?RoleProfessionnel.MEDECIN_REFERENT:RoleProfessionnel.INFIRMIER_REFERENT,
                            LocalDate.now(ZoneOffset.UTC),true));
                }
            }
        }
        if(clinique&&r.containsKey("pathologie"))synchroniserPathologie(p,dossier(p));
        return vue(p,clinique);
    }
    @JournaliserPortail("PATIENT")
    @Transactional public Map<String,Object> modifierSoi(UUID id,Map<String,Object> r) {
        var p=soi(id,false);
        mettreAJour(p,r,true);
        if (r.containsKey("pathologie")) {
            dossiers.findFirstByPatientIdAndStatutOrderByDateOuvertureDesc(id,StatutDossier.ACTIF)
                    .ifPresent(d -> synchroniserPathologie(p,d,false));
        }
        return vue(p,true);
    }
    public void identite(Utilisateur p,Map<String,Object> r) {
        validation.verifier(r);
        p.modifierProfil(texte(r,"nom",p.getNom()),texte(r,"prenoms",p.getPrenoms()),texte(r,"telephone",p.getTelephone()),texte(r,"email",p.getEmail()),texte(r,"languePreferee",p.getLanguePreferee()));
        p.modifierEtatCivil(r.containsKey("sexe")&&r.get("sexe")!=null?Sexe.valueOf(r.get("sexe").toString()):p.getSexe(),r.containsKey("dateNaissance")?(r.get("dateNaissance")==null?null:LocalDate.parse(r.get("dateNaissance").toString())):p.getDateNaissance());
    }
    private void mettreAJour(Patient p,Map<String,Object> r,boolean clinique) {
        identite(p,r);
        p.renseignerInformationsSuivi(clinique?texte(r,"groupeSanguin",p.getGroupeSanguin()):p.getGroupeSanguin(),texte(r,"adresse",p.getAdresse()),texte(r,"personneAContacter",p.getPersonneAContacter()),texte(r,"telephoneUrgence",p.getTelephoneUrgence()),p.getModeConnexionPrefere());
        p.completerProfil(texte(r,"ville",p.getVille()),texte(r,"pays",p.getPays()),clinique?nombre(r,"tailleCm",p.getTailleCm()):p.getTailleCm(),clinique?nombre(r,"poidsKg",p.getPoidsKg()):p.getPoidsKg(),clinique?texte(r,"allergies",p.getAllergies()):p.getAllergies(),clinique?texte(r,"antecedents",p.getAntecedents()):p.getAntecedents(),clinique?texte(r,"pathologie",p.getPathologie()):p.getPathologie());
    }
    private void synchroniserPathologie(Patient p,DossierSuivi d) {
        synchroniserPathologie(p,d,true);
    }
    private void synchroniserPathologie(Patient p,DossierSuivi d,boolean retirerAbsentes) {
        var anciennes=pathologies.findAllByDossierIdAndStatut(d.getId(),StatutPathologie.ACTIVE);
        Set<String> souhaitees = new LinkedHashSet<>();
        if (p.getPathologie()!=null) {
            if (p.getPathologie().contains("diabete")) souhaitees.add("diabete");
            if (p.getPathologie().contains("hypertension")) souhaitees.add("hypertension");
        }
        if (retirerAbsentes) {
            anciennes.stream().filter(x -> Set.of("diabete","hypertension").contains(Objects.toString(x.getCode(),"")))
                    .filter(x -> !souhaitees.contains(x.getCode())).forEach(Pathologie::desactiver);
        }
        for (String code : souhaitees) {
            if (anciennes.stream().noneMatch(x -> code.equals(x.getCode()))) {
                pathologies.save(new Pathologie(d,code.equals("diabete")?"Diabète":"Hypertension",code,null,null,null));
            }
        }
    }
    @JournaliserPortail("CONSENTEMENT")
    @Transactional public Map<String,Object> consentir(UUID id,String version,boolean cgu,boolean confidentialite,boolean accepte,String ip) {
        var p=soi(id,false);
        if(accepte) {
            if(!cgu||!confidentialite)throw new IllegalArgumentException("Veuillez lire les CGU et la confidentialité");
            enregistrerConsentement(p,version,ip,"CGU et confidentialité lues et acceptées personnellement");
        }
        else consentements.findAllByPatientIdAndStatut(id,StatutConsentement.ACCORDE).forEach(c->c.retirer(Instant.now()));
        return vue(p,true);
    }
    private void enregistrerConsentement(Patient p,String version,String ip,String description) {
        consentements.save(new Consentement(p,"SUIVI_MEDICAL",description,version,ip,Instant.now()));
    }
    @JournaliserPortail("OBSERVATION_SUIVI")
    public Page<Map<String,Object>> observations(UUID id,Pageable p) {
        Patient patient=autoriser(id,true);
        return observations.findAllByDossierIdAndArchiveeFalse(dossier(patient).getId(),ordonner(p,Sort.by(Sort.Order.desc("dateCreation"),Sort.Order.asc("id")))).map(this::observationVue);
    }
    @JournaliserPortail("OBSERVATION_SUIVI")
    @Transactional public Map<String,Object> observer(UUID id,String contenu) {
        var p=autoriser(id,true);
        return observationVue(observations.saveAndFlush(new ObservationSuivi(dossier(p),(ProfessionnelSante)acteur(),contenu,"SUIVI",false)));
    }
    private Map<String,Object> observationVue(ObservationSuivi o) {
        return Map.of("id",o.getId(),"contenu",o.getContenu(),"professionnelId",o.getProfessionnel().getId(),"professionnelNom",nom(o.getProfessionnel()),"dateCreation",o.getDateCreation());
    }
    @JournaliserPortail("MESURE_SANTE")
    public Page<Map<String,Object>> mesures(UUID id,boolean mobile,TypeMesure type,Instant from,Instant to,Pageable p) {
        if(mobile)soi(id,true);
        else autoriser(id,true);
        if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException("Période de mesures invalide");
        UUID structureId=mobile?null:structure(acteur()).getId();
        return mesures.findAll((r,q,cb)->{var conditions=new ArrayList<jakarta.persistence.criteria.Predicate>();conditions.add(cb.equal(r.get("patient").get("id"),id));if(structureId!=null)conditions.add(cb.equal(r.get("dossier").get("structure").get("id"),structureId));if(type!=null)conditions.add(cb.equal(r.get("typeMesure"),type));if(from!=null)conditions.add(cb.greaterThanOrEqualTo(r.get("dateMesure"),from));if(to!=null)conditions.add(cb.lessThanOrEqualTo(r.get("dateMesure"),to));return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));},ordonner(p,Sort.by(Sort.Order.desc("dateMesure"),Sort.Order.asc("id")))).map(PortailService::mesureVue);
    }
    public static Map<String,Object> mesureVue(MesureSante m) {
        Map<String,Object> v=new LinkedHashMap<>();
        v.put("id",m.getId());
        v.put("patientId",m.getPatient().getId());
        v.put("typeMesure",m.getTypeMesure());
        v.put("valeurPrincipale",m.getValeurPrincipale());
        v.put("valeurSecondaire",m.getValeurSecondaire());
        v.put("unite",m.getUnite());
        v.put("dateMesure",m.getDateMesure());
        v.put("contexteMesure",m.getContexteMesure());
        v.put("commentaire",m.getCommentaire());
        v.put("source",m.getSource());
        v.put("sourceRef",m.getSourceRef());
        v.put("valeurConfirmee",m.isValeurConfirmee());
        return v;
    }
    @JournaliserPortail("ALERTE")
    public Page<Map<String,Object>> alertes(UUID mobile,Pageable p) {
        var u=acteur();
        if(mobile!=null)soi(mobile,true);
        else require(u instanceof ProfessionnelSante&&actif(u));
        return chercherAlertes(mobile,null,false,p);
    }
    public Page<Map<String,Object>> alertesOuvertes(UUID patientId,Pageable page){autoriser(patientId,true);return chercherAlertes(null,patientId,true,page);}
    private Page<Map<String,Object>> chercherAlertes(UUID mobile,UUID patientId,boolean ouvertes,Pageable page){
        var u=acteur();UUID structureId=mobile==null?structure(u).getId():null;
        return alertes.findAll((r,q,cb)->{var conditions=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(mobile!=null)conditions.add(cb.equal(r.get("patient").get("id"),mobile));
            else{conditions.add(PorteePatientSql.accessible(r.get("patient").get("id"),q,cb,structureId,u.getId(),true));var m=r.join("mesure",jakarta.persistence.criteria.JoinType.LEFT);conditions.add(cb.or(cb.isNull(m.get("id")),cb.equal(m.join("dossier",jakarta.persistence.criteria.JoinType.LEFT).get("structure").get("id"),structureId)));}
            if(patientId!=null)conditions.add(cb.equal(r.get("patient").get("id"),patientId));if(ouvertes)conditions.add(cb.notEqual(r.get("statut"),com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte.CLOTUREE));
            if(q.getResultType()!=Long.class&&q.getResultType()!=long.class){var priorite=cb.<Integer>selectCase().when(cb.equal(r.get("niveauPriorite"),com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.CRITIQUE),4).when(cb.equal(r.get("niveauPriorite"),com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE),3).when(cb.equal(r.get("niveauPriorite"),com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.MOYEN),2).otherwise(1);q.orderBy(cb.desc(priorite),cb.desc(r.get("dateCreation")),cb.asc(r.get("id")));}
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));},page.isPaged()?PageRequest.of(page.getPageNumber(),Math.min(100,page.getPageSize())):PageRequest.of(0,100)).map(this::alerteVue);
    }
    @JournaliserPortail("ALERTE")
    public Map<String,Object> alerte(UUID id) {
        var a=alertes.findById(id).orElseThrow(()->new RessourceIntrouvableException("Alerte",id));
        autoriser(a.getPatient().getId(),true);
        require(a.getMesure()==null||(a.getMesure().getDossier().getStructure()!=null&&a.getMesure().getDossier().getStructure().getId().equals(structure(acteur()).getId())));
        return alerteVue(a);
    }
    @JournaliserPortail("ALERTE")
    @Transactional public Map<String,Object> cloturer(UUID id,String motif) {
        var a=alertes.findById(id).orElseThrow(()->new RessourceIntrouvableException("Alerte",id));
        autoriser(a.getPatient().getId(),true);
        require(a.getMesure()==null||(a.getMesure().getDossier().getStructure()!=null&&a.getMesure().getDossier().getStructure().getId().equals(structure(acteur()).getId())));
        a.cloturer(motif,Instant.now(),(ProfessionnelSante)acteur());
        return alerteVue(a);
    }
    private Map<String,Object> alerteVue(Alerte a) {
        Map<String,Object> v=new LinkedHashMap<>();
        v.put("id",a.getId());
        v.put("patientId",a.getPatient().getId());
        v.put("patientNom",nom(a.getPatient()));
        v.put("numeroPatient",a.getPatient().getNumeroPatient());
        var m=a.getMesure();
        v.put("typeMesure",m==null?null:m.getTypeMesure());
        v.put("valeurPrincipale",m==null?null:m.getValeurPrincipale());
        v.put("valeurSecondaire",m==null?null:m.getValeurSecondaire());
        v.put("unite",m==null?null:m.getUnite());
        var capture=a.getRegleAppliquee();
        v.put("regleAppliquee",com.nourtech.ecareplus.alert.api.dto.response.RegleAppliqueeVue.depuis(capture));
        v.put("seuil",capture==null?null:capture.getSeuilMin());
        v.put("seuilMin",capture==null?null:capture.getSeuilMin());
        v.put("seuilMax",capture==null?null:capture.getSeuilMax());
        v.put("operateur",capture==null?null:capture.getOperateur());
        v.put("composante",capture==null?null:capture.getComposante());
        v.put("niveauPriorite",a.getNiveauPriorite());
        v.put("gravite",a.getNiveauPriorite()==com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.CRITIQUE?"CRITIQUE":"ATTENTION");
        v.put("statutSuivi",switch(a.getStatut()){case NOUVELLE -> "NOUVELLE";case CLOTUREE -> "TRAITEE";default -> "VUE";});
        v.put("statut",a.getStatut());
        v.put("titre",a.getTitre());
        v.put("description",a.getDescription());
        v.put("dateCreation",a.getDateCreation());
        v.put("dateCloture",a.getDateCloture());
        v.put("motifCloture",a.getMotifCloture());
        v.put("professionnelNom",a.getProfessionnel()==null?null:nom(a.getProfessionnel()));
        v.put("auteurClotureId",a.getClotureePar()==null?null:a.getClotureePar().getId());
        v.put("auteurClotureNom",a.getClotureePar()==null?null:nom(a.getClotureePar()));
        return v;
    }
    public static <T> Page<T> page(List<T> values,Pageable p) {
        if(p.isUnpaged())return new PageImpl<>(values);
        int start=(int)Math.min(p.getOffset(),values.size());
        return new PageImpl<>(values.subList(start,Math.min(start+p.getPageSize(),values.size())),p,values.size());
    }
    private static Pageable ordonner(Pageable page,Sort tri){return PageRequest.of(page.isPaged()?page.getPageNumber():0,page.isPaged()?Math.min(100,page.getPageSize()):100,tri);}
    public List<Map<String,Object>> seuils(UUID patientId)  {
        autoriser(patientId,true);
        UUID structureId=structure(acteur()).getId();
        var applicable=regles.findAll().stream().filter(r->r.isActif()
        && (r.getPatientId()==null||r.getPatientId().equals(patientId))
        && (r.getStructureId()==null||r.getStructureId().equals(structureId))).toList();
        return applicable.stream().filter(r->r.getPatientId()!=null || applicable.stream().noneMatch(other->other.getPatientId()!=null
        && other.getTypeMesure()==r.getTypeMesure() && other.getComposante().equals(r.getComposante())
        && Objects.equals(other.getContexteMesure(),r.getContexteMesure())))
        .map(r-> {
            Map<String,Object> v=new LinkedHashMap<>();v.put("id",r.getId());v.put("typeMesure",r.getTypeMesure());
            v.put("composante",r.getComposante());v.put("unite",r.getUnite());v.put("operateur",r.getOperateur());
            v.put("seuilMin",r.getSeuilMin());v.put("seuilMax",r.getSeuilMax());v.put("niveauPriorite",r.getNiveauPriorite());
            v.put("patientId",r.getPatientId());v.put("contexteMesure",r.getContexteMesure());return v;
        }
        ).toList();
    }
    public static String nom(Utilisateur u) {
        return u.getPrenoms()+" "+u.getNom();
    }
    public static String texte(Map<String,Object> r,String k,String def) {
        if(!r.containsKey(k))return def;
        Object v=r.get(k);
        return v==null||v.toString().isBlank()?null:v.toString().trim();
    }
    private static Double nombre(Map<String,Object> r,String k,Double def) {
        if(!r.containsKey(k))return def;
        return r.get(k)==null?null:Double.valueOf(r.get(k).toString());
    }
    private static String obligatoire(Map<String,Object> r,String k) {
        String v=texte(r,k,null);
        if(v==null)throw new IllegalArgumentException(k+" est obligatoire");
        return v;
    }
    public static void validerPassword(String p) {
        if(p==null||p.length()<12||p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72||!p.matches("(?s)(?=.*[A-Za-z])(?=.*[0-9]).+"))throw new IllegalArgumentException("Mot de passe : 12 caractères minimum, une lettre et un chiffre, 72 octets maximum");
    }
    private static void require(boolean b) {
        if(!b)throw new AccessDeniedException("Accès refusé : affectation, structure ou consentement requis");
    }
}


