ALTER TABLE confirmation_prise ADD COLUMN medicament_nom VARCHAR(150);
ALTER TABLE confirmation_prise ADD COLUMN medicament_dosage VARCHAR(40);
ALTER TABLE confirmation_prise ADD COLUMN medicament_unite_dosage VARCHAR(20);
ALTER TABLE confirmation_prise ADD COLUMN medicament_forme VARCHAR(60);
ALTER TABLE confirmation_prise ADD COLUMN voie_prevue VARCHAR(100);
ALTER TABLE confirmation_prise ADD COLUMN fuseau_prevu VARCHAR(80);
-- Preserve known values at migration time; no claim to reconstruct an unknown historical prescription.
UPDATE confirmation_prise c SET medicament_nom=m.nom_commercial,medicament_dosage=m.dosage,medicament_unite_dosage=m.unite_dosage,medicament_forme=m.forme,voie_prevue=p.voie_administration,fuseau_prevu=p.fuseau FROM prise_medicament p JOIN medicament m ON m.id=p.medicament_id WHERE c.prise_medicament_id=p.id;
