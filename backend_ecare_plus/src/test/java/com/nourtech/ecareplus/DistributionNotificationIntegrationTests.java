package com.nourtech.ecareplus;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DistributionNotificationIntegrationTests extends BackendFixture {
    @Autowired ApplicationContext contexte;
    static HttpServer fournisseur;static AtomicBoolean panne=new AtomicBoolean(true);static AtomicInteger requetes=new AtomicInteger();static String payload,cle;
    @DynamicPropertySource static void fournisseur(DynamicPropertyRegistry registre) throws Exception {
        fournisseur=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        fournisseur.createContext("/envoi",e->{requetes.incrementAndGet();payload=new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);cle=e.getRequestHeaders().getFirst("Idempotency-Key");
            byte[] corps="{\"reference\":\"sandbox-1\",\"statut\":\"ACCEPTE\"}".getBytes();e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(panne.get()?503:200,corps.length);e.getResponseBody().write(corps);e.close();});fournisseur.start();
        registre.add("app.notifications.gateway-url",()->"http://127.0.0.1:"+fournisseur.getAddress().getPort()+"/envoi");
        registre.add("app.notifications.gateway-secret",()->"cle-sandbox-notifications");registre.add("app.notifications.delai-reprise-secondes",()->"0");
        registre.add("app.traitements-planifies",()->"false");
    }
    @AfterAll static void arreter(){if(fournisseur!=null)fournisseur.stop(0);}
    @Test void outboxPersistanteReessayeSansAnnoncerLivraison() throws Exception {
        var patient=patientId();var token=login();
        mvc.perform(put("/api/v1/compte/terminaux/{id}","doctor-"+suffix).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"plateforme\":\"WEB\",\"jetonPush\":\"jeton-sandbox\"}" )).andExpect(status().isOk());
        var conversation=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}" )).andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation.path("id").asText()).header("Authorization",bearer(token)).contentType("application/json").content("{\"contenu\":\"Information médicale confidentielle à ne pas notifier\"}" )).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where lower(table_name)='envoi_notification'",Integer.class)).as("outbox durable créée").isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from envoi_notification where destinataire_id=?",Integer.class,doctor)).isEqualTo(1);
        var worker=contexte.getBean("notificationWorker");panne.set(true);ReflectionTestUtils.invokeMethod(worker,"traiter");
        assertThat(jdbc.queryForObject("select statut from envoi_notification where destinataire_id=?",String.class,doctor)).isEqualTo("A_REESSAYER");
        String premiereCle=cle;panne.set(false);ReflectionTestUtils.invokeMethod(worker,"traiter");
        assertThat(jdbc.queryForObject("select statut from envoi_notification where destinataire_id=?",String.class,doctor)).isEqualTo("ACCEPTE");
        assertThat(cle).isEqualTo(premiereCle);assertThat(payload).doesNotContain("Information médicale confidentielle");assertThat(requetes.get()).isGreaterThanOrEqualTo(2);
        var envoi=jdbc.queryForObject("select id from envoi_notification where destinataire_id=?",java.util.UUID.class,doctor);
        String callback="{\"envoiId\":\""+envoi+"\",\"reference\":\"sandbox-1\",\"statut\":\"LIVRE\"}";
        String date=String.valueOf(java.time.Instant.now().getEpochSecond());
        mvc.perform(post("/api/v1/integrations/notifications/callback").header("X-Ecare-Timestamp",date).header("X-Ecare-Signature","invalide").contentType("application/json").content(callback)).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(post("/api/v1/integrations/notifications/callback").header("X-Ecare-Timestamp",date).header("X-Ecare-Signature",signature(date,callback)).contentType("application/json").content(callback)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select statut from envoi_notification where id=?",String.class,envoi)).isEqualTo("LIVRE");
    }
    String signature(String date,String corps)throws Exception{var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("cle-sandbox-notifications".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal((date+"."+corps).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
}
