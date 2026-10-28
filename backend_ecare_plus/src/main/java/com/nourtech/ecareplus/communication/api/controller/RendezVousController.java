package com.nourtech.ecareplus.communication.api.controller;

import com.nourtech.ecareplus.communication.api.dto.request.ProgrammerRendezVousRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ReporterRendezVousRequest;
import com.nourtech.ecareplus.communication.api.dto.response.RendezVousResponse;
import com.nourtech.ecareplus.communication.api.mapper.CommunicationMapper;
import com.nourtech.ecareplus.communication.application.service.RendezVousService;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;

@RestController
@RequestMapping("/api/v1/rendez-vous")
public class RendezVousController {

    private final RendezVousService service;
    private final CommunicationMapper mapper;
    private final EntityManager em;
    private final AutorisationRessources acces;

    public RendezVousController(RendezVousService service, CommunicationMapper mapper, EntityManager em, AutorisationRessources acces) {
        this.service = service;
        this.mapper = mapper;
        this.em = em;
        this.acces = acces;
    }

    @GetMapping @Transactional(readOnly=true)
    public List<Map<String,Object>> lister(@RequestParam(required=false) UUID patientId) {
        var professionnel=(ProfessionnelSante)acces.acteur();
        var resultats=em.createQuery("""
                select r from RendezVous r join fetch r.patient p
                where r.personnel=false and (:patient is null or p.id=:patient) and p.statutCompte=:compte
                and exists (select a.id from AffectationPatient a where a.patient=p
                    and a.professionnel.id=:professionnel and a.structure.id=:structure
                    and a.statut=:affectation and a.dateDebut<=:jour
                    and (a.dateFin is null or a.dateFin>=:jour))
                and exists (select c.id from Consentement c where c.patient=p
                    and c.statut=:consentement and c.dateAcceptation<=:maintenant
                    and (c.dateRetrait is null or c.dateRetrait>:maintenant))
                order by r.dateHeure asc, r.id asc
                """,RendezVous.class)
                .setParameter("patient",patientId).setParameter("compte",StatutCompte.ACTIF)
                .setParameter("professionnel",professionnel.getId()).setParameter("structure",professionnel.getStructure().getId())
                .setParameter("affectation",StatutAffectation.ACTIVE).setParameter("jour",LocalDate.now(ZoneOffset.UTC))
                .setParameter("consentement",StatutConsentement.ACCORDE).setParameter("maintenant",Instant.now()).getResultList();
        return resultats.stream().map(r->{
            Map<String,Object> vue=new java.util.LinkedHashMap<>();
            vue.put("id",r.getId());vue.put("patientId",r.getPatient().getId());
            vue.put("patientNom",r.getPatient().getPrenoms()+" "+r.getPatient().getNom());
            vue.put("numeroPatient",r.getPatient().getNumeroPatient());vue.put("motif",r.getMotif());
            vue.put("dateHeure",r.getDateHeure());vue.put("lieu",r.getLieu());vue.put("typeRendezVous",r.getTypeRendezVous());
            vue.put("statut",r.getStatut());vue.put("version",r.getVersion());return vue;
        }).toList();
    }

    @PostMapping
    public ResponseEntity<RendezVousResponse> programmer(@Valid @RequestBody ProgrammerRendezVousRequest request) {
        RendezVous rendezVous = service.programmer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(rendezVous.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(rendezVous));
    }

    @GetMapping("/{rendezVousId}")
    public RendezVousResponse consulter(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.consulter(rendezVousId));
    }

    @PatchMapping("/{rendezVousId}/confirmation")
    public RendezVousResponse confirmer(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.confirmer(rendezVousId));
    }

    @PatchMapping("/{rendezVousId}/report")
    public RendezVousResponse reporter(
            @PathVariable UUID rendezVousId,
            @Valid @RequestBody ReporterRendezVousRequest request
    ) {
        return mapper.toResponse(service.reporter(rendezVousId, request.nouvelleDateHeure()));
    }

    @PatchMapping("/{rendezVousId}/annulation")
    public RendezVousResponse annuler(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.annuler(rendezVousId));
    }

    @PatchMapping("/{rendezVousId}/realisation")
    public RendezVousResponse marquerCommeRealise(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.marquerCommeRealise(rendezVousId));
    }
}
