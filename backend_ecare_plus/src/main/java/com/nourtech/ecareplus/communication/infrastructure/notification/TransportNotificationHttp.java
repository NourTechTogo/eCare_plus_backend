package com.nourtech.ecareplus.communication.infrastructure.notification;
import com.nourtech.ecareplus.communication.application.port.TransportNotification;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
@Component
public class TransportNotificationHttp implements TransportNotification {
    private final String url,secret;private final ObjectMapper json;
    public TransportNotificationHttp(@Value("${app.notifications.gateway-url:}")String url,@Value("${app.notifications.gateway-secret:}")String secret,ObjectMapper json){this.url=url;this.secret=secret;this.json=json;
        if(!url.isBlank()){var uri=URI.create(url);if(!"https".equals(uri.getScheme())&&!("http".equals(uri.getScheme())&&"127.0.0.1".equals(uri.getHost())))throw new IllegalArgumentException("Passerelle de notification : HTTPS requis");}}
    public boolean configure(){return !url.isBlank()&&!secret.isBlank();}
    public AccuseFournisseur envoyer(EnvoiCommande commande){
        if(!configure())throw new IllegalStateException("Passerelle non configurée");
        try{
            var requete=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+secret).header("Idempotency-Key",commande.envoiId().toString()).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(commande))).build();
            var resultat=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build().send(requete,HttpResponse.BodyHandlers.ofString());
            if(resultat.statusCode()<200||resultat.statusCode()>=300)throw new IllegalStateException("FOURNISSEUR_HTTP_"+resultat.statusCode());
            var reponse=json.readTree(resultat.body());String statut=reponse.path("statut").asText(),reference=reponse.path("reference").asText();
            if(statut.equals("JETON_INVALIDE"))return new AccuseFournisseur(reference,false,true);
            if(reference.isBlank()||reference.length()>200||!java.util.Set.of("ACCEPTE","LIVRE").contains(statut))throw new IllegalStateException("ACCUSE_INVALIDE");
            return new AccuseFournisseur(reference,statut.equals("LIVRE"),false);
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("ENVOI_INTERROMPU");}catch(java.io.IOException e){throw new IllegalStateException("FOURNISSEUR_INJOIGNABLE");}
    }
}
