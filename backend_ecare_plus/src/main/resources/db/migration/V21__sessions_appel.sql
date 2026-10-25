CREATE TABLE session_appel (
 id UUID PRIMARY KEY, version BIGINT NOT NULL, date_creation TIMESTAMPTZ NOT NULL, date_modification TIMESTAMPTZ NOT NULL,
 conversation_id UUID NOT NULL REFERENCES conversation(id), initiateur_id UUID NOT NULL REFERENCES utilisateur(id), destinataire_id UUID NOT NULL REFERENCES utilisateur(id),
 type VARCHAR(20) NOT NULL, statut VARCHAR(30) NOT NULL, salle VARCHAR(80) NOT NULL UNIQUE, source_ref VARCHAR(128) NOT NULL,
 expire_le TIMESTAMPTZ NOT NULL, debut_le TIMESTAMPTZ, fin_le TIMESTAMPTZ, duree_secondes BIGINT,
 fermeture_confirmee BOOLEAN NOT NULL DEFAULT FALSE, initiateur_connecte BOOLEAN NOT NULL DEFAULT FALSE, destinataire_connecte BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE(initiateur_id,source_ref)
);
CREATE INDEX idx_session_appel_conversation ON session_appel(conversation_id,date_creation DESC);
