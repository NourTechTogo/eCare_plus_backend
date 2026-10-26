ALTER TABLE piece_jointe ADD COLUMN duree_ms BIGINT;
ALTER TABLE piece_jointe ADD CONSTRAINT ck_piece_duree_vocale CHECK (duree_ms IS NULL OR (type_mime = 'audio/wav' AND duree_ms BETWEEN 250 AND 120000));
