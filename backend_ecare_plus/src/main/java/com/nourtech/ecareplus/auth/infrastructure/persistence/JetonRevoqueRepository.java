package com.nourtech.ecareplus.auth.infrastructure.persistence;
import com.nourtech.ecareplus.auth.domain.entity.JetonRevoque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface JetonRevoqueRepository extends JpaRepository<JetonRevoque,UUID> { boolean existsByEmpreinte(String empreinte); }
