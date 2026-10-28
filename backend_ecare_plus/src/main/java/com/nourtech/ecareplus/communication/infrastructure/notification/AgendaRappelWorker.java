package com.nourtech.ecareplus.communication.infrastructure.notification;

import com.nourtech.ecareplus.communication.application.service.RappelAgendaService;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component @RequiredArgsConstructor
public class AgendaRappelWorker {
    private final RendezVousRepository rendezVous;
    private final RappelAgendaService rappels;
    public void traiter() {
        Instant maintenant=Instant.now();
        for(var id:rendezVous.echeancesPersonnelles(maintenant,StatutRappel.PLANIFIE,PageRequest.of(0,100))) {
            try{rappels.declencher(id,maintenant);}
            catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Rappel agenda {} à reprendre ({})",id,e.getClass().getSimpleName());}
        }
    }
}
