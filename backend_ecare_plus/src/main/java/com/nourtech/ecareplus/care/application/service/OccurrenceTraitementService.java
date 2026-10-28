package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.infrastructure.persistence.ConfirmationPriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
/** Internal materialization only. Callers must hold the patient lock and authorize the operation. */
@Service @RequiredArgsConstructor @Transactional
public class OccurrenceTraitementService {
    private final ConfirmationPriseRepository confirmations;
    private final PlanningHistoriqueService historique;
    public void materialiser(UUID patientId,UUID structure,LocalDate du,LocalDate au,List<PriseMedicament> liste){
        var existantes=new HashSet<String>();for(var c:confirmations.periodeStructure(patientId,structure,PlanningTraitementService.debut(du),PlanningTraitementService.fin(au)))existantes.add(c.getPriseMedicament().getId()+":"+c.getDateInitiale());
        for(var p:liste)for(var revision:historique.revisions(p)){var instantane=historique.lire(revision);
            for(LocalDate jour=du;!jour.isAfter(au);jour=jour.plusDays(1)){
                if(!instantane.prevu(jour))continue;Instant date=instantane.date(jour);
                if(revision.applicable(date)&&existantes.add(p.getId()+":"+date)){var occurrence=new ConfirmationPrise(p,date);occurrence.appliquerInstantane(instantane);confirmations.save(occurrence);}
            }
        }
    }
}
