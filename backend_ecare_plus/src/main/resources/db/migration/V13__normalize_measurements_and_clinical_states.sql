-- Additive clinical integration migration. Existing data is retained.
ALTER TABLE mesure_sante DROP CONSTRAINT ck_mesure_type;
ALTER TABLE mesure_sante ADD CONSTRAINT ck_mesure_type
    CHECK (type_mesure IN ('TENSION_ARTERIELLE','GLYCEMIE','POIDS','FREQUENCE_CARDIAQUE','TEMPERATURE','SPO2'));
ALTER TABLE mesure_sante ADD COLUMN source_ref VARCHAR(100);
CREATE UNIQUE INDEX uk_mesure_patient_source_ref ON mesure_sante(patient_id,source_ref) WHERE source_ref IS NOT NULL;

-- Normalize only known glucose units; never guess the scale of unknown historical units.
UPDATE mesure_sante SET valeur_principale=ROUND(valeur_principale*100,3), unite='mg/dL'
    WHERE type_mesure='GLYCEMIE' AND LOWER(REPLACE(unite,' ',''))='g/l';
UPDATE mesure_sante SET valeur_principale=ROUND(valeur_principale*18.0182,3), unite='mg/dL'
    WHERE type_mesure='GLYCEMIE' AND LOWER(REPLACE(unite,' ',''))='mmol/l';
UPDATE mesure_sante SET unite='mg/dL'
    WHERE type_mesure='GLYCEMIE' AND LOWER(REPLACE(unite,' ',''))='mg/dl';

ALTER TABLE rappel ADD COLUMN derniere_lecture TIMESTAMPTZ;
ALTER TABLE regle_alerte ADD COLUMN patient_id UUID REFERENCES patient(id);
ALTER TABLE regle_alerte ADD COLUMN structure_id UUID REFERENCES structure_sante(id);
ALTER TABLE regle_alerte ADD COLUMN composante VARCHAR(20) NOT NULL DEFAULT 'PRINCIPALE';
ALTER TABLE regle_alerte ADD COLUMN unite VARCHAR(20);
ALTER TABLE regle_alerte ADD COLUMN contexte_mesure VARCHAR(100);
ALTER TABLE regle_alerte DROP CONSTRAINT ck_regle_type_mesure;
ALTER TABLE regle_alerte ADD CONSTRAINT ck_regle_type_mesure
    CHECK(type_mesure IN ('TENSION_ARTERIELLE','GLYCEMIE','POIDS','FREQUENCE_CARDIAQUE','TEMPERATURE','SPO2'));
ALTER TABLE regle_alerte ADD CONSTRAINT ck_regle_composante CHECK(composante IN ('PRINCIPALE','DIASTOLIQUE'));
CREATE INDEX idx_regle_patient ON regle_alerte(patient_id);
CREATE INDEX idx_regle_structure ON regle_alerte(structure_id);

-- Legacy rules have an unknown clinical unit/owner. Preserve them, but require revalidation
-- before using them on normalized measurements rather than silently changing their meaning.
UPDATE regle_alerte SET actif=FALSE, date_validation=NULL;

ALTER TABLE notification DROP CONSTRAINT ck_notification_canal;
ALTER TABLE notification ADD CONSTRAINT ck_notification_canal CHECK(canal IN ('PUSH','SMS','EMAIL','APPLICATION'));
ALTER TABLE rappel DROP CONSTRAINT ck_rappel_canal;
ALTER TABLE rappel ADD CONSTRAINT ck_rappel_canal CHECK(canal IN ('PUSH','SMS','EMAIL','APPLICATION'));
