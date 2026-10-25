package com.nourtech.ecareplus.messaging.infrastructure.persistence;
import com.nourtech.ecareplus.messaging.domain.entity.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.Instant;
public interface PieceJointeRepository extends JpaRepository<PieceJointe,UUID>{
    @org.springframework.data.jpa.repository.Query("select p from PieceJointe p where p.conversation.id=:conversation and p.supprimee=false and (p.message is not null or p.auteurId=:acteur)")
    org.springframework.data.domain.Page<PieceJointe> visibles(UUID conversation,UUID acteur,org.springframework.data.domain.Pageable page);
    boolean existsByCleStockage(String cleStockage);
    Optional<PieceJointe> findByAuteurIdAndSourceRef(UUID auteur,String sourceRef);
    List<PieceJointe> findTop100ByMessageIsNullAndSupprimeeFalseAndDateCreationBefore(Instant date);
}
