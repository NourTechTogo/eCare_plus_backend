ALTER TABLE prise_medicament ADD COLUMN fuseau VARCHAR(80) NOT NULL DEFAULT 'UTC';
ALTER TABLE prise_medicament ADD COLUMN jours_semaine VARCHAR(20);
ALTER TABLE confirmation_prise ADD COLUMN date_initiale TIMESTAMPTZ;
ALTER TABLE confirmation_prise ADD COLUMN annulee BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE confirmation_prise ADD COLUMN quantite_prevue NUMERIC(10,3);
ALTER TABLE confirmation_prise ADD COLUMN unite_prevue VARCHAR(20);
ALTER TABLE confirmation_prise ADD COLUMN date_action_client TIMESTAMPTZ;
UPDATE confirmation_prise SET date_initiale=date_prevue;
UPDATE confirmation_prise c SET quantite_prevue=p.quantite, unite_prevue=p.unite FROM prise_medicament p WHERE c.prise_medicament_id=p.id;
ALTER TABLE confirmation_prise ALTER COLUMN date_initiale SET NOT NULL;
ALTER TABLE confirmation_prise DROP CONSTRAINT uk_confirmation_prise_date_prevue;
ALTER TABLE confirmation_prise ADD CONSTRAINT uq_confirmation_initiale UNIQUE(prise_medicament_id,date_initiale);
CREATE TABLE reponse_prise_operation (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 acteur_id UUID NOT NULL REFERENCES utilisateur(id), source_ref VARCHAR(128) NOT NULL, requete TEXT NOT NULL, resultat TEXT NOT NULL,
 UNIQUE(acteur_id,source_ref)
);
