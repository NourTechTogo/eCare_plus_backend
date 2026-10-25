package com.nourtech.ecareplus.document.infrastructure.storage;

import com.nourtech.ecareplus.document.application.port.StockagePrive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.io.*;
import java.util.UUID;

@Component
public class StockageFichierPrive implements StockagePrive {
    private final Path racine;
    public StockageFichierPrive(@Value("${app.documents.repertoire:./var/ecare-prive}") String repertoire){
        racine=Path.of(repertoire).toAbsolutePath().normalize();
        try{Files.createDirectories(racine);if(Files.isSymbolicLink(racine))throw new IOException("Lien symbolique interdit");}
        catch(IOException e){throw new IllegalStateException("Stockage privé inaccessible",e);}
    }
    private Path chemin(String cle){
        if(cle==null||!cle.matches("[0-9a-f-]{36}\\.bin"))throw new IllegalArgumentException("Clé de stockage invalide");
        Path path=racine.resolve(cle).normalize();
        if(!path.getParent().equals(racine)||Files.isSymbolicLink(path))throw new IllegalArgumentException("Chemin privé invalide");
        return path;
    }
    public FichierStocke enregistrer(InputStream flux,long limite){
        Path temporaire=null;
        try{
            temporaire=Files.createTempFile(racine,"depot-",".tmp");long taille=0;
            try(var sortie=Files.newOutputStream(temporaire)){byte[] buffer=new byte[8192];int n;
                while((n=flux.read(buffer))!=-1){taille+=n;if(taille>limite)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Fichier trop volumineux");sortie.write(buffer,0,n);}}
            String cle=UUID.randomUUID()+".bin";Files.move(temporaire,chemin(cle),StandardCopyOption.ATOMIC_MOVE);return new FichierStocke(cle,taille);
        }catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Stockage temporairement indisponible");}
        finally{if(temporaire!=null)try{Files.deleteIfExists(temporaire);}catch(IOException ignored){}}
    }
    public InputStream ouvrir(String cle){try{return Files.newInputStream(chemin(cle));}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Fichier temporairement inaccessible");}}
    public void supprimer(String cle){try{Files.deleteIfExists(chemin(cle));}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Suppression du fichier à reprendre");}}
    public java.util.List<String> candidatsNettoyage(java.time.Instant avant){
        var candidats=new java.util.ArrayList<String>();
        try(var fichiers=Files.list(racine)){for(var fichier:fichiers.toList()){
            if(Files.isSymbolicLink(fichier)||!Files.isRegularFile(fichier,LinkOption.NOFOLLOW_LINKS)||!Files.getLastModifiedTime(fichier,LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(avant))continue;
            String nom=fichier.getFileName().toString();if(nom.matches("[0-9a-f-]{36}\\.bin"))candidats.add(nom);else if(nom.matches("depot-.*\\.tmp"))Files.deleteIfExists(fichier);
        }}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Nettoyage du stockage à reprendre");}return java.util.List.copyOf(candidats);
    }
}
