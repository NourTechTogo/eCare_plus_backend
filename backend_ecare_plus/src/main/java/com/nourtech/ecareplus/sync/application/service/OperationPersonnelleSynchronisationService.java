package com.nourtech.ecareplus.sync.application.service;

import com.nourtech.ecareplus.care.domain.entity.Traitement;
import com.nourtech.ecareplus.mobile.api.dto.AgendaPersonnelRequest;
import com.nourtech.ecareplus.mobile.api.dto.DeclarationTraitementRequest;
import com.nourtech.ecareplus.mobile.application.service.AgendaPersonnelService;
import com.nourtech.ecareplus.mobile.application.service.DeclarationTraitementService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

/** Adapter called inside the per-element transaction; business rules stay in their existing services. */
@Service @RequiredArgsConstructor
public class OperationPersonnelleSynchronisationService {
    private final AgendaPersonnelService agenda;
    private final DeclarationTraitementService declarations;
    private final EntityManager em;
    private final ObjectMapper json;
    private final Validator validation;
    public record Resultat(UUID ressourceId,long version){}
    public record AnnulationCommande(@jakarta.validation.constraints.NotNull UUID sourceRef,
                                     @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long version){}

    public Resultat traiter(String type,Map<String,Object> contenu){
        try { return switch(type){
            case "CREER_RENDEZ_VOUS" -> resultat(agenda.creer(convertir(contenu,AgendaPersonnelRequest.class)));
            case "MODIFIER_RENDEZ_VOUS" -> {
                var corps=new LinkedHashMap<>(contenu);UUID id=extraireRendezVous(corps);
                yield resultat(agenda.modifier(id,convertir(corps,AgendaPersonnelRequest.class)));
            }
            case "ANNULER_RENDEZ_VOUS" -> {
                var corps=new LinkedHashMap<>(contenu);UUID id=extraireRendezVous(corps);
                var commande=convertir(corps,AnnulationCommande.class);
                yield resultat(agenda.annuler(id,commande.sourceRef(),commande.version()));
            }
            case "DECLARER_TRAITEMENT" -> {
                var resultat=declarations.declarer(convertir(contenu,DeclarationTraitementRequest.class));
                var traitement=em.find(Traitement.class,resultat.traitementId());
                yield new Resultat(traitement.getId(),traitement.getVersion());
            }
            default -> throw new IllegalArgumentException("Type d'opération non pris en charge");
        }; } catch(java.time.DateTimeException e) {
            throw new IllegalArgumentException("Date ou fuseau de synchronisation invalide");
        }
    }
    private static UUID extraireRendezVous(Map<String,Object> contenu){
        Object id=contenu.remove("rendezVousId");
        if(id==null)throw new IllegalArgumentException("Identifiant du rendez-vous requis");
        return UUID.fromString(id.toString());
    }
    private <T> T convertir(Map<String,Object> contenu,Class<T> type){
        final T commande;
        try{commande=json.convertValue(contenu,type);}
        catch(tools.jackson.core.JacksonException|IllegalArgumentException e){throw new IllegalArgumentException("Contenu de synchronisation incompatible avec l'opération");}
        if(commande==null||!validation.validate(commande).isEmpty())throw new IllegalArgumentException("Données de synchronisation incomplètes ou invalides");
        return commande;
    }
    private static Resultat resultat(AgendaPersonnelService.Vue vue){return new Resultat(vue.id(),vue.version());}
}
