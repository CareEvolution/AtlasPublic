select DOMAIN_ID, DOMAIN_NAME, DOMAIN_CONCEPT_ID 
from @vocab_schema.domain
order by DOMAIN_NAME asc
