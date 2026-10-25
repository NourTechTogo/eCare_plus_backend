package com.nourtech.ecareplus.messaging.infrastructure.persistence;
import com.nourtech.ecareplus.messaging.domain.entity.MessageConversation; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface MessageConversationRepository extends JpaRepository<MessageConversation,UUID>{Page<MessageConversation> findAllByConversationId(UUID id,Pageable p);
java.util.Optional<MessageConversation> findFirstByConversationIdOrderByEnvoyeLeDescIdDesc(UUID id);
long countByConversationIdAndExpediteurIdNotAndLuLeIsNull(UUID id, UUID utilisateurId);
}
