CREATE TABLE pathologie (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    dossier_id UUID NOT NULL REFERENCES dossier_suivi(id),
    nom VARCHAR(150) NOT NULL,
    code VARCHAR(40),
    description TEXT,
    date_diagnostic_declaree DATE,
    statut VARCHAR(20) NOT NULL,
    niveau_risque VARCHAR(20),
    CONSTRAINT ck_pathologie_statut CHECK (statut IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_pathologie_niveau_risque CHECK (niveau_risque IS NULL OR niveau_risque IN ('FAIBLE', 'MODERE', 'ELEVE'))
);

CREATE INDEX idx_pathologie_dossier ON pathologie(dossier_id);

CREATE TABLE mesure_sante (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    dossier_id UUID NOT NULL REFERENCES dossier_suivi(id),
    type_mesure VARCHAR(30) NOT NULL,
    valeur_principale NUMERIC(10, 3) NOT NULL,
    valeur_secondaire NUMERIC(10, 3),
    unite VARCHAR(20) NOT NULL,
    date_mesure TIMESTAMPTZ NOT NULL,
    contexte_mesure VARCHAR(100),
    commentaire TEXT,
    source VARCHAR(30) NOT NULL,
    statut_synchronisation VARCHAR(30),
    valeur_confirmee BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_mesure_type CHECK (type_mesure IN ('TENSION_ARTERIELLE', 'GLYCEMIE', 'POIDS', 'FREQUENCE_CARDIAQUE')),
    CONSTRAINT ck_mesure_source CHECK (source IN ('PATIENT', 'PROCHE_AIDANT', 'PROFESSIONNEL', 'APPAREIL_CONNECTE'))
);

CREATE INDEX idx_mesure_patient_date ON mesure_sante(patient_id, date_mesure);
CREATE INDEX idx_mesure_dossier ON mesure_sante(dossier_id);
