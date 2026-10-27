package com.nourtech.ecareplus.organization.application.service;
import com.nourtech.ecareplus.organization.domain.entity.*;
import com.nourtech.ecareplus.organization.domain.enumeration.*;
import com.nourtech.ecareplus.organization.infrastructure.persistence.AffectationPatientRepository;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.portail.application.PortailService;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import java.security.SecureRandom;
@Service @RequiredArgsConstructor @Transactional
public class ReferencementPatientService {
    private final PortailService portail;private final EntityManager em;private final DossierSuiviRepository dossiers;private final AffectationPatientRepository affectations;private final AffectationPatientService affectationService;
    @Value("${app.referencement.duree:PT10M}") private Duration duree;
    public record JetonVue(String jeton,Instant expireLe){}
    public record ReferenceCommande(String jeton,UUID professionnelId,Long versionDossier){}
    public record ResolutionVue(UUID patientId,String nom,String prenoms,LocalDate dateNaissance,UUID dossierId,Long versionDossier){}
    public JetonVue generer(){
        var acteur=portail.acteur();if(!(acteur instanceof Patient))throw new AccessDeniedException("QR réservé au patient");
        var patient=em.find(Patient.class,acteur.getId(),LockModeType.PESSIMISTIC_WRITE);
        em.createQuery("select j from JetonReferencement j where j.patientId=:id",JetonReferencement.class).setParameter("id",patient.getId()).getResultList().forEach(JetonReferencement::revoquer);
        byte[] aleatoire=new byte[48];new SecureRandom().nextBytes(aleatoire);String secret=Base64.getUrlEncoder().withoutPadding().encodeToString(aleatoire);
        var expiration=Instant.now().plus(duree);em.persist(new JetonReferencement(patient.getId(),Empreintes.sha256(secret),expiration));return new JetonVue(secret,expiration);
    }
    public ResolutionVue resoudre(String secret){administrateur();var jeton=jeton(secret);var patient=em.find(Patient.class,jeton.getPatientId());if(patient==null||patient.getStatutCompte()!=StatutCompte.ACTIF)throw new ConflitMetierException("Compte patient indisponible");
        var dossier=dossierCompatible(patient,administrateur().getStructure());return new ResolutionVue(patient.getId(),patient.getNom(),patient.getPrenoms(),patient.getDateNaissance(),dossier==null?null:dossier.getId(),dossier==null?null:dossier.getVersion());}
    public Map<String,Object> referencer(ReferenceCommande commande){
        var admin=administrateur();var initial=jeton(commande.jeton());
        var patient=em.find(Patient.class,initial.getPatientId(),LockModeType.PESSIMISTIC_WRITE);em.refresh(initial,LockModeType.PESSIMISTIC_WRITE);
        if(!initial.valide()||patient.getStatutCompte()!=StatutCompte.ACTIF)throw new ConflitMetierException("Jeton ou compte indisponible");
        var pro=commande.professionnelId()==null?null:em.find(ProfessionnelSante.class,commande.professionnelId());
        if(pro==null||!pro.getStructure().getId().equals(admin.getStructure().getId())||pro.getStatutCompte()!=StatutCompte.ACTIF||!"ACTIF".equals(pro.getStatutProfessionnel()))throw new AccessDeniedException("Professionnel actif de la structure requis");
        var dossier=dossierCompatible(patient,admin.getStructure());
        if(dossier!=null&&(commande.versionDossier()==null||commande.versionDossier()!=dossier.getVersion()))throw new ConflitMetierException("Version de dossier incompatible");
        if(dossier==null){if(commande.versionDossier()!=null)throw new ConflitMetierException("Aucun dossier à cette version");dossier=new DossierSuivi(patient,admin.getStructure(),null);em.persist(dossier);}
        var principaux=affectations.findAllByPatientIdAndStatut(patient.getId(),StatutAffectation.ACTIVE).stream().filter(a->a.isProfessionnelPrincipal()&&a.getStructure().getId().equals(admin.getStructure().getId())).toList();
        if(principaux.stream().anyMatch(a->!a.getProfessionnel().getId().equals(pro.getId())))throw new ConflitMetierException("Un référent existe : utiliser le transfert explicite");
        if(principaux.isEmpty())affectationService.affecter(patient.getId(),pro.getId(),admin.getStructure().getId(),RoleProfessionnel.MEDECIN_REFERENT,LocalDate.now(ZoneOffset.UTC),true);
        patient.activerSuivi();initial.consommer();em.flush();return portail.consulter(patient.getId());
    }
    private AdministrateurStructure administrateur(){var acteur=portail.acteur();if(!(acteur instanceof AdministrateurStructure admin)||admin.getStructure().getStatut()!=StatutStructure.ACTIVE)throw new AccessDeniedException("Référencement réservé à l'établissement actif");return admin;}
    private JetonReferencement jeton(String secret){
        if(secret==null||secret.length()<43||secret.length()>256)throw new ConflitMetierException("Jeton invalide");
        var liste=em.createQuery("select j from JetonReferencement j where j.empreinte=:hash",JetonReferencement.class).setParameter("hash",Empreintes.sha256(secret)).getResultList();
        if(liste.isEmpty()||!liste.getFirst().valide())throw new ConflitMetierException("Jeton expiré, révoqué ou déjà utilisé");return liste.getFirst();
    }
    private DossierSuivi dossierCompatible(Patient patient,StructureSante structure){var actifs=dossiers.findAllByPatientId(patient.getId()).stream().filter(d->d.getStatut()==StatutDossier.ACTIF&&d.getStructure()!=null).toList();
        if(actifs.size()>1||actifs.stream().anyMatch(d->!d.getStructure().getId().equals(structure.getId())))throw new ConflitMetierException("Dossier préexistant : rapprochement humain requis");return actifs.isEmpty()?null:actifs.getFirst();}
}
