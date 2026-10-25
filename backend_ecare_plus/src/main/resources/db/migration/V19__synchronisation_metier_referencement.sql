CREATE TABLE operation_synchronisation (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 utilisateur_id UUID NOT NULL REFERENCES utilisateur(id), identifiant_appareil VARCHAR(128) NOT NULL, type_operation VARCHAR(40) NOT NULL,
 identifiant_local VARCHAR(128) NOT NULL, empreinte VARCHAR(64) NOT NULL, resultat TEXT NOT NULL,
 UNIQUE(utilisateur_id,identifiant_appareil,type_operation,identifiant_local)
);
CREATE TABLE jeton_referencement (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 patient_id UUID NOT NULL REFERENCES patient(id), empreinte VARCHAR(64) NOT NULL UNIQUE, expire_le TIMESTAMPTZ NOT NULL,
 consomme_le TIMESTAMPTZ, revoque BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_jeton_referencement_patient ON jeton_referencement(patient_id);
