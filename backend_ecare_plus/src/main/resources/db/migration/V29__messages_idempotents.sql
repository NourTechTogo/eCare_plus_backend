ALTER TABLE message_conversation ADD COLUMN source_ref UUID;
ALTER TABLE message_conversation ADD CONSTRAINT uk_message_source UNIQUE (conversation_id, expediteur_id, source_ref);
