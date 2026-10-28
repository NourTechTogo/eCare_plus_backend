package com.nourtech.ecareplus;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class ValidationRegleHistoriqueTests {
    @Test void tousLesChangementsInvalidantsEffacentAussiLaProvenance(){
        for(int chemin=0;chemin<3;chemin++){
            var r=new RegleAlerte("Exemple fictif",null,TypeMesure.POIDS,">=",BigDecimal.TEN,null,1,60,NiveauPriorite.ELEVE);
            r.validerRegle(Instant.now());r.tracerValidation(UUID.randomUUID(),"MEDECIN");r.activerRegle();
            switch(chemin){case 0->r.modifierRegle(null,">=",new BigDecimal("20"),null,1,60);case 1->r.definirContexte("test");case 2->r.definirPortee(null,null,"PRINCIPALE","kg");}
            assertThat(r.isActif()).isFalse();assertThat(r.getDateValidation()).isNull();assertThat(r.getValidateurId()).isNull();assertThat(r.getSourceValidation()).isNull();
            assertThatIllegalStateException().isThrownBy(r::activerRegle);
        }
    }
}
