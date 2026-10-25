package com.nourtech.ecareplus.care.infrastructure.persistence;
import com.nourtech.ecareplus.care.domain.entity.ReponsePriseOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReponsePriseOperationRepository extends JpaRepository<ReponsePriseOperation,UUID>{Optional<ReponsePriseOperation> findByActeurIdAndSourceRef(UUID acteur,String ref);}
