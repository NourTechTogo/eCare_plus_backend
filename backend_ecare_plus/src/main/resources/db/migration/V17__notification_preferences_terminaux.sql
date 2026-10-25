CREATE TABLE preference_notification (utilisateur_id UUID PRIMARY KEY REFERENCES utilisateur(id), version BIGINT NOT NULL, push_actif BOOLEAN NOT NULL DEFAULT TRUE, whatsapp_actif BOOLEAN NOT NULL DEFAULT FALSE);
ALTER TABLE notification DROP CONSTRAINT ck_notification_canal;
ALTER TABLE notification ADD CONSTRAINT ck_notification_canal CHECK(canal IN ('PUSH','WHATSAPP','SMS','EMAIL','APPLICATION'));
ALTER TABLE rappel DROP CONSTRAINT ck_rappel_canal;
ALTER TABLE rappel ADD CONSTRAINT ck_rappel_canal CHECK(canal IN ('PUSH','WHATSAPP','SMS','EMAIL','APPLICATION'));
CREATE TABLE terminal_notification (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 installation_id VARCHAR(128) NOT NULL UNIQUE, utilisateur_id UUID NOT NULL REFERENCES utilisateur(id), plateforme VARCHAR(20) NOT NULL,
 jeton_push VARCHAR(4096) NOT NULL, session_ref VARCHAR(64) NOT NULL, session_version BIGINT NOT NULL, actif BOOLEAN NOT NULL
);
CREATE INDEX idx_terminal_utilisateur ON terminal_notification(utilisateur_id,actif);
CREATE TABLE consentement_canal (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 utilisateur_id UUID NOT NULL REFERENCES utilisateur(id), canal VARCHAR(20) NOT NULL, accepte BOOLEAN NOT NULL, version_document VARCHAR(80)
);
