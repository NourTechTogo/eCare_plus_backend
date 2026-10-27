-- Preserve every institutional dossier; add an independent personal space.
ALTER TABLE dossier_suivi ALTER COLUMN structure_id DROP NOT NULL;

CREATE UNIQUE INDEX uk_dossier_personnel_actif
    ON dossier_suivi(patient_id) WHERE structure_id IS NULL AND statut = 'ACTIF';

INSERT INTO dossier_suivi
    (id, version, date_creation, date_modification, patient_id, structure_id,
     date_ouverture, statut, date_derniere_mise_ajour)
SELECT gen_random_uuid(), 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, p.id, NULL,
       CURRENT_DATE, 'ACTIF', CURRENT_TIMESTAMP
FROM patient p
WHERE NOT EXISTS (SELECT 1 FROM dossier_suivi d
                  WHERE d.patient_id = p.id AND d.structure_id IS NULL AND d.statut = 'ACTIF');
