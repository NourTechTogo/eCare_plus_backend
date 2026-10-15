-- Distingue les alertes issues du moteur de règles des alertes créées manuellement.
ALTER TABLE alerte
    ADD COLUMN origine VARCHAR(20) NOT NULL DEFAULT 'MANUELLE';

ALTER TABLE alerte
    ADD CONSTRAINT ck_alerte_origine CHECK (origine IN ('AUTOMATIQUE', 'MANUELLE'));

ALTER TABLE alerte
    ALTER COLUMN origine DROP DEFAULT;

-- Une mesure donnée ne doit déclencher qu'une alerte par règle, même en cas de retry.
CREATE UNIQUE INDEX uk_alerte_regle_mesure
    ON alerte(regle_id, mesure_id)
    WHERE regle_id IS NOT NULL AND mesure_id IS NOT NULL;

-- Convertit l'ancienne fréquence libre en récurrence exploitable par le backend.
UPDATE rappel
SET frequence = CASE UPPER(COALESCE(frequence, ''))
    WHEN 'QUOTIDIEN' THEN 'QUOTIDIEN'
    WHEN 'HEBDOMADAIRE' THEN 'HEBDOMADAIRE'
    WHEN 'MENSUELLE' THEN 'MENSUELLE'
    ELSE 'AUCUNE'
END;

ALTER TABLE rappel
    ALTER COLUMN frequence TYPE VARCHAR(20),
    ALTER COLUMN frequence SET NOT NULL,
    ALTER COLUMN frequence SET DEFAULT 'AUCUNE',
    ADD COLUMN derniere_execution TIMESTAMPTZ;

ALTER TABLE rappel DROP CONSTRAINT ck_rappel_statut;
ALTER TABLE rappel
    ADD CONSTRAINT ck_rappel_statut
        CHECK (statut IN ('PLANIFIE', 'ENVOYE', 'ECHEC', 'ANNULE', 'LU', 'TERMINE'));
ALTER TABLE rappel
    ADD CONSTRAINT ck_rappel_frequence
        CHECK (frequence IN ('AUCUNE', 'QUOTIDIEN', 'HEBDOMADAIRE', 'MENSUELLE'));
ALTER TABLE rappel
    ADD CONSTRAINT ck_rappel_dates
        CHECK (date_fin IS NULL OR date_fin >= date_heure_prevue);

CREATE INDEX idx_rappel_echeance
    ON rappel(statut, date_heure_prevue);
