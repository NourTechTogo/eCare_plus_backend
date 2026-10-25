package com.nourtech.ecareplus.sync.application.service;
import com.nourtech.ecareplus.sync.application.service.OperationSynchronisationService.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
@Service @RequiredArgsConstructor
public class TraitementLotSynchronisationService {
    private final OperationSynchronisationService operations;private final AccesPatientService acces;
    public record LotSynchronisationCommande(String identifiantAppareil,List<ElementCommande> elements){}
    public record LotSynchronisationResultat(List<ElementResultat> elements){}
    public LotSynchronisationResultat traiter(UUID acteur,LotSynchronisationCommande commande){
        acces.verifier(acteur,acteur);
        if(commande.identifiantAppareil()==null||commande.identifiantAppareil().isBlank()||commande.identifiantAppareil().length()>128||commande.elements()==null||commande.elements().isEmpty()||commande.elements().size()>100)throw new IllegalArgumentException("Appareil requis et lot de 1 à 100 éléments attendu");
        var resultats=new ArrayList<ElementResultat>();
        for(var element:commande.elements()){
            if(element==null){resultats.add(new ElementResultat(null,null,"REJETE",null,null,"ELEMENT_INVALIDE","Élément absent"));continue;}
            try{resultats.add(operations.traiter(acteur,commande.identifiantAppareil(),element));}
            catch(ConflitMetierException|IllegalStateException|org.springframework.dao.OptimisticLockingFailureException|org.springframework.dao.DataIntegrityViolationException e){resultats.add(new ElementResultat(element.identifiantLocal(),element.typeOperation(),"CONFLIT",null,null,"CONFLIT","Opération incompatible : recharger les données"));}
            catch(com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException|org.springframework.security.access.AccessDeniedException e){resultats.add(new ElementResultat(element.identifiantLocal(),element.typeOperation(),"REJETE",null,null,"RESSOURCE_INACCESSIBLE","Ressource indisponible pour cet élément"));}
            catch(IllegalArgumentException e){resultats.add(new ElementResultat(element.identifiantLocal(),element.typeOperation(),"REJETE",null,null,"DONNEES_INVALIDES",e.getMessage()));}
        }return new LotSynchronisationResultat(List.copyOf(resultats));
    }
}
