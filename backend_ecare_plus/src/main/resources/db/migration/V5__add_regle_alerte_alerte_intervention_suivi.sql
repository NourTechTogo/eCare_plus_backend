CREATE TABLE regle_alerte (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    type_mesure VARCHAR(30) NOT NULL,
    operateur VARCHAR(20) NOT NULL,
    seuil_min NUMERIC(10, 3),
    seuil_max NUMERIC(10, 3),
    nombre_occurrences INTEGER NOT NULL,
    periode_observation INTEGER NOT NULL,
    niveau_priorite VARCHAR(20) NOT NULL,
    actif BOOLEAN NOT NULL,
    date_validation TIMESTAMPTZ,
    CONSTRAINT ck_regle_type_mesure CHECK (type_mesure IN ('TENSION_ARTERIELLE', 'GLYCEMIE', 'POIDS', 'FREQUENCE_CARDIAQUE')),
    CONSTRAINT ck_regle_operateur CHECK (operateur IN ('>', '>=', '<', '<=', 'HORS_PLAGE')),
    CONSTRAINT ck_regle_occurrences CHECK (nombre_occurrences > 0),
    CONSTRAINT ck_regle_periode CHECK (periode_observation > 0),
    CONSTRAINT ck_regle_niveau_priorite CHECK (niveau_priorite IN ('FAIBLE', 'MOYEN', 'ELEVE', 'CRITIQUE'))
);

CREATE TABLE alerte (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    patient_id UUID NOT NULL REFERENCES patient(id),
    mesure_id UUID REFERENCES mesure_sante(id),
    regle_id UUID REFERENCES regle_alerte(id),
    professionnel_id UUID REFERENCES professionnel_sante(id),
    type_alerte VARCHAR(30) NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    niveau_priorite VARCHAR(20) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    date_prise_en_charge TIMESTAMPTZ,
    date_cloture TIMESTAMPTZ,
    motif_cloture TEXT,
    CONSTRAINT ck_alerte_type CHECK (type_alerte IN ('SEUIL_MESURE', 'ABSENCE_MESURE', 'AUTRE')),
    CONSTRAINT ck_alerte_niveau_priorite CHECK (niveau_priorite IN ('FAIBLE', 'MOYEN', 'ELEVE', 'CRITIQUE')),
    CONSTRAINT ck_alerte_statut CHECK (statut IN ('NOUVELLE', 'PRISE_EN_CHARGE', 'TRANSMISE', 'CLOTUREE'))
);

CREATE INDEX idx_alerte_patient_statut ON alerte(patient_id, statut);
CREATE INDEX idx_alerte_professionnel ON alerte(professionnel_id);

CREATE TABLE intervention_suivi (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL,
    date_modification TIMESTAMPTZ NOT NULL,
    alerte_id UUID NOT NULL REFERENCES alerte(id),
    type_intervention VARCHAR(100) NOT NULL,
    description TEXT,
    date_intervention TIMESTAMPTZ NOT NULL,
    resultat TEXT,
    action_suivante TEXT,
    date_prochaine_action TIMESTAMPTZ
);

CREATE INDEX idx_intervention_alerte ON intervention_suivi(alerte_id);
