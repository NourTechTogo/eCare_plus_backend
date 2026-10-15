CREATE TABLE declaration_symptome (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    symptome VARCHAR(150) NOT NULL,
    intensite VARCHAR(30) NOT NULL,
    description TEXT,
    date_apparition TIMESTAMPTZ,
    date_declaration TIMESTAMPTZ NOT NULL,
    persistant BOOLEAN NOT NULL,
    statut_evaluation VARCHAR(20) NOT NULL,
    CONSTRAINT ck_declaration_statut CHECK (statut_evaluation IN ('NON_EVALUE', 'TRANSMIS', 'EVALUE'))
);

CREATE INDEX idx_declaration_patient ON declaration_symptome(patient_id);

CREATE TABLE observation_suivi (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    dossier_id UUID NOT NULL REFERENCES dossier_suivi(id),
    professionnel_id UUID NOT NULL REFERENCES professionnel_sante(id),
    contenu TEXT NOT NULL,
    type_observation VARCHAR(50) NOT NULL,
    confidentielle BOOLEAN NOT NULL,
    archivee BOOLEAN NOT NULL
);

CREATE INDEX idx_observation_dossier ON observation_suivi(dossier_id);

CREATE TABLE rapport_suivi (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    dossier_id UUID NOT NULL REFERENCES dossier_suivi(id),
    titre VARCHAR(200) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    resume TEXT,
    date_generation TIMESTAMPTZ NOT NULL,
    format VARCHAR(20) NOT NULL,
    chemin_fichier VARCHAR(500),
    statut VARCHAR(20) NOT NULL,
    CONSTRAINT ck_rapport_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_rapport_statut CHECK (statut IN ('EN_COURS', 'GENERE', 'ARCHIVE'))
);

CREATE INDEX idx_rapport_dossier ON rapport_suivi(dossier_id);

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id),
    titre VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type_notification VARCHAR(50) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    date_envoi TIMESTAMPTZ,
    date_lecture TIMESTAMPTZ,
    statut_envoi VARCHAR(20) NOT NULL,
    sensible BOOLEAN NOT NULL,
    CONSTRAINT ck_notification_canal CHECK (canal IN ('PUSH', 'SMS', 'EMAIL')),
    CONSTRAINT ck_notification_statut CHECK (statut_envoi IN ('EN_ATTENTE', 'ENVOYEE', 'ECHEC', 'LUE', 'ANNULEE'))
);

CREATE INDEX idx_notification_utilisateur ON notification(utilisateur_id);

CREATE TABLE contenu_educatif (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    auteur_id UUID NOT NULL REFERENCES medecin(id),
    titre VARCHAR(200) NOT NULL,
    resume VARCHAR(500),
    contenu TEXT NOT NULL,
    type_contenu VARCHAR(50) NOT NULL,
    langue VARCHAR(10) NOT NULL,
    pathologie_cible VARCHAR(150),
    statut_validation VARCHAR(20) NOT NULL,
    date_publication TIMESTAMPTZ,
    url_media VARCHAR(500),
    CONSTRAINT ck_contenu_statut CHECK (statut_validation IN ('BROUILLON', 'VALIDE', 'PUBLIE', 'ARCHIVE'))
);
