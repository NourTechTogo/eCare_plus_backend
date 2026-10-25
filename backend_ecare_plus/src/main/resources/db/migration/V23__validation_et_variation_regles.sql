ALTER TABLE regle_alerte ADD COLUMN validateur_id UUID REFERENCES utilisateur(id);
ALTER TABLE regle_alerte ADD COLUMN source_validation VARCHAR(30);
ALTER TABLE regle_alerte DROP CONSTRAINT ck_regle_operateur;
ALTER TABLE regle_alerte ADD CONSTRAINT ck_regle_operateur CHECK(operateur IN ('>','>=','<','<=','HORS_PLAGE','VARIATION_ABSOLUE'));
