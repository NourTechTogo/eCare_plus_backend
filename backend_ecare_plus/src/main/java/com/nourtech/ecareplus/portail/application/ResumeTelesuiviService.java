package com.nourtech.ecareplus.portail.application;
import com.nourtech.ecareplus.care.application.service.*;
import com.nourtech.ecareplus.messaging.application.service.MessagerieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class ResumeTelesuiviService {
    private final PortailService portail;private final PlanningTraitementService planning;private final ObservanceService observance;private final MessagerieService messagerie;
    @Transactional public Map<String,Object> consulter(UUID patientId){
        portail.autoriser(patientId,true);UUID acteur=portail.acteur().getId();LocalDate aujourdHui=LocalDate.now(ZoneOffset.UTC);
        Map<String,Object> vue=new LinkedHashMap<>();vue.put("patient",portail.consulter(patientId));vue.put("dernieresMesures",portail.mesures(patientId,false,null,null,null,PageRequest.of(0,20)));
        vue.put("alertesOuvertes",portail.alertesOuvertes(patientId,PageRequest.of(0,20)));vue.put("planning",planning.consulter(acteur,patientId,aujourdHui,aujourdHui.plusDays(7)));
        vue.put("observance",observance.calculer(acteur,patientId,aujourdHui.minusDays(29),aujourdHui));
        vue.put("conversations",messagerie.lister(acteur).stream().filter(c->c.getPatient().getId().equals(patientId)).map(c->Map.of("id",c.getId(),"professionnelId",c.getProfessionnel().getId())).toList());return vue;
    }
}
