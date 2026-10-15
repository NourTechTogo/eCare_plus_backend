-- Previously verified tokens used BCrypt and must be requested again after this upgrade.
UPDATE demande_reinitialisation SET utilisee = TRUE
WHERE verifiee = TRUE AND utilisee = FALSE;

CREATE UNIQUE INDEX uk_reinitialisation_jeton_actif
    ON demande_reinitialisation (jeton_hash)
    WHERE jeton_hash IS NOT NULL AND utilisee = FALSE;
