package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.StatutRapport;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "rapport_suivi", uniqueConstraints=@jakarta.persistence.UniqueConstraint(name="uq_rapport_synthese_source",columnNames={"dossier_id","source_ref_synthese"}), indexes = {
        @Index(name = "idx_rapport_dossier", columnList = "dossier_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RapportSuivi extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSuivi dossier;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(columnDefinition = "text")
    private String resume;

    @Column(nullable = false)
    private Instant dateGeneration;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(length = 500)
    private String cheminFichier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRapport statut;

    @Column(columnDefinition="text",updatable=false) private String contenuSynthese;
    @Column(updatable=false) private java.util.UUID sourceRefSynthese;
    @Column(length=64,updatable=false) private String empreinteSynthese;
    @Column(length=80,updatable=false) private String fuseauSynthese;

    public void publierSynthese(java.util.UUID reference,String empreinte,String fuseau,String contenu){
        if(statut!=StatutRapport.EN_COURS||contenuSynthese!=null||!"JSON".equals(format))throw new IllegalStateException("Cette synthèse ne peut plus être modifiée");
        sourceRefSynthese=Objects.requireNonNull(reference);empreinteSynthese=obligatoire(empreinte,"empreinte");fuseauSynthese=obligatoire(fuseau,"fuseau");contenuSynthese=obligatoire(contenu,"contenu");statut=StatutRapport.GENERE;
    }

    public RapportSuivi(DossierSuivi dossier, String titre, LocalDate dateDebut, LocalDate dateFin, String resume, String format) {
        this.dossier = Objects.requireNonNull(dossier, "dossier");
        this.titre = obligatoire(titre, "titre");
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.dateFin = Objects.requireNonNull(dateFin, "dateFin");
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }
        this.resume = normaliser(resume);
        this.format = obligatoire(format, "format");
        this.dateGeneration = Instant.now();
        this.statut = StatutRapport.EN_COURS;
    }

    public void marquerGenere(String cheminFichier) {
        if (statut != StatutRapport.EN_COURS) {
            throw new IllegalStateException("Le rapport n'est pas en cours de génération");
        }
        this.cheminFichier = obligatoire(cheminFichier, "cheminFichier");
        this.statut = StatutRapport.GENERE;
    }

    public void archiver() {
        if (statut != StatutRapport.GENERE) {
            throw new IllegalStateException("Seul un rapport généré peut être archivé");
        }
        this.statut = StatutRapport.ARCHIVE;
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
