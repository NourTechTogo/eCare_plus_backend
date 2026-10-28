package com.nourtech.ecareplus.sync.application.service;
import com.nourtech.ecareplus.sync.domain.entity.OperationSynchronisation;
import com.nourtech.ecareplus.care.application.service.*;
import com.nourtech.ecareplus.care.application.command.EnregistrerMesureCommand;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.mobile.api.dto.CreerMesureMobileRequest;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.portail.application.PortailService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import jakarta.persistence.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor
public class OperationSynchronisationService {
    private final EntityManager em;private final ObjectMapper json;private final Validator validation;private final AccesPatientService acces;private final PortailService portail;private final com.nourtech.ecareplus.care.infrastructure.persistence.MesureSanteRepository mesuresRepository;private final MesureSanteService mesures;private final ObservanceService observance;
    private final OperationPersonnelleSynchronisationService personnelles;
    public record ElementCommande(String identifiantLocal,String typeOperation,int versionSchema,Instant dateCreationLocale,Map<String,Object> contenu){}
    public record ElementResultat(String identifiantLocal,String typeOperation,String statut,UUID ressourceId,Long versionServeur,String codeErreur,String message){}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public ElementResultat traiter(UUID acteur,String appareil,ElementCommande element){
        var patient=acces.verifier(acteur,acteur);em.find(Patient.class,acteur,LockModeType.PESSIMISTIC_WRITE);
        if(element.identifiantLocal()==null||element.identifiantLocal().isBlank()||element.identifiantLocal().length()>128||element.typeOperation()==null||element.typeOperation().length()>40||element.contenu()==null||element.dateCreationLocale()==null||element.dateCreationLocale().isAfter(Instant.now().plusSeconds(300)))throw new IllegalArgumentException("Identité ou horodatage de synchronisation invalide");
        if(element.versionSchema()!=1)throw new IllegalArgumentException("Version de schéma non prise en charge");
        String canonique=json.writeValueAsString(Empreintes.canonique(element.contenu()));
        if(canonique.length()>16000)throw new IllegalArgumentException("Contenu de synchronisation trop volumineux");
        String hash=Empreintes.sha256(element.versionSchema()+":"+element.dateCreationLocale()+":"+canonique);
        var precedents=em.createQuery("select o from OperationSynchronisation o where o.utilisateurId=:u and o.identifiantAppareil=:a and o.typeOperation=:t and o.identifiantLocal=:l",OperationSynchronisation.class).setParameter("u",acteur).setParameter("a",appareil).setParameter("t",element.typeOperation()).setParameter("l",element.identifiantLocal()).getResultList();
        if(!precedents.isEmpty()){var precedent=precedents.getFirst();if(!precedent.getEmpreinte().equals(hash))throw new ConflitMetierException("Identifiant local réutilisé avec un autre contenu");return json.readValue(precedent.getResultat(),ElementResultat.class);}
        if(element.contenu().containsKey("patientId")||element.contenu().containsKey("dossierId")||element.contenu().containsKey("auteurId"))throw new IllegalArgumentException("Propriétaire imposé par la session");
        String reference="sync:"+Empreintes.sha256(appareil+":"+element.typeOperation()+":"+element.identifiantLocal());
        UUID ressource;long version;
        switch(element.typeOperation()){
            case "CREER_MESURE" -> {
                var corps=new LinkedHashMap<>(element.contenu());String origine=String.valueOf(corps.remove("origine"));
                if(!Set.of("null","PATIENT","APPAREIL_CONNECTE").contains(origine))throw new IllegalArgumentException("Provenance inconnue");
                var requete=json.convertValue(corps,CreerMesureMobileRequest.class);if(!validation.validate(requete).isEmpty())throw new IllegalArgumentException("Mesure incomplète ou invalide");
                var mesure=mesures.enregistrer(new EnregistrerMesureCommand(mesuresRepository.findByPatientIdAndSourceRef(acteur,reference).map(m->m.getDossier().getId()).orElseGet(()->portail.dossier(patient).getId()),acteur,requete.typeMesure(),requete.valeurPrincipale(),requete.valeurSecondaire(),requete.unite(),requete.dateMesure(),requete.contexteMesure(),requete.commentaire(),"APPAREIL_CONNECTE".equals(origine)?SourceMesure.APPAREIL_CONNECTE:SourceMesure.PATIENT,"SYNCHRONISEE",reference));
                em.flush();ressource=mesure.getId();version=mesure.getVersion();
            }
            case "REPONDRE_PRISE" -> {
                var corps=new LinkedHashMap<>(element.contenu());Object occurrenceId=corps.remove("occurrenceId");
                if(occurrenceId==null)throw new IllegalArgumentException("Occurrence requise");corps.put("sourceRef",reference);
                var resultat=observance.confirmer(acteur,UUID.fromString(occurrenceId.toString()),json.convertValue(corps,ObservanceService.ReponsePriseCommande.class));ressource=resultat.id();version=resultat.version();
            }
            case "CREER_RENDEZ_VOUS", "MODIFIER_RENDEZ_VOUS", "ANNULER_RENDEZ_VOUS", "DECLARER_TRAITEMENT" -> {
                var resultat=personnelles.traiter(element.typeOperation(),element.contenu());
                em.flush();ressource=resultat.ressourceId();version=resultat.version();
            }
            default -> throw new IllegalArgumentException("Type d'opération non pris en charge");
        }
        var resultat=new ElementResultat(element.identifiantLocal(),element.typeOperation(),"SUCCES",ressource,version,null,null);
        em.persist(new OperationSynchronisation(acteur,appareil,element.typeOperation(),element.identifiantLocal(),hash,json.writeValueAsString(resultat)));return resultat;
    }
}
