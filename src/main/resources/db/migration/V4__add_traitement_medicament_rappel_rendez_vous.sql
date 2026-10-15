CREATE TABLE traitement (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    dossier_id UUID NOT NULL REFERENCES dossier_suivi(id),
    libelle VARCHAR(200) NOT NULL,
    description TEXT,
    date_debut DATE NOT NULL,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL,
    prescripteur VARCHAR(200),
    consignes_generales TEXT,
    CONSTRAINT ck_traitement_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_traitement_statut CHECK (statut IN ('ACTIF', 'SUSPENDU', 'TERMINE'))
);

CREATE INDEX idx_traitement_dossier ON traitement(dossier_id);

CREATE TABLE medicament (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    nom_commercial VARCHAR(150) NOT NULL,
    denomination VARCHAR(200) NOT NULL,
    forme VARCHAR(60) NOT NULL,
    dosage VARCHAR(40) NOT NULL,
    unite_dosage VARCHAR(20) NOT NULL
);

CREATE TABLE prise_medicament (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    traitement_id UUID NOT NULL REFERENCES traitement(id),
    medicament_id UUID NOT NULL REFERENCES medicament(id),
    quantite NUMERIC(10, 3) NOT NULL,
    unite VARCHAR(20) NOT NULL,
    frequence VARCHAR(100) NOT NULL,
    heure_prevue TIME NOT NULL,
    moment_prise VARCHAR(50),
    voie_administration VARCHAR(100),
    consigne TEXT,
    actif BOOLEAN NOT NULL,
    CONSTRAINT ck_prise_quantite CHECK (quantite > 0)
);

CREATE INDEX idx_prise_traitement ON prise_medicament(traitement_id);
CREATE INDEX idx_prise_medicament ON prise_medicament(medicament_id);

CREATE TABLE confirmation_prise (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    prise_medicament_id UUID NOT NULL REFERENCES prise_medicament(id),
    date_prevue TIMESTAMPTZ NOT NULL,
    date_confirmation TIMESTAMPTZ,
    statut VARCHAR(20) NOT NULL,
    commentaire TEXT,
    CONSTRAINT ck_confirmation_statut CHECK (statut IN ('ATTENTE', 'PRISE', 'OUBLIEE', 'REPORTEE'))
);

CREATE INDEX idx_confirmation_prise ON confirmation_prise(prise_medicament_id);

CREATE TABLE rappel (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    prise_medicament_id UUID REFERENCES prise_medicament(id),
    type_rappel VARCHAR(30) NOT NULL,
    titre VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    date_heure_prevue TIMESTAMPTZ NOT NULL,
    frequence VARCHAR(100),
    statut VARCHAR(20) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    date_fin TIMESTAMPTZ,
    CONSTRAINT ck_rappel_type CHECK (type_rappel IN ('PRISE_MEDICAMENT', 'RENDEZ_VOUS', 'MESURE', 'AUTRE')),
    CONSTRAINT ck_rappel_statut CHECK (statut IN ('PLANIFIE', 'ENVOYE', 'ECHEC', 'ANNULE', 'LU')),
    CONSTRAINT ck_rappel_canal CHECK (canal IN ('PUSH', 'SMS', 'EMAIL'))
);

CREATE INDEX idx_rappel_patient ON rappel(patient_id);
CREATE INDEX idx_rappel_prise_medicament ON rappel(prise_medicament_id);

CREATE TABLE rendez_vous (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    motif VARCHAR(200) NOT NULL,
    date_heure TIMESTAMPTZ NOT NULL,
    lieu VARCHAR(300),
    type_rendez_vous VARCHAR(60) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    commentaire TEXT,
    CONSTRAINT ck_rendez_vous_statut CHECK (statut IN ('PLANIFIE', 'CONFIRME', 'REPORTE', 'ANNULE', 'REALISE'))
);

CREATE INDEX idx_rendez_vous_patient ON rendez_vous(patient_id);
