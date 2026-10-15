package com.nourtech.ecareplus.messaging.infrastructure.persistence;
import com.nourtech.ecareplus.messaging.domain.entity.Conversation; import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import org.springframework.data.jpa.repository.EntityGraph;
public interface ConversationRepository extends JpaRepository<Conversation,UUID>{
 Optional<Conversation> findByPatientIdAndProfessionnelId(UUID p,UUID pro);
 @EntityGraph(attributePaths = {"patient", "professionnel"})
 List<Conversation> findAllByPatientIdOrProfessionnelId(UUID p,UUID pro);
}
