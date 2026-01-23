/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ohdsi.webapi.report;

public class ConditionOccurrenceTreemapNode {
    public ConditionOccurrenceTreemapNode() {
    }

    public long conceptId;
    public String conceptPath;
    public long numPersons;
    public Float percentPersons;
    public Float recordsPerPerson;
}
