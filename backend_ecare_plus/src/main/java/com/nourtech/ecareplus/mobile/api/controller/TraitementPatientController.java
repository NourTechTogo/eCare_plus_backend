package com.nourtech.ecareplus.mobile.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.care.application.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor @AccesVerifieParService
public class TraitementPatientController {
    private final PlanningTraitementService planning;private final ObservanceService observance;private final AccesPatientService acces;
    @org.springframework.beans.factory.annotation.Autowired
    private com.nourtech.ecareplus.mobile.application.service.DeclarationTraitementService declarations;
    @PostMapping("/mobile/me/traitements")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public PrescriptionAtomiqueService.PrescriptionVue declarer(@jakarta.validation.Valid @RequestBody com.nourtech.ecareplus.mobile.api.dto.DeclarationTraitementRequest commande) {
        return declarations.declarer(commande);
    }
    @GetMapping({"/mobile/me/planning-traitements","/portail/patients/{patientId}/planning-traitements"})
    public PlanningTraitementService.PlanningVue planning(@PathVariable(required=false) UUID patientId,@RequestParam LocalDate du,@RequestParam LocalDate au){var acteur=acces.acteurId();return planning.consulter(acteur,patientId==null?acteur:patientId,du,au);}
    @PutMapping("/portail/prises/{priseId}/planning")
    public PlanningTraitementService.PrisePlanningVue configurer(@PathVariable UUID priseId,@RequestBody PlanningTraitementService.PlanningCommande commande){return planning.configurer(acces.acteurId(),priseId,commande);}
    @GetMapping({"/mobile/me/prises","/portail/patients/{patientId}/prises"})
    public List<PlanningTraitementService.OccurrenceVue> historique(@PathVariable(required=false) UUID patientId,@RequestParam LocalDate du,@RequestParam LocalDate au){var acteur=acces.acteurId();return observance.historique(acteur,patientId==null?acteur:patientId,du,au);}
    @GetMapping("/mobile/me/prises/{occurrenceId}")
    public PlanningTraitementService.OccurrenceVue occurrence(@PathVariable UUID occurrenceId){return observance.consulterOccurrence(acces.acteurId(),occurrenceId);}
    @PostMapping("/mobile/me/prises/{occurrenceId}/reponse")
    public PlanningTraitementService.OccurrenceVue repondre(@PathVariable UUID occurrenceId,@RequestBody ObservanceService.ReponsePriseCommande commande){return observance.confirmer(acces.acteurId(),occurrenceId,commande);}
    @GetMapping({"/mobile/me/observance","/portail/patients/{patientId}/observance"})
    public ObservanceService.ObservanceVue observance(@PathVariable(required=false) UUID patientId,@RequestParam LocalDate du,@RequestParam LocalDate au){var acteur=acces.acteurId();return observance.calculer(acteur,patientId==null?acteur:patientId,du,au);}
}
