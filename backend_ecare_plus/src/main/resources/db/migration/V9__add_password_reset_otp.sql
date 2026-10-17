CREATE TABLE demande_reinitialisation (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    otp_hash VARCHAR(100) NOT NULL,
    expire_le TIMESTAMPTZ NOT NULL,
    nombre_essais INTEGER NOT NULL,
    verifiee BOOLEAN NOT NULL,
    utilisee BOOLEAN NOT NULL,
    jeton_hash VARCHAR(100)
);
CREATE INDEX idx_reinitialisation_utilisateur ON demande_reinitialisation(utilisateur_id, date_creation DESC);
