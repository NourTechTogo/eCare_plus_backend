-- Deliberately nullable: historical rules cannot be reconstructed reliably.
ALTER TABLE alerte ADD COLUMN capture_regle_version BIGINT;
ALTER TABLE alerte ADD COLUMN capture_regle_nom VARCHAR(150);
ALTER TABLE alerte ADD COLUMN capture_regle_type_mesure VARCHAR(30);
ALTER TABLE alerte ADD COLUMN capture_regle_operateur VARCHAR(20);
ALTER TABLE alerte ADD COLUMN capture_regle_seuil_min NUMERIC(10,3);
ALTER TABLE alerte ADD COLUMN capture_regle_seuil_max NUMERIC(10,3);
ALTER TABLE alerte ADD COLUMN capture_regle_nombre_occurrences INTEGER;
ALTER TABLE alerte ADD COLUMN capture_regle_periode_observation INTEGER;
ALTER TABLE alerte ADD COLUMN capture_regle_niveau_priorite VARCHAR(20);
ALTER TABLE alerte ADD COLUMN capture_regle_composante VARCHAR(20);
ALTER TABLE alerte ADD COLUMN capture_regle_unite VARCHAR(20);
ALTER TABLE alerte ADD COLUMN capture_regle_contexte_mesure VARCHAR(100);
ALTER TABLE alerte ADD COLUMN capture_regle_date_validation TIMESTAMP WITH TIME ZONE;
ALTER TABLE alerte ADD COLUMN capture_regle_validateur_id UUID;
ALTER TABLE alerte ADD COLUMN capture_regle_source_validation VARCHAR(30);
ALTER TABLE alerte ADD COLUMN capture_regle_actif BOOLEAN;
