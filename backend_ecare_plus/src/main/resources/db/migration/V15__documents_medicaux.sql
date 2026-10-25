CREATE TABLE document_medical (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 patient_id UUID NOT NULL REFERENCES patient(id), auteur_id UUID NOT NULL REFERENCES utilisateur(id),
 type_document VARCHAR(40) NOT NULL, titre VARCHAR(200) NOT NULL, date_document DATE NOT NULL,
 nom_original VARCHAR(255) NOT NULL, type_mime VARCHAR(60) NOT NULL, taille_octets BIGINT NOT NULL CHECK (taille_octets > 0),
 cle_stockage VARCHAR(80) NOT NULL UNIQUE, source_ref VARCHAR(128) NOT NULL, empreinte VARCHAR(64) NOT NULL,
 supprime_le TIMESTAMPTZ, fichier_supprime BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT uq_document_patient_source UNIQUE(patient_id, source_ref)
);
CREATE INDEX idx_document_patient_actif ON document_medical(patient_id, date_creation DESC) WHERE supprime_le IS NULL;
