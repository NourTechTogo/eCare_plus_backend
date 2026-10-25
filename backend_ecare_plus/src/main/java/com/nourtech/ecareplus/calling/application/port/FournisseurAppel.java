package com.nourtech.ecareplus.calling.application.port;
import java.time.Instant;
import java.util.UUID;
public interface FournisseurAppel {
    record AccesAppel(String url,String jeton,Instant expireLe){}
    boolean configure();
    void creerSalle(String salle);
    AccesAppel autoriserParticipant(String salle,UUID utilisateur,String type);
    void fermerSalle(String salle,UUID initiateur,UUID destinataire);
    void verifierWebhook(String corps,String autorisation);
}
