package com.nourtech.ecareplus.document.application.service;

import com.nourtech.ecareplus.document.application.port.StockagePrive;
import com.nourtech.ecareplus.document.domain.entity.DocumentMedical;
import com.nourtech.ecareplus.document.infrastructure.persistence.DocumentMedicalRepository;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.audit.application.service.JournalisationService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.time.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(timeout=60)
public class DocumentMedicalService {
    private final DocumentMedicalRepository documents;
    private final StockagePrive stockage;
    private final AccesPatientService acces;
    private final EntityManager em;
    private final JournalisationService audit;
    @Value("${app.documents.taille-max:5242880}") private long limite;
    public record DepotDocumentCommande(MultipartFile fichier,String typeDocument,String titre,LocalDate dateDocument,String sourceRef){}
    public record DocumentVue(UUID id,UUID patientId,String typeDocument,String titre,LocalDate dateDocument,String nomOriginal,String typeMime,long tailleOctets,Instant dateCreation,UUID auteurId){}
    public record ContenuDocument(DocumentVue document,byte[] octets){}
    public DocumentVue deposer(UUID acteurId,UUID patientId,DepotDocumentCommande commande){
        acces.verifier(acteurId,patientId);em.find(Patient.class,patientId,LockModeType.PESSIMISTIC_WRITE);
        String titre=texte(commande.titre(),200,"titre"),type=texte(commande.typeDocument(),40,"typeDocument"),source=texte(commande.sourceRef(),128,"sourceRef");
        if(!Set.of("ORDONNANCE","RESULTAT_ANALYSE","COMPTE_RENDU","CARNET","AUTRE").contains(type))throw new IllegalArgumentException("Type de document inconnu");
        if(commande.dateDocument()==null||commande.dateDocument().isAfter(LocalDate.now(ZoneOffset.UTC)))throw new IllegalArgumentException("Date du document invalide");
        var fichier=commande.fichier();
        if(fichier==null||fichier.isEmpty())throw new IllegalArgumentException("Fichier requis");
        if(fichier.getSize()>limite)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Fichier trop volumineux");
        byte[] octets;
        try(var flux=fichier.getInputStream()){octets=flux.readNBytes(Math.toIntExact(limite+1));}catch(IOException e){throw new IllegalArgumentException("Fichier illisible");}
        if(octets.length>limite)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Fichier trop volumineux");
        String mime=typeReel(octets);
        if(!mime.equals(fichier.getContentType()))throw new IllegalArgumentException("Le contenu ne correspond pas au format déclaré");
        String empreinte=empreinte(octets,titre,type,commande.dateDocument().toString());
        var precedent=documents.findByPatientIdAndSourceRef(patientId,source);
        if(precedent.isPresent()){
            if(precedent.get().getSupprimeLe()!=null||!precedent.get().getEmpreinte().equals(empreinte))throw new ConflitMetierException("Clé de dépôt déjà utilisée avec un autre contenu");
            return vue(precedent.get());
        }
        String nom=Objects.requireNonNullElse(fichier.getOriginalFilename(),"document").replace('\\','/');nom=nom.substring(nom.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]", "");
        if(nom.isBlank())nom="document";if(nom.length()>255)nom=nom.substring(nom.length()-255);
        var stocke=stockage.enregistrer(new ByteArrayInputStream(octets),limite);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)stockage.supprimer(stocke.cle());}
        });
        var document=documents.saveAndFlush(new DocumentMedical(patientId,acteurId,type,titre,commande.dateDocument(),nom,mime,stocke.taille(),stocke.cle(),source,empreinte));
        audit.enregistrer(acteurId,"DEPOT_DOCUMENT","DOCUMENT",document.getId(),null,null,"SUCCES");return vue(document);
    }
    public Page<DocumentVue> lister(UUID acteurId,UUID patientId,Pageable page){acces.verifier(acteurId,patientId);return documents.findByPatientIdAndSupprimeLeIsNull(patientId,page).map(this::vue);}
    public ContenuDocument contenu(UUID acteurId,UUID patientId,UUID documentId){
        var document=exiger(acteurId,patientId,documentId);
        if(document.getSupprimeLe()!=null)throw new RessourceIntrouvableException("Document",documentId);
        try(var flux=stockage.ouvrir(document.getCleStockage())){
            byte[] octets=flux.readNBytes(Math.toIntExact(limite+1));if(octets.length!=document.getTailleOctets())throw new IOException();
            audit.enregistrer(acteurId,"CONSULTER_DOCUMENT","DOCUMENT",documentId,null,null,"SUCCES");return new ContenuDocument(vue(document),octets);
        }catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Fichier temporairement inaccessible");}
    }
    public void supprimer(UUID acteurId,UUID documentId){
        var document=documents.findById(documentId).orElseThrow(()->new RessourceIntrouvableException("Document",documentId));
        if(!acteurId.equals(document.getPatientId()))throw new org.springframework.security.access.AccessDeniedException("Suppression réservée au patient");
        acces.verifier(acteurId,document.getPatientId());document.retirer();
        audit.enregistrer(acteurId,"SUPPRIMER_DOCUMENT","DOCUMENT",documentId,null,null,"SUCCES");
    }
    private DocumentMedical exiger(UUID acteurId,UUID patientId,UUID documentId){
        acces.verifier(acteurId,patientId);
        var document=documents.findById(documentId).orElseThrow(()->new RessourceIntrouvableException("Document",documentId));
        if(!document.getPatientId().equals(patientId))throw new RessourceIntrouvableException("Document",documentId);return document;
    }
    private DocumentVue vue(DocumentMedical d){return new DocumentVue(d.getId(),d.getPatientId(),d.getTypeDocument(),d.getTitre(),d.getDateDocument(),d.getNomOriginal(),d.getTypeMime(),d.getTailleOctets(),d.getDateCreation(),d.getAuteurId());}
    private static String texte(String valeur,int max,String champ){if(valeur==null||valeur.isBlank()||valeur.trim().length()>max)throw new IllegalArgumentException(champ+" invalide");return valeur.trim();}
    public static String typeReel(byte[] data){
        if(data.length>12&&new String(data,0,5,StandardCharsets.US_ASCII).equals("%PDF-")&&new String(data,Math.max(0,data.length-1024),Math.min(data.length,1024),StandardCharsets.ISO_8859_1).contains("%%EOF"))return "application/pdf";
        String mime=null;
        if(data.length>8&&(data[0]&255)==0xff&&(data[1]&255)==0xd8)mime="image/jpeg";
        if(data.length>8&&Arrays.equals(Arrays.copyOf(data,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))mime="image/png";
        if(mime!=null)try(var entree=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(data))){var lecteurs=javax.imageio.ImageIO.getImageReaders(entree);if(lecteurs.hasNext()){
            var lecteur=lecteurs.next();try{lecteur.setInput(entree);int largeur=lecteur.getWidth(0),hauteur=lecteur.getHeight(0);
                if(largeur<1||hauteur<1||(long)largeur*hauteur>20_000_000L)throw new IllegalArgumentException("Les dimensions de l'image dépassent la limite de 20 mégapixels");
                if(lecteur.read(0)!=null)return mime;
            }finally{lecteur.dispose();}
        }}catch(IOException ignored){}
        throw new IllegalArgumentException("Formats autorisés : JPEG, PNG ou PDF valide");
    }
    private static String empreinte(byte[] data,String... valeurs){try{var md=MessageDigest.getInstance("SHA-256");md.update(data);for(String value:valeurs){md.update((byte)0);md.update(value.getBytes(StandardCharsets.UTF_8));}return HexFormat.of().formatHex(md.digest());}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
