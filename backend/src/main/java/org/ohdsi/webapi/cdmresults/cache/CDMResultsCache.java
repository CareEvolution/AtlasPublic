/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.ohdsi.webapi.cdmresults.cache;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.ohdsi.webapi.cdmresults.DescendantRecordAndPersonCount;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class caches not only the values from getRecordsFromQueryFunction, it also caches
 * the id arguments that are passed to this function. It makes possible to run only once id that does not present in the
 * getRecordsFromQueryFunction storage.
 *
 * @author fdefalco, ymolodkov
 */
public class CDMResultsCache {
    //BoundedConcurrentHashMap is hibernate implementation of the LRU(Least recently used) cache map. It supports concurrency out of the box, and does not block get operation.
    //I set 1,000,000 for capacity, this is a significant amount, but at the same time it should be only 20-25mb for 8 digital ids
    private final Set<Integer> requestedIdsThatDoesNotHaveValueInStorage = Collections.newSetFromMap(
            new BoundedConcurrentHashMap<>(1_000_000, 16));
    private final Map<Integer, DescendantRecordAndPersonCount> cachedValues = new ConcurrentHashMap<>();

    private boolean warm;

    public Collection<DescendantRecordAndPersonCount> findAndCache(
            List<Integer> ids,
            Function<List<Integer>, List<DescendantRecordAndPersonCount>> getRecordsFromQueryFunction) {

        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        var notRequestedRecordIds = ids.stream().filter(this::notRequested)
                .collect(Collectors.toList());

        var recordsFromCache = ids.stream().map(this::get).filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (this.isWarm() || CollectionUtils.isEmpty(notRequestedRecordIds)) {
            return recordsFromCache;
        }

        var recordsFromQuery = getRecordsFromQueryFunction.apply(notRequestedRecordIds);
        recordsFromQuery = CollectionUtils.isNotEmpty(recordsFromQuery) ? recordsFromQuery : Collections.emptyList();

        this.cacheValues(recordsFromQuery);
        this.cacheRequestedIds(ids);

        return CollectionUtils.union(recordsFromCache, recordsFromQuery);
    }

    public void warm() {
        warm = true;
    }

    public boolean isWarm() {
        return warm;
    }

    public boolean notWarm() {
        return !warm;
    }

    public void cacheValue(DescendantRecordAndPersonCount value) {
        cachedValues.put(value.getId(), value);
    }

    public void cacheValues(Collection<DescendantRecordAndPersonCount> values) {
        values.forEach(this::cacheValue);
    }

    public DescendantRecordAndPersonCount get(Integer id) {
        return cachedValues.get(id);
    }

    protected void cacheRequestedId(Integer id) {
        if (cachedValues.containsKey(id)) {
            return;
        }
        requestedIdsThatDoesNotHaveValueInStorage.add(id);
    }

    protected void cacheRequestedIds(Collection<Integer> values) {
        values.forEach(this::cacheRequestedId);
    }

    protected boolean isRequested(Integer id) {
        return cachedValues.containsKey(id) || requestedIdsThatDoesNotHaveValueInStorage.contains(id);
    }

    protected boolean notRequested(Integer id) {
        return !isRequested(id);
    }
}
