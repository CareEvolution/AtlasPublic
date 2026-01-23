-- Create the anonymous user.  We will use the special id of 1, which is safe
-- because the sequence generator is set to start at 1000.
--
-- The login and name values match those used by Spring Security.
INSERT INTO ${ohdsiSchema}.sec_user (id, login, name, last_viewed_notifications_time, origin)
VALUES (1, 'anonymousUser', 'anonymousUser', NULL, 'SYSTEM');


-- We want the anonymous user to have the system role admin.  We have to test for
-- a system role because a user named "admin" could be created, and by default the
-- personal (non-system) role "admin" will be created.
INSERT INTO ${ohdsiSchema}.sec_user_role (id, user_id, role_id, status, origin)
SELECT
    nextval('${ohdsiSchema}.sec_user_role_sequence'),
    1,
    sr.id,
    NULL,
    'SYSTEM'
FROM
     ${ohdsiSchema}.sec_role sr
WHERE
    sr.name='admin' AND sr.system_role IS TRUE;

-- Add role "Atlas users"
INSERT INTO ${ohdsiSchema}.sec_user_role (id, user_id, role_id, status, origin)
SELECT
    nextval('${ohdsiSchema}.sec_user_role_sequence'),
    1,
    sr.id,
    NULL,
    'SYSTEM'
FROM
     ${ohdsiSchema}.sec_role sr
WHERE
    sr.name='Atlas users' AND sr.system_role IS TRUE;

-- Add role "Moderator"
INSERT INTO ${ohdsiSchema}.sec_user_role (id, user_id, role_id, status, origin)
SELECT
    nextval('${ohdsiSchema}.sec_user_role_sequence'),
    1,
    sr.id,
    NULL,
    'SYSTEM'
FROM
     ${ohdsiSchema}.sec_role sr
WHERE
    sr.name='Moderator' AND sr.system_role IS TRUE;

-- Add role "public"
INSERT INTO ${ohdsiSchema}.sec_user_role (id, user_id, role_id, status, origin)
SELECT
    nextval('${ohdsiSchema}.sec_user_role_sequence'),
    1,
    sr.id,
    NULL,
    'SYSTEM'
FROM
     ${ohdsiSchema}.sec_role sr
WHERE
    sr.name='public' AND sr.system_role IS TRUE;
