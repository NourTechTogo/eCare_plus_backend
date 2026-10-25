package com.nourtech.ecareplus.document.infrastructure.persistence;
import com.nourtech.ecareplus.document.domain.entity.DocumentMedical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface DocumentMedicalRepository extends JpaRepository<DocumentMedical,UUID>{
    boolean existsByCleStockage(String cleStockage);
    Optional<DocumentMedical> findByPatientIdAndSourceRef(UUID patientId,String sourceRef);
    Page<DocumentMedical> findByPatientIdAndSupprimeLeIsNull(UUID patientId,Pageable page);
    List<DocumentMedical> findTop100BySupprimeLeIsNotNullAndFichierSupprimeFalse();
}
