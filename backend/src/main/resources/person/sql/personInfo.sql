select gender_concept_id, year_of_birth, concept_name as gender
from @cdm_schema.person p
join @vocab_schema.concept c on p.gender_concept_id = c.concept_id
where person_id = @personId
