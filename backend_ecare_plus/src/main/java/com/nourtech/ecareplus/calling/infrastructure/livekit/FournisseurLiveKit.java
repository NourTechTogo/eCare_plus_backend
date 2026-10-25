package com.nourtech.ecareplus.calling.infrastructure.livekit;
import com.nourtech.ecareplus.calling.application.port.FournisseurAppel;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
@Component
public class FournisseurLiveKit implements FournisseurAppel {
    private final String url,cle,secret;private final boolean revocation;private final ObjectMapper json;
    public FournisseurLiveKit(@Value("${app.appels.livekit-url:}")String url,@Value("${app.appels.api-key:}")String cle,@Value("${app.appels.api-secret:}")String secret,@Value("${app.appels.revocation-active:false}")boolean revocation,ObjectMapper json){
        this.url=url.replaceAll("/+$","");this.cle=cle;this.secret=secret;this.revocation=revocation;this.json=json;
        if(!url.isBlank()){var uri=URI.create(url);if(!"wss".equals(uri.getScheme())&&!("ws".equals(uri.getScheme())&&"127.0.0.1".equals(uri.getHost())))throw new IllegalArgumentException("LiveKit doit utiliser wss hors boucle locale");}
    }
    public boolean configure(){return !url.isBlank()&&!cle.isBlank()&&secret.getBytes(StandardCharsets.UTF_8).length>=32&&revocation;}
    private void disponible(){if(!configure())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Appels non configurés ; utilisez la messagerie");}
    public void creerSalle(String salle){disponible();var resultat=requete("CreateRoom",Map.of("name",salle,"empty_timeout",90,"departure_timeout",20,"max_participants",2),Map.of("roomCreate",true));if(!salle.equals(json.readTree(resultat).path("name").asText()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Salle non confirmée par le fournisseur");}
    public AccesAppel autoriserParticipant(String salle,UUID utilisateur,String type){disponible();Instant expiration=Instant.now().plusSeconds(60);return new AccesAppel(url,jeton(utilisateur.toString(),Map.of("room",salle,"roomJoin",true,"canSubscribe",true,"canPublish",true,"canPublishData",false,"canPublishSources",type.equals("AUDIO")?List.of("microphone"):List.of("microphone","camera")),expiration),expiration);}
    public void fermerSalle(String salle,UUID initiateur,UUID destinataire){disponible();var grant=Map.<String,Object>of("roomAdmin",true,"room",salle);long cutoff=Instant.now().plusSeconds(1).getEpochSecond();
        requete("RemoveParticipant",Map.of("room",salle,"identity",initiateur.toString(),"revoke_token_ts",cutoff),grant);
        requete("RemoveParticipant",Map.of("room",salle,"identity",destinataire.toString(),"revoke_token_ts",cutoff),grant);
        requete("DeleteRoom",Map.of("room",salle),Map.of("roomCreate",true));
    }
    private String requete(String methode,Map<String,Object> corps,Map<String,Object> grant){
        try{String http=url.replaceFirst("^wss:","https:").replaceFirst("^ws:","http:");
            var demande=HttpRequest.newBuilder(URI.create(http+"/twirp/livekit.RoomService/"+methode)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+jeton(null,grant,Instant.now().plusSeconds(60))).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corps))).build();
            var resultat=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build().send(demande,HttpResponse.BodyHandlers.ofString());
            if(resultat.statusCode()==404 && Set.of("RemoveParticipant","DeleteRoom").contains(methode)
                    && "not_found".equals(json.readTree(resultat.body()).path("code").asText()))return resultat.body();
            if(resultat.statusCode()<200||resultat.statusCode()>=300)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Fournisseur d'appel indisponible");return resultat.body();
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Appel interrompu");}catch(java.io.IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Fournisseur d'appel injoignable");}
    }
    private String jeton(String sujet,Map<String,Object> grant,Instant expiration){try{var claims=new JWTClaimsSet.Builder().issuer(cle).issueTime(new Date()).notBeforeTime(new Date()).expirationTime(Date.from(expiration)).claim("video",grant);if(sujet!=null)claims.subject(sujet);var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),claims.build());jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));return jwt.serialize();}catch(JOSEException e){throw new IllegalStateException("Signature d'appel impossible",e);}}
    public void verifierWebhook(String corps,String autorisation){
        try{disponible();if(corps==null||corps.length()>64000||autorisation==null)throw new IllegalArgumentException();var token=SignedJWT.parse(autorisation.startsWith("Bearer ")?autorisation.substring(7):autorisation);var c=token.getJWTClaimsSet();
            if(!token.getHeader().getAlgorithm().equals(JWSAlgorithm.HS256)||!token.verify(new MACVerifier(secret.getBytes(StandardCharsets.UTF_8)))||!cle.equals(c.getIssuer())||c.getExpirationTime()==null||c.getExpirationTime().before(new Date())||(c.getNotBeforeTime()!=null&&c.getNotBeforeTime().after(Date.from(Instant.now().plusSeconds(10)))))throw new IllegalArgumentException();
            byte[] attendu=Base64.getDecoder().decode(c.getStringClaim("sha256"));if(!MessageDigest.isEqual(attendu,MessageDigest.getInstance("SHA-256").digest(corps.getBytes(StandardCharsets.UTF_8))))throw new IllegalArgumentException();
        }catch(Exception invalide){throw new AccessDeniedException("Signature de webhook d'appel invalide");}
    }
}
