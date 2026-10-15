package com.nourtech.ecareplus.alert.infrastructure.persistence;

import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegleAlerteRepository extends JpaRepository<RegleAlerte, UUID> {

    List<RegleAlerte> findAllByActif(boolean actif);

    List<RegleAlerte> findAllByTypeMesureAndActifTrue(TypeMesure typeMesure);
}
