package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendParcoursCompletTests extends BackendFixture {
    @Test void openApiExposeLesNouveauxContrats()throws Exception{
        var reponse=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v3/api-docs")).timeout(Duration.ofSeconds(30)).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));assertThat(reponse.statusCode()).isEqualTo(200);var chemins=json.readTree(reponse.body()).path("paths");
        for(String chemin:List.of("/api/v1/mobile/me/documents","/api/v1/mobile/me/synchronisations","/api/v1/compte/notifications","/api/v1/portail/prises/{priseId}/planning","/api/v1/appels/{id}/connexion"))assertThat(chemins.has(chemin)).as(chemin).isTrue();
    }
    static com.sun.net.httpserver.HttpServer fournisseur;
    @DynamicPropertySource static void appels(DynamicPropertyRegistry r)throws Exception{
        fournisseur=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);fournisseur.createContext("/twirp/livekit.RoomService/",e->{byte[] requete=e.getRequestBody().readAllBytes();byte[] reponse=e.getRequestURI().getPath().endsWith("CreateRoom")?requete:"{}".getBytes();e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,reponse.length);e.getResponseBody().write(reponse);e.close();});fournisseur.start();
        r.add("app.appels.livekit-url",()->"ws://127.0.0.1:"+fournisseur.getAddress().getPort());r.add("app.appels.api-key",()->"test-http");r.add("app.appels.api-secret",()->"cle-livekit-http-de-test-de-plus-de-32-octets");r.add("app.appels.revocation-active",()->"true");
    }
    @org.junit.jupiter.api.AfterAll static void arreter(){if(fournisseur!=null)fournisseur.stop(0);}
    @LocalServerPort int port;
    @Test void vraiHttpProfilMesureConversationPuisRejetJetonRevoque()throws Exception{
        var patient=patientId();var token=login();var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var profil=client.send(requete("/mobile/me",token).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(profil.statusCode()).isEqualTo(200);assertThat(json.readTree(profil.body()).path("id").asText()).isEqualTo(patient.toString());
        String mesure="{\"typeMesure\":\"GLYCEMIE\",\"valeurPrincipale\":1.1,\"unite\":\"g/L\",\"dateMesure\":\""+Instant.now().minusSeconds(60)+"\",\"sourceRef\":\"http-reel\"}";
        var enregistrement=client.send(requete("/mobile/me/mesures",token).POST(HttpRequest.BodyPublishers.ofString(mesure)).build(),HttpResponse.BodyHandlers.ofString());assertThat(enregistrement.statusCode()).isEqualTo(201);assertThat(json.readTree(enregistrement.body()).path("valeurPrincipale").asInt()).isEqualTo(110);
        var conversation=client.send(requete("/mobile/conversations",token).POST(HttpRequest.BodyPublishers.ofString("{\"professionnelId\":\""+doctor+"\"}")).build(),HttpResponse.BodyHandlers.ofString());assertThat(conversation.statusCode()).isEqualTo(201);String id=json.readTree(conversation.body()).path("id").asText();
        long debut=System.nanoTime();var envoi=client.send(requete("/mobile/conversations/"+id+"/messages",token).POST(HttpRequest.BodyPublishers.ofString("{\"contenu\":\"Bonjour docteur\"}")).build(),HttpResponse.BodyHandlers.ofString());assertThat(envoi.statusCode()).isEqualTo(201);
        var lecture=client.send(requete("/mobile/conversations/"+id+"/messages",doctorToken).GET().build(),HttpResponse.BodyHandlers.ofString());assertThat(lecture.statusCode()).isEqualTo(200);assertThat(json.readTree(lecture.body()).path("content").get(0).path("contenu").asText()).isEqualTo("Bonjour docteur");assertThat(Duration.ofNanos(System.nanoTime()-debut)).isLessThan(Duration.ofSeconds(5));
        var sortie=client.send(requete("/auth/deconnexion",token).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());assertThat(sortie.statusCode()).isEqualTo(204);
        assertThat(client.send(requete("/mobile/me",token).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }
    HttpRequest.Builder requete(String chemin,String token){var demande=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+chemin)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json");if(token!=null)demande.header("Authorization",bearer(token));return demande;}
    final java.util.List<Map<String,Object>> preuves=new ArrayList<>();
    com.fasterxml.jackson.databind.JsonNode http(HttpClient client,String methode,String chemin,String token,Object contenu,int statut)throws Exception{
        var demande=requete(chemin,token).method(methode,contenu==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(contenu instanceof String texte?texte:json.writeValueAsString(contenu))).build();long debut=System.nanoTime();var resultat=client.send(demande,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        preuves.add(Map.of("methode",methode,"route",chemin,"statut",resultat.statusCode(),"dureeMs",(System.nanoTime()-debut)/1_000_000));assertThat(resultat.statusCode()).as(methode+" "+chemin+" : "+resultat.body()).isEqualTo(statut);return resultat.body().isBlank()?json.createObjectNode():json.readTree(resultat.body());
    }
    com.fasterxml.jackson.databind.JsonNode depot(HttpClient client,String chemin,String token,Map<String,String> champs)throws Exception{
        String limite="ecare-"+UUID.randomUUID();var sortie=new java.io.ByteArrayOutputStream();
        for(var champ:champs.entrySet())sortie.write(("--"+limite+"\r\nContent-Disposition: form-data; name=\""+champ.getKey()+"\"\r\n\r\n"+champ.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
        sortie.write(("--"+limite+"\r\nContent-Disposition: form-data; name=\"fichier\"; filename=\"document.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));sortie.write(DocumentMedicalIntegrationTests.PDF);sortie.write(("\r\n--"+limite+"--\r\n").getBytes(StandardCharsets.UTF_8));
        var demande=requete(chemin,token).setHeader("Content-Type","multipart/form-data; boundary="+limite).POST(HttpRequest.BodyPublishers.ofByteArray(sortie.toByteArray())).build();var resultat=client.send(demande,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));preuves.add(Map.of("methode","POST multipart","route",chemin,"statut",resultat.statusCode()));assertThat(resultat.statusCode()).as(resultat.body()).isEqualTo(201);return json.readTree(resultat.body());
    }
    @Test void nouveauComptePersonnelPuisRattachementEtTousLesModulesParVraiHttp()throws Exception{
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();String email="nouveau-"+suffix+"@example.test";LocalDate jour=LocalDate.now(ZoneOffset.UTC);
        var inscription=http(client,"POST","/auth/inscription",null,Map.of("nom","Nouveau","prenoms","Patient","email",email,"telephone","NEW-"+suffix,"motDePasse",password,"consentementAccepte",true,"versionDocument","test-2026","cguLues",true,"confidentialiteLue",true),201);String patient=inscription.path("id").asText();
        String token=http(client,"POST","/auth/connexion",null,Map.of("identifiant",email,"motDePasse",password),200).path("jeton").asText();
        var profilPersonnel=http(client,"GET","/mobile/me",token,null,200);
        assertThat(profilPersonnel.path("dossierId").asText()).isNotBlank();
        assertThat(profilPersonnel.path("structureId").isNull()).isTrue();
        assertThat(profilPersonnel.path("professionnelId").isNull()).isTrue();
        assertThat(http(client,"GET","/mobile/me/dossier",token,null,200).path("id").asText()).isEqualTo(profilPersonnel.path("dossierId").asText());
        http(client,"PATCH","/mobile/me",token,Map.of("dateNaissance","1990-01-01","personneAContacter","Proche fictif","telephoneUrgence","URG-NEW-"+suffix),200);
        String qr=http(client,"POST","/mobile/me/referencements",token,null,201).path("jeton").asText();
        var reference=new LinkedHashMap<String,Object>();reference.put("jeton",qr);reference.put("professionnelId",doctor.toString());reference.put("versionDossier",null);
        String dossier=http(client,"POST","/portail/referencements",adminToken,reference,200).path("dossierId").asText();assertThat(dossier).isNotBlank();
        depot(client,"/mobile/me/documents",token,Map.of("typeDocument","ORDONNANCE","titre","Document fictif","dateDocument",jour.toString(),"sourceRef","http-document"));
        String traitement=http(client,"POST","/dossiers-suivi/"+dossier+"/traitements",doctorToken,Map.of("libelle","Traitement fictif","dateDebut",jour.toString(),"dateFin",jour.toString()),201).path("id").asText();
        String medicament=http(client,"POST","/medicaments",doctorToken,Map.of("nomCommercial","Exemple","denomination","Exemple","forme","comprimé","dosage","10","uniteDosage","mg"),201).path("id").asText();
        http(client,"POST","/traitements/"+traitement+"/prises",doctorToken,Map.of("medicamentId",medicament,"quantite",1,"unite","comprimé","frequence","QUOTIDIEN","heurePrevue","00:00:00","voieAdministration","orale"),201);
        var occurrence=http(client,"GET","/mobile/me/planning-traitements?du="+jour+"&au="+jour,token,null,200).path("occurrences").get(0);
        http(client,"POST","/mobile/me/prises/"+occurrence.path("id").asText()+"/reponse",token,Map.of("action","PRIS","dateAction",Instant.now().minusSeconds(1).toString(),"sourceRef","http-prise","version",occurrence.path("version").asLong()),200);
        String regle=http(client,"POST","/regles-alerte",doctorToken,Map.of("nom","Test critique","patientId",patient,"typeMesure","GLYCEMIE","operateur",">=","seuilMin",200,"nombreOccurrences",1,"periodeObservation",1,"niveauPriorite","CRITIQUE"),201).path("id").asText();
        http(client,"PATCH","/regles-alerte/"+regle+"/validation",doctorToken,null,200);http(client,"PATCH","/regles-alerte/"+regle+"/activation",doctorToken,null,200);
        var mesure=http(client,"POST","/mobile/me/mesures",token,Map.of("typeMesure","GLYCEMIE","valeurPrincipale",250,"unite","mg/dL","dateMesure",Instant.now().minusSeconds(10).toString(),"sourceRef","http-alerte"),201);
        assertThat(http(client,"GET","/mobile/me/alertes",token,null,200).path("totalElements").asInt()).isEqualTo(1);
        assertThat(http(client,"GET","/mobile/me/notifications",token,null,200).path("totalElements").asInt()).isGreaterThanOrEqualTo(2);
        String conversation=http(client,"POST","/mobile/conversations",token,Map.of("professionnelId",doctor.toString()),201).path("id").asText();
        String piece=depot(client,"/mobile/conversations/"+conversation+"/pieces-jointes",token,Map.of("sourceRef","http-piece")).path("id").asText();
        http(client,"POST","/mobile/conversations/"+conversation+"/messages",token,Map.of("contenu","Document de suivi","piecesJointes",List.of(piece)),201);
        assertThat(http(client,"GET","/mobile/conversations/"+conversation+"/messages",doctorToken,null,200).path("content").get(0).path("piecesJointes").size()).isEqualTo(1);
        http(client,"POST","/notes",token,Map.of("typeCible","MESURE","cibleId",mesure.path("id").asText(),"contenu","Note partagée","visibilite","PARTAGEE"),201);
        assertThat(http(client,"GET","/notes?typeCible=MESURE&cibleId="+mesure.path("id").asText(),doctorToken,null,200).path("totalElements").asInt()).isEqualTo(1);
        String appel=http(client,"POST","/mobile/conversations/"+conversation+"/appels",token,Map.of("type","AUDIO","sourceRef","http-appel"),201).path("id").asText();
        http(client,"POST","/appels/"+appel+"/acceptation",doctorToken,null,200);assertThat(http(client,"GET","/appels/"+appel+"/connexion",token,null,200).path("jeton").asText()).isNotBlank();
        assertThat(http(client,"POST","/appels/"+appel+"/fin",token,null,200).path("fermetureConfirmee").asBoolean()).isTrue();
        http(client,"GET","/portail/patients/"+patient+"/resume",doctorToken,null,200);
        http(client,"POST","/mobile/me/consentement",token,Map.of("accepte",false),200);http(client,"GET","/portail/patients/"+patient+"/resume",doctorToken,null,403);
        http(client,"POST","/auth/deconnexion",token,null,204);http(client,"GET","/mobile/me",token,null,401);
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/backend-parcours-http.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("scenarios",preuves,"limites",List.of("H2 isolé","fournisseur appels HTTP local contrôlé","aucune preuve de média ou livraison externe réelle"))),StandardCharsets.UTF_8);
    }
}
