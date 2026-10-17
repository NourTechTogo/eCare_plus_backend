CREATE TABLE structure_sante (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    nom VARCHAR(200) NOT NULL,
    type_structure VARCHAR(80) NOT NULL,
    adresse VARCHAR(300) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    telephone VARCHAR(30) NOT NULL,
    email VARCHAR(254),
    statut VARCHAR(20) NOT NULL,
    numero_autorisation VARCHAR(100) NOT NULL UNIQUE,
    date_adhesion DATE NOT NULL,
    CONSTRAINT ck_structure_statut CHECK (statut IN ('EN_ATTENTE', 'ACTIVE', 'SUSPENDUE', 'FERMEE'))
);

CREATE TABLE utilisateur (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenoms VARCHAR(150) NOT NULL,
    sexe VARCHAR(20) NOT NULL,
    date_naissance DATE,
    telephone VARCHAR(30) UNIQUE,
    email VARCHAR(254) UNIQUE,
    mot_de_passe_hash VARCHAR(100) NOT NULL,
    langue_preferee VARCHAR(10) NOT NULL,
    statut_compte VARCHAR(20) NOT NULL,
    derniere_connexion TIMESTAMPTZ,
    CONSTRAINT ck_utilisateur_contact CHECK (telephone IS NOT NULL OR email IS NOT NULL),
    CONSTRAINT ck_utilisateur_statut CHECK (statut_compte IN ('EN_ATTENTE', 'ACTIF', 'SUSPENDU', 'DESACTIVE'))
);

CREATE TABLE patient (
    id UUID PRIMARY KEY REFERENCES utilisateur(id) ON DELETE CASCADE,
    numero_patient VARCHAR(40) NOT NULL UNIQUE,
    groupe_sanguin VARCHAR(10),
    adresse VARCHAR(300),
    personne_a_contacter VARCHAR(200),
    telephone_urgence VARCHAR(30),
    code_pin_hash VARCHAR(100),
    mode_connexion_prefere VARCHAR(30) NOT NULL,
    date_inscription DATE NOT NULL,
    statut_suivi VARCHAR(20) NOT NULL,
    CONSTRAINT ck_patient_statut_suivi CHECK (statut_suivi IN ('EN_ATTENTE', 'ACTIF', 'SUSPENDU', 'TERMINE'))
);

CREATE TABLE proche_aidant (
    id UUID PRIMARY KEY REFERENCES utilisateur(id) ON DELETE CASCADE,
    lien_avec_patient VARCHAR(100) NOT NULL,
    telephone_secondaire VARCHAR(30),
    statut_autorisation VARCHAR(30) NOT NULL
);

CREATE TABLE professionnel_sante (
    id UUID PRIMARY KEY REFERENCES utilisateur(id) ON DELETE CASCADE,
    numero_professionnel VARCHAR(60) NOT NULL UNIQUE,
    specialite VARCHAR(150) NOT NULL,
    fonction VARCHAR(100) NOT NULL,
    statut_professionnel VARCHAR(30) NOT NULL,
    date_prise_fonction DATE NOT NULL,
    structure_id UUID NOT NULL REFERENCES structure_sante(id)
);

CREATE INDEX idx_professionnel_structure ON professionnel_sante(structure_id);

CREATE TABLE medecin (
    id UUID PRIMARY KEY REFERENCES professionnel_sante(id) ON DELETE CASCADE,
    numero_ordre VARCHAR(80) NOT NULL UNIQUE,
    domaine_specialisation VARCHAR(150) NOT NULL
);

CREATE TABLE infirmier (
    id UUID PRIMARY KEY REFERENCES professionnel_sante(id) ON DELETE CASCADE,
    service VARCHAR(100) NOT NULL,
    niveau_responsabilite VARCHAR(80) NOT NULL
);

CREATE TABLE administrateur_structure (
    id UUID PRIMARY KEY REFERENCES utilisateur(id) ON DELETE CASCADE,
    fonction_administrative VARCHAR(120) NOT NULL,
    niveau_acces VARCHAR(50) NOT NULL,
    structure_id UUID NOT NULL REFERENCES structure_sante(id)
);

CREATE INDEX idx_administrateur_structure ON administrateur_structure(structure_id);

CREATE TABLE affectation_patient (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    professionnel_id UUID NOT NULL REFERENCES professionnel_sante(id),
    structure_id UUID NOT NULL REFERENCES structure_sante(id),
    date_debut DATE NOT NULL,
    date_fin DATE,
    role_professionnel VARCHAR(40) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    professionnel_principal BOOLEAN NOT NULL,
    CONSTRAINT ck_affectation_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_affectation_statut CHECK (statut IN ('ACTIVE', 'SUSPENDUE', 'TERMINEE'))
);

CREATE INDEX idx_affectation_patient_statut ON affectation_patient(patient_id, statut);
CREATE INDEX idx_affectation_professionnel_statut ON affectation_patient(professionnel_id, statut);
CREATE INDEX idx_affectation_structure ON affectation_patient(structure_id);

CREATE TABLE dossier_suivi (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    structure_id UUID NOT NULL REFERENCES structure_sante(id),
    date_ouverture DATE NOT NULL,
    statut VARCHAR(20) NOT NULL,
    resume_medical TEXT,
    frequence_suivi VARCHAR(100),
    prochain_controle DATE,
    date_derniere_mise_ajour TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_dossier_statut CHECK (statut IN ('ACTIF', 'SUSPENDU', 'FERME'))
);

CREATE INDEX idx_dossier_patient_structure ON dossier_suivi(patient_id, structure_id);

-- Un seul dossier actif est autorisé par patient et par structure.
CREATE UNIQUE INDEX uk_dossier_actif_patient_structure
    ON dossier_suivi(patient_id, structure_id)
    WHERE statut = 'ACTIF';

CREATE TABLE objectif_suivi (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    dossier_id UUID NOT NULL REFERENCES dossier_suivi(id),
    medecin_id UUID NOT NULL REFERENCES medecin(id),
    libelle VARCHAR(200) NOT NULL,
    description TEXT,
    valeur_cible_min NUMERIC(12, 3),
    valeur_cible_max NUMERIC(12, 3),
    unite VARCHAR(30),
    date_debut DATE NOT NULL,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL,
    CONSTRAINT ck_objectif_cibles CHECK (
        valeur_cible_min IS NULL OR valeur_cible_max IS NULL OR valeur_cible_min <= valeur_cible_max
    ),
    CONSTRAINT ck_objectif_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_objectif_statut CHECK (statut IN ('PLANIFIE', 'ACTIF', 'ATTEINT', 'NON_ATTEINT', 'ANNULE'))
);

CREATE INDEX idx_objectif_dossier_statut ON objectif_suivi(dossier_id, statut);
CREATE INDEX idx_objectif_medecin ON objectif_suivi(medecin_id);

CREATE TABLE consentement (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    type_consentement VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    date_acceptation TIMESTAMPTZ NOT NULL,
    date_retrait TIMESTAMPTZ,
    statut VARCHAR(20) NOT NULL,
    version_document VARCHAR(40) NOT NULL,
    adresse_ip VARCHAR(45) NOT NULL,
    CONSTRAINT ck_consentement_dates CHECK (date_retrait IS NULL OR date_retrait >= date_acceptation),
    CONSTRAINT ck_consentement_statut CHECK (statut IN ('ACCORDE', 'RETIRE', 'EXPIRE'))
);

CREATE INDEX idx_consentement_patient_statut ON consentement(patient_id, statut);

CREATE TABLE autorisation_partage (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    proche_aidant_id UUID NOT NULL REFERENCES proche_aidant(id),
    consentement_id UUID NOT NULL REFERENCES consentement(id),
    type_donnee VARCHAR(40) NOT NULL,
    date_debut TIMESTAMPTZ NOT NULL,
    date_fin TIMESTAMPTZ,
    peut_consulter BOOLEAN NOT NULL,
    peut_recevoir_notification BOOLEAN NOT NULL,
    statut VARCHAR(20) NOT NULL,
    CONSTRAINT ck_autorisation_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_autorisation_statut CHECK (statut IN ('EN_ATTENTE', 'ACTIVE', 'RETIREE', 'EXPIREE'))
);

CREATE INDEX idx_autorisation_patient_statut ON autorisation_partage(patient_id, statut);
CREATE INDEX idx_autorisation_proche_statut ON autorisation_partage(proche_aidant_id, statut);
CREATE INDEX idx_autorisation_consentement ON autorisation_partage(consentement_id);

CREATE TABLE journal_activite (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    utilisateur_id UUID REFERENCES utilisateur(id),
    action VARCHAR(120) NOT NULL,
    ressource VARCHAR(120) NOT NULL,
    ressource_id UUID,
    date_action TIMESTAMPTZ NOT NULL,
    adresse_ip VARCHAR(45),
    appareil VARCHAR(300),
    resultat VARCHAR(40) NOT NULL
);

CREATE INDEX idx_journal_utilisateur_date ON journal_activite(utilisateur_id, date_action);
CREATE INDEX idx_journal_ressource ON journal_activite(ressource, ressource_id);

CREATE TABLE element_synchronisation (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    utilisateur_id UUID NOT NULL REFERENCES utilisateur(id),
    type_ressource VARCHAR(80) NOT NULL,
    identifiant_appareil VARCHAR(100) NOT NULL,
    identifiant_local VARCHAR(100) NOT NULL,
    contenu_local JSONB NOT NULL,
    date_creation_locale TIMESTAMPTZ NOT NULL,
    nombre_tentatives INTEGER NOT NULL,
    statut VARCHAR(20) NOT NULL,
    message_erreur TEXT,
    CONSTRAINT uk_sync_utilisateur_appareil_local UNIQUE (
        utilisateur_id,
        identifiant_appareil,
        identifiant_local
    ),
    CONSTRAINT ck_sync_tentatives CHECK (nombre_tentatives >= 0),
    CONSTRAINT ck_sync_statut CHECK (statut IN ('RECU', 'EN_COURS', 'SYNCHRONISE', 'EN_ECHEC'))
);

CREATE INDEX idx_sync_statut ON element_synchronisation(statut, date_creation_locale);

CREATE TABLE abonnement_structure (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    structure_id UUID NOT NULL REFERENCES structure_sante(id),
    formule VARCHAR(80) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_patients_autorises INTEGER NOT NULL,
    montant NUMERIC(14, 2) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    frequence_paiement VARCHAR(20) NOT NULL,
    CONSTRAINT ck_abonnement_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_abonnement_limite CHECK (nombre_patients_autorises > 0),
    CONSTRAINT ck_abonnement_montant CHECK (montant >= 0),
    CONSTRAINT ck_abonnement_statut CHECK (statut IN ('EN_ATTENTE', 'ACTIF', 'SUSPENDU', 'EXPIRE', 'RESILIE'))
);

CREATE INDEX idx_abonnement_structure_statut ON abonnement_structure(structure_id, statut);
