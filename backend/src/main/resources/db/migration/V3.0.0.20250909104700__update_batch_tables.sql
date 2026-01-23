
-- These views use the batch job tables. The batch job schema is changing, so we need to drop these
-- and then recreate them after the changes are made.
DROP VIEW ${ohdsiSchema}.USER_IMPORT_JOB_HISTORY;
DROP VIEW ${ohdsiSchema}.CC_GENERATION;
DROP VIEW ${ohdsiSchema}.ESTIMATION_ANALYSIS_GENERATION;
DROP VIEW ${ohdsiSchema}.PATHWAY_ANALYSIS_GENERATION;
DROP VIEW ${ohdsiSchema}.PREDICTION_ANALYSIS_GENERATION;

-- Make changes to the batch job tables
ALTER TABLE ${ohdsiSchema}.BATCH_STEP_EXECUTION ADD CREATE_TIME TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00';

ALTER TABLE ${ohdsiSchema}.BATCH_STEP_EXECUTION ALTER COLUMN START_TIME DROP NOT NULL;

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS DROP COLUMN DATE_VAL;

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS DROP COLUMN DOUBLE_VAL;

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN TYPE_CD TYPE VARCHAR(100);

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS RENAME TYPE_CD TO PARAMETER_TYPE;

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN KEY_NAME TYPE VARCHAR(100);

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS RENAME KEY_NAME TO PARAMETER_NAME;

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN STRING_VAL TYPE VARCHAR(2500);

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS RENAME STRING_VAL TO PARAMETER_VALUE;

-- Batch data migrations

-- Migrate data from the LONG_VAL column to the PARAMETER_VALUE column.
-- We don't need to do the DATE_VAL or DOUBLE_VAL columns because they are not used.
UPDATE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS
SET PARAMETER_VALUE = LONG_VAL::text
WHERE PARAMETER_VALUE IS NULL AND LONG_VAL IS NOT NULL;

ALTER TABLE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS DROP COLUMN LONG_VAL;

-- Migrate the TYPE_CD (now named PARAMETER_CD) values to the new format.
UPDATE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS
SET PARAMETER_TYPE = 'java.lang.Long'
WHERE PARAMETER_TYPE = 'LONG';

UPDATE ${ohdsiSchema}.BATCH_JOB_EXECUTION_PARAMS
SET PARAMETER_TYPE = 'java.lang.String'
WHERE PARAMETER_TYPE = 'STRING';

-- I'm a bit concerned that just base64 enconding the existing data might not work, but let's give it a try.
UPDATE ${ohdsiSchema}.BATCH_JOB_EXECUTION_CONTEXT
SET SHORT_CONTEXT = encode(SHORT_CONTEXT::bytea, 'base64');

UPDATE ${ohdsiSchema}.BATCH_STEP_EXECUTION_CONTEXT
SET SHORT_CONTEXT = encode(SHORT_CONTEXT::bytea, 'base64');

-- Recreate the views

CREATE VIEW ${ohdsiSchema}.USER_IMPORT_JOB_HISTORY AS
SELECT job.job_execution_id AS id,
    job.start_time,
    job.end_time,
    job.status,
    job.exit_code,
    job.exit_message,
    name_param.parameter_value AS job_name,
    author_param.parameter_value AS author,
    user_import_param.parameter_value::integer AS user_import_id
   FROM ${ohdsiSchema}.batch_job_execution job
     JOIN ${ohdsiSchema}.batch_job_instance instance ON instance.job_instance_id = job.job_instance_id
     JOIN ${ohdsiSchema}.batch_job_execution_params name_param ON job.job_execution_id = name_param.job_execution_id AND name_param.parameter_name::text = 'jobName'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params user_import_param ON job.job_execution_id = user_import_param.job_execution_id AND user_import_param.parameter_name::text = 'user_import_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params author_param ON job.job_execution_id = author_param.job_execution_id AND author_param.parameter_name::text = 'jobAuthor'::text
  WHERE instance.job_name::text = 'usersImport'::text;

CREATE VIEW ${ohdsiSchema}.CC_GENERATION AS
SELECT job.job_execution_id AS id,
    job.create_time AS start_time,
    job.end_time,
    job.status,
    job.exit_message,
    cc_id_param.parameter_value::integer AS cc_id,
    source_param.parameter_value::integer AS source_id,
    gen_info.hash_code,
    gen_info.created_by_id
   FROM ${ohdsiSchema}.batch_job_execution job
     JOIN ${ohdsiSchema}.batch_job_execution_params cc_id_param ON job.job_execution_id = cc_id_param.job_execution_id AND cc_id_param.parameter_name::text = 'cohort_characterization_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params source_param ON job.job_execution_id = source_param.job_execution_id AND source_param.parameter_name::text = 'source_id'::text
     JOIN ${ohdsiSchema}.source s ON s.source_id = source_param.parameter_value::integer
     LEFT JOIN ${ohdsiSchema}.analysis_generation_info gen_info ON job.job_execution_id = gen_info.job_execution_id
  ORDER BY job.create_time DESC;

CREATE VIEW ${ohdsiSchema}.ESTIMATION_ANALYSIS_GENERATION AS
SELECT job.job_execution_id AS id,
    job.create_time AS start_time,
    job.end_time,
    job.status,
    job.exit_message,
    estimation_id_param.parameter_value::integer AS estimation_id,
    source_param.parameter_value::integer AS source_id,
    passwd_param.parameter_value AS update_password,
    gen_info.hash_code,
    gen_info.created_by_id,
    exec_info.id AS analysis_execution_id
   FROM ${ohdsiSchema}.batch_job_execution job
     JOIN ${ohdsiSchema}.batch_job_execution_params estimation_id_param ON job.job_execution_id = estimation_id_param.job_execution_id AND estimation_id_param.parameter_name::text = 'estimation_analysis_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params source_param ON job.job_execution_id = source_param.job_execution_id AND source_param.parameter_name::text = 'source_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params passwd_param ON job.job_execution_id = passwd_param.job_execution_id AND passwd_param.parameter_name::text = 'update_password'::text
     JOIN ${ohdsiSchema}.source s ON s.source_id = source_param.parameter_value::integer
     LEFT JOIN ${ohdsiSchema}.ee_analysis_status exec_info ON job.job_execution_id = exec_info.job_execution_id
     LEFT JOIN ${ohdsiSchema}.analysis_generation_info gen_info ON job.job_execution_id = gen_info.job_execution_id;

CREATE VIEW ${ohdsiSchema}.PATHWAY_ANALYSIS_GENERATION AS
SELECT job.job_execution_id AS id,
    job.create_time AS start_time,
    job.end_time,
    job.status,
    job.exit_message,
    pa_id_param.parameter_value::integer AS pathway_analysis_id,
    source_param.parameter_value::integer AS source_id,
    gen_info.hash_code,
    gen_info.created_by_id
   FROM ${ohdsiSchema}.batch_job_execution job
     JOIN ${ohdsiSchema}.batch_job_execution_params pa_id_param ON job.job_execution_id = pa_id_param.job_execution_id AND pa_id_param.parameter_name::text = 'pathway_analysis_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params source_param ON job.job_execution_id = source_param.job_execution_id AND source_param.parameter_name::text = 'source_id'::text
     JOIN ${ohdsiSchema}.source s ON s.source_id = source_param.parameter_value::integer
     LEFT JOIN ${ohdsiSchema}.analysis_generation_info gen_info ON job.job_execution_id = gen_info.job_execution_id
  ORDER BY job.create_time DESC;

CREATE VIEW ${ohdsiSchema}.PREDICTION_ANALYSIS_GENERATION AS
SELECT job.job_execution_id AS id,
    job.create_time AS start_time,
    job.end_time,
    job.status,
    job.exit_message,
    plp_id_param.parameter_value::integer AS prediction_id,
    source_param.parameter_value::integer AS source_id,
    passwd_param.parameter_value AS update_password,
    gen_info.hash_code,
    gen_info.created_by_id,
    exec_info.id AS analysis_execution_id
   FROM ${ohdsiSchema}.batch_job_execution job
     JOIN ${ohdsiSchema}.batch_job_execution_params plp_id_param ON job.job_execution_id = plp_id_param.job_execution_id AND plp_id_param.parameter_name::text = 'prediction_analysis_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params source_param ON job.job_execution_id = source_param.job_execution_id AND source_param.parameter_name::text = 'source_id'::text
     JOIN ${ohdsiSchema}.batch_job_execution_params passwd_param ON job.job_execution_id = passwd_param.job_execution_id AND passwd_param.parameter_name::text = 'update_password'::text
     JOIN ${ohdsiSchema}.source s ON s.source_id = source_param.parameter_value::integer
     LEFT JOIN ${ohdsiSchema}.ee_analysis_status exec_info ON job.job_execution_id = exec_info.job_execution_id
     LEFT JOIN ${ohdsiSchema}.analysis_generation_info gen_info ON job.job_execution_id = gen_info.job_execution_id;