/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ohdsi.webapi.cohortdefinition;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CohortDefinitionRepository extends CrudRepository<CohortDefinitionEntity, Integer> {
    Page<CohortDefinitionEntity> findAll(Pageable pageable);

    // The CohortDefinitionEntity marks the details property to be lazy loaded, so that when we're getting the full
    // list of definitions we don't load all of the details. However, when we're loading an individual cohort definition
    // we do want to eagerly load the details. So we use this method for doing that.
    @EntityGraph(attributePaths = "details.expression", type = EntityGraph.EntityGraphType.LOAD)
    @Query("select cd from CohortDefinition cd LEFT JOIN FETCH cd.createdBy LEFT JOIN FETCH cd.modifiedBy where cd.id = ?1")
    CohortDefinitionEntity findOneWithDetail(Integer id);

    @Query("select cd from CohortDefinition AS cd LEFT JOIN FETCH cd.createdBy LEFT JOIN FETCH cd.modifiedBy")
    List<CohortDefinitionEntity> list();

    @Query("select count(cd) from CohortDefinition AS cd WHERE cd.name = :name and cd.id <> :id")
    int getCountCDefWithSameName(@Param("id") Integer id, @Param("name") String name);

    @Query("SELECT cd FROM CohortDefinition cd WHERE cd.name LIKE ?1 ESCAPE '\\'")
    List<CohortDefinitionEntity> findAllByNameStartsWith(String pattern);

    Optional<CohortDefinitionEntity> findByName(String name);

    @Query("SELECT DISTINCT cd FROM CohortDefinition cd JOIN FETCH cd.tags t WHERE lower(t.name) in :tagNames")
    List<CohortDefinitionEntity> findByTags(@Param("tagNames") List<String> tagNames);
}
