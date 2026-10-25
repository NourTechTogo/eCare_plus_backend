package com.nourtech.ecareplus.document.application.port;
import java.io.InputStream;
public interface StockagePrive {
    record FichierStocke(String cle,long taille){}
    FichierStocke enregistrer(InputStream flux,long limite);
    InputStream ouvrir(String cle);
    void supprimer(String cle);
    default java.util.List<String> candidatsNettoyage(java.time.Instant avant){return java.util.List.of();}
}
