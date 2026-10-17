package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "medicament")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medicament extends AbstractEntity {

    @Column(nullable = false, length = 150)
    private String nomCommercial;

    @Column(nullable = false, length = 200)
    private String denomination;

    @Column(nullable = false, length = 60)
    private String forme;

    @Column(nullable = false, length = 40)
    private String dosage;

    @Column(nullable = false, length = 20)
    private String uniteDosage;

    public Medicament(String nomCommercial, String denomination, String forme, String dosage, String uniteDosage) {
        this.nomCommercial = obligatoire(nomCommercial, "nomCommercial");
        this.denomination = obligatoire(denomination, "denomination");
        this.forme = obligatoire(forme, "forme");
        this.dosage = obligatoire(dosage, "dosage");
        this.uniteDosage = obligatoire(uniteDosage, "uniteDosage");
    }

    public void modifier(String nomCommercial, String denomination, String forme, String dosage, String uniteDosage) {
        this.nomCommercial = obligatoire(nomCommercial, "nomCommercial");
        this.denomination = obligatoire(denomination, "denomination");
        this.forme = obligatoire(forme, "forme");
        this.dosage = obligatoire(dosage, "dosage");
        this.uniteDosage = obligatoire(uniteDosage, "uniteDosage");
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }
}
