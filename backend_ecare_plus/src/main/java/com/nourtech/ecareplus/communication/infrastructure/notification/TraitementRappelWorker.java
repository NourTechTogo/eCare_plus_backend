package com.nourtech.ecareplus.communication.infrastructure.notification;
import com.nourtech.ecareplus.care.application.service.RappelTraitementService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
/** Each tick handles at most 100 patients; keyset cursor avoids starving subsequent patients. */
@Component @RequiredArgsConstructor @Slf4j
public class TraitementRappelWorker {
    private final EntityManager em;
    private final TransactionTemplate transactions;
    private final RappelTraitementService rappels;
    private UUID apres;
    public synchronized void traiter(){
        List<UUID> ids=transactions.execute(s->{
            String filtre=apres==null?"":" and p.traitement.dossier.patient.id>:apres";
            var q=em.createQuery("select distinct p.traitement.dossier.patient.id from PriseMedicament p where p.actif=true and p.traitement.statut='ACTIF'"+filtre+" order by p.traitement.dossier.patient.id",UUID.class).setMaxResults(100);
            if(apres!=null)q.setParameter("apres",apres);return q.getResultList();
        });
        Instant now=Instant.now();
        for(UUID id:ids){try{rappels.traiter(id,now);}catch(RuntimeException e){log.warn("Rappel traitement patient {} : {}",id,e.getClass().getSimpleName());}}
        apres=ids.size()<100?null:ids.getLast();
    }
}
