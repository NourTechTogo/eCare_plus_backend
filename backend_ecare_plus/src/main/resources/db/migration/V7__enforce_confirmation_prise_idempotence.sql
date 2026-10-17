-- Empêche la création de plusieurs confirmations en attente pour la même prise programmée,
-- pour rendre l'appel de programmation idempotent (retry mobile sans doublon).
ALTER TABLE confirmation_prise
    ADD CONSTRAINT uk_confirmation_prise_date_prevue UNIQUE (prise_medicament_id, date_prevue);
