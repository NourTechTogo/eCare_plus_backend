package com.nourtech.ecareplus.messaging.application.service;
import com.nourtech.ecareplus.messaging.domain.entity.*;
import com.nourtech.ecareplus.messaging.infrastructure.persistence.*;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.document.application.port.StockagePrive;
import com.nourtech.ecareplus.document.application.service.DocumentMedicalService;
import com.nourtech.ecareplus.shared.application.Empreintes;
import com.nourtech.ecareplus.shared.application.exception.*;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import java.io.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(timeout=60)
public class PieceJointeService {
    private final PieceJointeRepository pieces;private final ConversationRepository conversations;private final AutorisationRessources acces;private final StockagePrive stockage;private final EntityManager em;
    @Value("${app.documents.taille-max:5242880}") private long limite;
    public record PieceVue(UUID id,String nomOriginal,String typeMime,long tailleOctets,Instant dateCreation,Long dureeMs){}
    public record ContenuPiece(PieceVue piece,byte[] octets){}
    public org.springframework.data.domain.Page<PieceVue> lister(UUID acteur,UUID conversation,org.springframework.data.domain.Pageable page){conversation(acteur,conversation);return pieces.visibles(conversation,acteur,org.springframework.data.domain.PageRequest.of(page.isPaged()?page.getPageNumber():0,page.isPaged()?Math.min(100,page.getPageSize()):20,org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("dateCreation"),org.springframework.data.domain.Sort.Order.asc("id")))).map(PieceJointeService::vue);}
    public PieceVue deposer(UUID acteur,UUID conversationId,MultipartFile fichier,String sourceRef){
        var conversation=conversation(acteur,conversationId);em.find(Utilisateur.class,acteur,LockModeType.PESSIMISTIC_WRITE);
        if(sourceRef==null||sourceRef.isBlank()||sourceRef.length()>128||fichier==null||fichier.isEmpty())throw new IllegalArgumentException("Fichier et sourceRef requis");
        if(fichier.getSize()>limite)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Fichier trop volumineux");
        byte[] octets;try(var flux=fichier.getInputStream()){octets=flux.readNBytes(Math.toIntExact(limite+1));}catch(IOException e){throw new IllegalArgumentException("Fichier illisible");}
        if(octets.length>limite)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Fichier trop volumineux");Long duree=null;
        String mime;
        if("audio/wav".equals(fichier.getContentType()) || AudioWav.signature(octets)) { duree=AudioWav.dureeMs(octets);mime="audio/wav"; }
        else mime=DocumentMedicalService.typeReel(octets);
        if(!mime.equals(fichier.getContentType()))throw new IllegalArgumentException("Format réel incompatible");
        String hash=Empreintes.sha256(conversationId+":"+Base64.getEncoder().encodeToString(octets));var precedente=pieces.findByAuteurIdAndSourceRef(acteur,sourceRef);
        if(precedente.isPresent()){var p=precedente.get();if(p.isSupprimee()||!p.getEmpreinte().equals(hash))throw new ConflitMetierException("sourceRef de pièce déjà utilisée");return vue(p);}
        String nom=Objects.requireNonNullElse(fichier.getOriginalFilename(),"document").replace('\\','/');nom=nom.substring(nom.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");if(nom.isBlank())nom="document";if(nom.length()>255)nom=nom.substring(nom.length()-255);
        var stocke=stockage.enregistrer(new ByteArrayInputStream(octets),limite);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int statut){if(statut!=STATUS_COMMITTED)stockage.supprimer(stocke.cle());}});
        var piece=new PieceJointe(conversation,acteur,nom,mime,stocke.taille(),stocke.cle(),sourceRef,hash);
        if(duree!=null)piece.definirDureeAudio(duree);
        return vue(pieces.saveAndFlush(piece));
    }
    public void rattacher(UUID acteur,UUID conversation,MessageConversation message,List<UUID> ids){
        if(ids==null||ids.isEmpty())return;if(ids.size()>5||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("1 à 5 pièces distinctes attendues");
        for(var id:ids){var piece=pieces.findById(id).orElseThrow(()->new RessourceIntrouvableException("Pièce",id));em.refresh(piece,LockModeType.PESSIMISTIC_WRITE);
            if(!piece.getAuteurId().equals(acteur)||!piece.getConversation().getId().equals(conversation)||piece.isSupprimee())throw new AccessDeniedException("Pièce non accessible");
            piece.rattacher(message);message.ajouterPiece(piece);
        }
    }
    public ContenuPiece contenu(UUID acteur,UUID conversation,UUID id){conversation(acteur,conversation);var piece=pieces.findById(id).orElseThrow(()->new RessourceIntrouvableException("Pièce",id));
        if(!piece.getConversation().getId().equals(conversation)||piece.isSupprimee())throw new RessourceIntrouvableException("Pièce",id);
        if(piece.getMessage()==null&&!piece.getAuteurId().equals(acteur))throw new AccessDeniedException("Brouillon privé");
        try(var flux=stockage.ouvrir(piece.getCleStockage())){var data=flux.readNBytes(Math.toIntExact(limite+1));if(data.length!=piece.getTailleOctets())throw new IOException();return new ContenuPiece(vue(piece),data);}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Pièce temporairement inaccessible");}
    }
    public int nettoyer(){int nombre=0;Instant limite=Instant.now().minusSeconds(86400);for(var piece:pieces.findTop100ByMessageIsNullAndSupprimeeFalseAndDateCreationBefore(limite)){
        em.refresh(piece,LockModeType.PESSIMISTIC_WRITE);if(piece.getMessage()!=null||piece.isSupprimee()||!piece.getDateCreation().isBefore(limite))continue;
        try{stockage.supprimer(piece.getCleStockage());piece.supprimer();nombre++;}catch(ResponseStatusException indisponible){/* Retry next cleanup pass. */}
    }return nombre;}
    private Conversation conversation(UUID acteur,UUID id){var c=conversations.findById(id).orElseThrow(()->new RessourceIntrouvableException("Conversation",id));if(!acces.conversationAccessible(c,acteur))throw new AccessDeniedException("Conversation inaccessible");return c;}
    public static PieceVue vue(PieceJointe p){return new PieceVue(p.getId(),p.getNomOriginal(),p.getTypeMime(),p.getTailleOctets(),p.getDateCreation(),p.getDureeMs());}
}
