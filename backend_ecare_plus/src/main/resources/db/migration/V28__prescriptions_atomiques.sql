CREATE TABLE prescription_operation (
 id UUID PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 date_creation TIMESTAMPTZ NOT NULL,
 date_modification TIMESTAMPTZ NOT NULL,
 acteur_id UUID NOT NULL REFERENCES utilisateur(id),
 patient_id UUID NOT NULL REFERENCES patient(id),
 source_ref VARCHAR(128) NOT NULL,
 empreinte VARCHAR(64) NOT NULL,
 resultat TEXT NOT NULL,
 CONSTRAINT uq_prescription_operation UNIQUE(acteur_id,patient_id,source_ref)
);
