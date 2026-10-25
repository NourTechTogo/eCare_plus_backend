package com.nourtech.ecareplus.care.infrastructure.persistence;
import com.nourtech.ecareplus.care.domain.entity.NoteCiblee;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface NoteCibleeRepository extends JpaRepository<NoteCiblee,UUID>{
    @Query("select n from NoteCiblee n where n.typeCible=:type and n.cibleId=:cible and n.supprimeLe is null and (n.auteurId=:acteur or (n.visibilite='PARTAGEE' and (:patientActeur=true or n.auteurId=:patient)))")
    Page<NoteCiblee> visibles(String type,UUID cible,UUID acteur,UUID patient,boolean patientActeur,Pageable page);
}
