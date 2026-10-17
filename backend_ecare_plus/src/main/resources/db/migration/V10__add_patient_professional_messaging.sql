CREATE TABLE conversation (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 patient_id UUID NOT NULL REFERENCES patient(id), professionnel_id UUID NOT NULL REFERENCES professionnel_sante(id),
 CONSTRAINT uk_conversation_patient_prof UNIQUE(patient_id, professionnel_id)
);
CREATE TABLE message_conversation (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
 expediteur_id UUID NOT NULL REFERENCES utilisateur(id), contenu VARCHAR(2000) NOT NULL, envoye_le TIMESTAMPTZ NOT NULL,
 lu_le TIMESTAMPTZ
);
CREATE INDEX idx_message_conversation_date ON message_conversation(conversation_id, envoye_le);
