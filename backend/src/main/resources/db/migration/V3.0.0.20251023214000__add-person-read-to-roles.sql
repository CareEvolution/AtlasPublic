INSERT INTO ${ohdsiSchema}.sec_role_permission (id, role_id, permission_id)
VALUES (
    nextval('${ohdsiSchema}.sec_role_permission_sequence'), 
    (SELECT id FROM ${ohdsiSchema}.sec_role WHERE name = 'admin'), 
    (SELECT id FROM ${ohdsiSchema}.sec_permission WHERE value = 'person:read'));

INSERT INTO ${ohdsiSchema}.sec_role_permission (id, role_id, permission_id)
VALUES (
    nextval('${ohdsiSchema}.sec_role_permission_sequence'), 
    (SELECT id FROM ${ohdsiSchema}.sec_role WHERE name = 'Atlas users'), 
    (SELECT id FROM ${ohdsiSchema}.sec_permission WHERE value = 'person:read'));
