CREATE INDEX idx_confirmation_echeance ON confirmation_prise (date_prevue, statut, annulee);
ALTER TABLE notification ADD COLUMN date_invalidation TIMESTAMP WITH TIME ZONE;
