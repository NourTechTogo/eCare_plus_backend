ALTER TABLE notification ADD COLUMN date_archivage TIMESTAMP WITH TIME ZONE;
ALTER TABLE notification ADD COLUMN date_suppression TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_notification_boite ON notification (utilisateur_id, date_suppression, date_archivage, date_creation);
