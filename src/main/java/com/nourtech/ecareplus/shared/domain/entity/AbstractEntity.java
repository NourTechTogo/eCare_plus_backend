package com.nourtech.ecareplus.shared.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Version
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant dateCreation;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant dateModification;

    /**
     * Compare l'identité persistante sans dépendre de la classe de proxy Hibernate.
     */
    public final boolean memeIdentiteQue(AbstractEntity autre) {
        return this == autre || (autre != null && id != null && id.equals(autre.id));
    }
}
