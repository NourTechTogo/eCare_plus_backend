package com.nourtech.ecareplus.calling.application.service;
import com.nourtech.ecareplus.calling.application.port.FournisseurAppel;
import com.nourtech.ecareplus.calling.application.service.AppelTransactions.AppelVue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class AppelService {
    private final AppelTransactions transactions;private final FournisseurAppel fournisseur;
    public record CreationAppel(String type,String sourceRef){}
    public AppelVue creer(UUID acteur,UUID conversation,CreationAppel commande){
        if(!fournisseur.configure())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Appels non configurés ; utilisez la messagerie");
        var preparation=transactions.preparer(acteur,conversation,commande.type(),commande.sourceRef());
        if(!preparation.creer())return preparation.vue();
        try{fournisseur.creerSalle(preparation.salle());return transactions.pret(preparation.vue().id());}
        catch(RuntimeException erreur){transactions.echec(preparation.vue().id());throw erreur;}
    }
    public AppelVue action(UUID acteur,UUID id,String action){transactions.action(acteur,id,action);fermer(id);return transactions.consulter(acteur,id);}
    public FournisseurAppel.AccesAppel connexion(UUID acteur,UUID id){return transactions.connexion(acteur,id);}
    public Page<AppelVue> historique(UUID acteur,UUID conversation,Pageable page){return transactions.historique(acteur,conversation,page);}
    public void entretenir(){if(!fournisseur.configure())return;for(var id:transactions.entretenir())fermer(id);}
    @org.springframework.transaction.event.TransactionalEventListener
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void sessionTerminee(com.nourtech.ecareplus.auth.application.service.SessionService.SessionTerminee event){
        for(var id:transactions.terminerUtilisateur(event.utilisateurId()))fermer(id);
    }
    private void fermer(UUID id){var salle=transactions.fermeture(id);if(salle==null)return;try{fournisseur.fermerSalle(salle.salle(),salle.initiateur(),salle.destinataire());transactions.confirmerFermeture(id);}catch(RuntimeException indisponible){/* Persistent pending closure retried by maintenance. */}}
}
