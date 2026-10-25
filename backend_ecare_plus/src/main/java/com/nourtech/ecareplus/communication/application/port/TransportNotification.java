package com.nourtech.ecareplus.communication.application.port;
import java.util.UUID;
public interface TransportNotification {
    record EnvoiCommande(UUID envoiId,String canal,String destination,String titre,String message,UUID notificationId,String typeRessource,UUID ressourceId){}
    record AccuseFournisseur(String reference,boolean livre,boolean jetonInvalide){}
    boolean configure();
    AccuseFournisseur envoyer(EnvoiCommande commande);
}
