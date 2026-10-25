ALTER TABLE notification ADD COLUMN source_ref VARCHAR(160);
CREATE UNIQUE INDEX uq_notification_source ON notification(utilisateur_id,source_ref) WHERE source_ref IS NOT NULL;
