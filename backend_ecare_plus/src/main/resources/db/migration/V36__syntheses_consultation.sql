ALTER TABLE rapport_suivi ADD COLUMN contenu_synthese TEXT;
ALTER TABLE rapport_suivi ADD COLUMN source_ref_synthese UUID;
ALTER TABLE rapport_suivi ADD COLUMN empreinte_synthese VARCHAR(64);
ALTER TABLE rapport_suivi ADD COLUMN fuseau_synthese VARCHAR(80);
ALTER TABLE rapport_suivi ADD CONSTRAINT uq_rapport_synthese_source UNIQUE (dossier_id, source_ref_synthese);
