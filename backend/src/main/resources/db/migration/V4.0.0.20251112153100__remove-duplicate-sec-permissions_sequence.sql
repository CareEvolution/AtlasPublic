ALTER TABLE ${ohdsiSchema}.sec_permission
    ALTER COLUMN id SET DEFAULT nextval('${ohdsiSchema}.sec_permission_id_seq'::regclass);
DROP SEQUENCE IF EXISTS ${ohdsiSchema}.sec_permission_sequence;