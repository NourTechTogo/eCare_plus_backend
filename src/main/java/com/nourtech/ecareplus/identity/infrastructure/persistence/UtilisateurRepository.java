package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByTelephone(String telephone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTelephone(String telephone);
}
