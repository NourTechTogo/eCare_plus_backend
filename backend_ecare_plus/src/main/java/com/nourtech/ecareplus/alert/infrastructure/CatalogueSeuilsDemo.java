package com.nourtech.ecareplus.alert.infrastructure;

import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.infrastructure.persistence.RegleAlerteRepository;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;

/** CDC p.19 examples, explicitly limited to the demo profile. No claim of medical validation. */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "app.alertes.seuils-demo", havingValue = "true")
public class CatalogueSeuilsDemo implements CommandLineRunner {
    private final RegleAlerteRepository repository;
    public CatalogueSeuilsDemo(RegleAlerteRepository repository) { this.repository = repository; }

    @Override @Transactional public void run(String... args) {
        seuil("Systolique haute", TypeMesure.TENSION_ARTERIELLE, "PRINCIPALE", ">=", "140", NiveauPriorite.ELEVE, null);
        seuil("Systolique critique haute", TypeMesure.TENSION_ARTERIELLE, "PRINCIPALE", ">=", "180", NiveauPriorite.CRITIQUE, null);
        seuil("Systolique basse", TypeMesure.TENSION_ARTERIELLE, "PRINCIPALE", "<", "90", NiveauPriorite.ELEVE, null);
        seuil("Systolique critique basse", TypeMesure.TENSION_ARTERIELLE, "PRINCIPALE", "<", "80", NiveauPriorite.CRITIQUE, null);
        seuil("Diastolique haute", TypeMesure.TENSION_ARTERIELLE, "DIASTOLIQUE", ">=", "90", NiveauPriorite.ELEVE, null);
        seuil("Diastolique critique haute", TypeMesure.TENSION_ARTERIELLE, "DIASTOLIQUE", ">=", "120", NiveauPriorite.CRITIQUE, null);
        seuil("Diastolique basse", TypeMesure.TENSION_ARTERIELLE, "DIASTOLIQUE", "<", "60", NiveauPriorite.ELEVE, null);
        seuil("Diastolique critique basse", TypeMesure.TENSION_ARTERIELLE, "DIASTOLIQUE", "<", "50", NiveauPriorite.CRITIQUE, null);
        seuil("Glycémie à jeun haute", TypeMesure.GLYCEMIE, "PRINCIPALE", ">", "110", NiveauPriorite.ELEVE, "a_jeun");
        seuil("Glycémie à jeun critique haute", TypeMesure.GLYCEMIE, "PRINCIPALE", ">=", "200", NiveauPriorite.CRITIQUE, "a_jeun");
        seuil("Glycémie à jeun basse", TypeMesure.GLYCEMIE, "PRINCIPALE", "<", "70", NiveauPriorite.ELEVE, "a_jeun");
        seuil("Glycémie à jeun critique basse", TypeMesure.GLYCEMIE, "PRINCIPALE", "<", "55", NiveauPriorite.CRITIQUE, "a_jeun");
        seuil("Fréquence haute", TypeMesure.FREQUENCE_CARDIAQUE, "PRINCIPALE", ">", "100", NiveauPriorite.ELEVE, null);
        seuil("Fréquence critique haute", TypeMesure.FREQUENCE_CARDIAQUE, "PRINCIPALE", ">", "130", NiveauPriorite.CRITIQUE, null);
        seuil("Fréquence basse", TypeMesure.FREQUENCE_CARDIAQUE, "PRINCIPALE", "<", "50", NiveauPriorite.ELEVE, null);
        seuil("Fréquence critique basse", TypeMesure.FREQUENCE_CARDIAQUE, "PRINCIPALE", "<", "40", NiveauPriorite.CRITIQUE, null);
        seuil("Saturation basse", TypeMesure.SPO2, "PRINCIPALE", "<", "95", NiveauPriorite.ELEVE, null);
        seuil("Saturation critique", TypeMesure.SPO2, "PRINCIPALE", "<=", "90", NiveauPriorite.CRITIQUE, null);
        seuil("Température haute", TypeMesure.TEMPERATURE, "PRINCIPALE", ">", "37.5", NiveauPriorite.ELEVE, null);
        seuil("Température critique haute", TypeMesure.TEMPERATURE, "PRINCIPALE", ">=", "39.5", NiveauPriorite.CRITIQUE, null);
        seuil("Température basse", TypeMesure.TEMPERATURE, "PRINCIPALE", "<", "36", NiveauPriorite.ELEVE, null);
        seuil("Température critique basse", TypeMesure.TEMPERATURE, "PRINCIPALE", "<", "35", NiveauPriorite.CRITIQUE, null);
        String nom="DÉMO - Variation de poids sur 3 jours";
        if(repository.findAll().stream().noneMatch(r->r.getNom().equals(nom))){
            var variation=new RegleAlerte(nom,"Exemple indicatif du CDC, démonstration uniquement.",TypeMesure.POIDS,"VARIATION_ABSOLUE",new BigDecimal("2"),null,1,4320,NiveauPriorite.ELEVE);
            variation.definirPortee(null,null,"PRINCIPALE","kg");variation.validerRegle(Instant.now());variation.tracerValidation(null,"DEMONSTRATION");variation.activerRegle();repository.save(variation);
        }
    }
    private void seuil(String name, TypeMesure type, String component, String op, String threshold,
                       NiveauPriorite priority, String context) {
        String nom = "DÉMO - " + name;
        if (repository.findAll().stream().anyMatch(r -> r.getNom().equals(nom))) return;
        RegleAlerte regle = new RegleAlerte(nom, "Valeur indicative du cahier des charges ; démonstration uniquement, à valider médicalement.",
                type, op, new BigDecimal(threshold), null, 1, 1, priority);
        regle.definirPortee(null, null, component, null);
        regle.definirContexte(context);
        regle.validerRegle(Instant.now()); regle.tracerValidation(null,"DEMONSTRATION"); regle.activerRegle();
        repository.save(regle);
    }
}
