package com.nourtech.ecareplus.communication.infrastructure.notification;
import com.nourtech.ecareplus.communication.application.port.TransportNotification;
import com.nourtech.ecareplus.communication.application.service.DistributionNotificationService;
import com.nourtech.ecareplus.communication.infrastructure.persistence.EnvoiNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
@Component @RequiredArgsConstructor
public class NotificationWorker {
    private final EnvoiNotificationRepository envois;private final DistributionNotificationService distribution;private final TransportNotification transport;
    public int traiter(){if(!transport.configure())return 0;int nombre=0;
        for(var id:envois.disponibles(Instant.now(),PageRequest.of(0,100))){var reservation=distribution.reclamer(id);if(reservation==null)continue;
            try{var accuse=transport.envoyer(reservation.commande());distribution.terminer(reservation,accuse,null);}
            catch(RuntimeException erreur){distribution.terminer(reservation,null,"TRANSPORT_INDISPONIBLE");}nombre++;
        }return nombre;
    }
}
