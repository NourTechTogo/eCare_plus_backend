package com.nourtech.ecareplus.shared.infrastructure;
import com.nourtech.ecareplus.communication.infrastructure.notification.NotificationWorker;
import com.nourtech.ecareplus.document.application.service.NettoyageDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name="app.traitements-planifies",havingValue="true",matchIfMissing=true)
public class PlanificationBackend {
    @org.springframework.context.annotation.Bean public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler(){var scheduler=new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();scheduler.setPoolSize(4);scheduler.setThreadNamePrefix("ecare-jobs-");scheduler.setWaitForTasksToCompleteOnShutdown(true);scheduler.setAwaitTerminationSeconds(30);return scheduler;}
    private final NotificationWorker notifications;private final NettoyageDocumentService documents;
    private final com.nourtech.ecareplus.calling.application.service.AppelService appels;
    private final com.nourtech.ecareplus.messaging.application.service.PieceJointeService pieces;
    @Scheduled(fixedDelayString="${app.notifications.intervalle-ms:5000}") public void notifications(){notifications.traiter();}
    private final com.nourtech.ecareplus.communication.infrastructure.notification.AgendaRappelWorker agenda;
    @Scheduled(fixedDelayString="${app.agenda.intervalle-ms:60000}",initialDelay=10000) public void agenda(){agenda.traiter();}
    private final com.nourtech.ecareplus.communication.infrastructure.notification.TraitementRappelWorker traitements;
    @Scheduled(fixedDelayString="${app.rappels-traitements.intervalle-ms:60000}",initialDelay=15000) public void traitements(){traitements.traiter();}
    @Scheduled(fixedDelayString="${app.documents.nettoyage-intervalle-ms:60000}",initialDelay=60000) public void documents(){documents.nettoyer();}
    @Scheduled(fixedDelay=5000) public void appels(){appels.entretenir();}
    @Scheduled(fixedDelay=60000,initialDelay=60000) public void pieces(){pieces.nettoyer();}
}
