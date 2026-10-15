package com.nourtech.ecareplus.auth.infrastructure.security;

import com.nourtech.ecareplus.identity.domain.entity.AdministrateurStructure;
import com.nourtech.ecareplus.identity.domain.entity.Infirmier;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class JwtService {

    private final Duration ttl;
    private final Clock clock;
    private final SecretKey secretKey;
    private final JwtEncoder encoder;

    public JwtService(String secret, Duration ttl, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("La clé JWT doit contenir au moins 32 octets");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("La durée de vie JWT doit être positive");
        }
        this.ttl = ttl;
        this.clock = clock;
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    public JetonEmis emettre(Utilisateur utilisateur) {
        Instant maintenant = clock.instant();
        Instant expiration = maintenant.plus(ttl);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("ecare-plus")
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .subject(utilisateur.getId().toString())
                .claim("role", role(utilisateur));
        if (utilisateur.getEmail() != null) {
            claims.claim("email", utilisateur.getEmail());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String valeur = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new JetonEmis(valeur, expiration);
    }

    public JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private static String role(Utilisateur utilisateur) {
        return switch (utilisateur) {
            case AdministrateurStructure ignored -> "ADMINISTRATEUR_STRUCTURE";
            case Medecin ignored -> "MEDECIN";
            case Infirmier ignored -> "INFIRMIER";
            case Patient ignored -> "PATIENT";
            case ProcheAidant ignored -> "PROCHE_AIDANT";
            default -> throw new IllegalArgumentException("Type d'utilisateur non pris en charge");
        };
    }

    public record JetonEmis(String jeton, Instant expireLe) {
    }
}
