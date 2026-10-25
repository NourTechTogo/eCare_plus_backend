CREATE TABLE piece_jointe (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 conversation_id UUID NOT NULL REFERENCES conversation(id), message_id UUID REFERENCES message_conversation(id), auteur_id UUID NOT NULL REFERENCES utilisateur(id),
 nom_original VARCHAR(255) NOT NULL, type_mime VARCHAR(60) NOT NULL, taille_octets BIGINT NOT NULL, cle_stockage VARCHAR(80) NOT NULL,
 source_ref VARCHAR(128) NOT NULL, empreinte VARCHAR(64) NOT NULL, supprimee BOOLEAN NOT NULL DEFAULT FALSE, UNIQUE(auteur_id,source_ref)
);
CREATE INDEX idx_piece_message ON piece_jointe(message_id);
CREATE TABLE note_ciblee (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 auteur_id UUID NOT NULL REFERENCES utilisateur(id), patient_id UUID NOT NULL REFERENCES patient(id), type_cible VARCHAR(20) NOT NULL,
 cible_id UUID NOT NULL, contenu TEXT NOT NULL, visibilite VARCHAR(20) NOT NULL CHECK(visibilite IN ('PRIVEE','PARTAGEE')), supprime_le TIMESTAMPTZ
);
CREATE INDEX idx_note_cible ON note_ciblee(type_cible,cible_id);
