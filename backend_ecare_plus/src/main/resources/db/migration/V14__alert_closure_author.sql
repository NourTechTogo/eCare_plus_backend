ALTER TABLE alerte ADD COLUMN cloturee_par_id UUID;
ALTER TABLE alerte ADD CONSTRAINT fk_alerte_cloturee_par
    FOREIGN KEY (cloturee_par_id) REFERENCES professionnel_sante(id);
