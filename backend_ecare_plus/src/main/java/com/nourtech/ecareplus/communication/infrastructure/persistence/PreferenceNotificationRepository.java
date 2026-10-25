package com.nourtech.ecareplus.communication.infrastructure.persistence;
import com.nourtech.ecareplus.communication.domain.entity.PreferenceNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PreferenceNotificationRepository extends JpaRepository<PreferenceNotification,UUID>{}
