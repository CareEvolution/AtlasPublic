package org.ohdsi.webapi.cohortcharacterization.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.cohortcharacterization.dto.CcPrevalenceStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComparativeDistributionItem.class),
        @JsonSubTypes.Type(value = ComparativeItem.class),
        @JsonSubTypes.Type(value = DistributionItem.class),
        @JsonSubTypes.Type(value = PrevalenceItem.class)
})
public abstract class ExportItem<T extends ExportItem<T>> implements Comparable<T> {
    private final Integer analysisId;
    private final String analysisName;
    private final Long strataId;
    private final String strataName;
    private final Long covariateId;
    private final String covariateName;
    private final String covariateShortName;
    private final String faType;
    private String domainId;
    private final Long conceptId;
    private final String conceptName;

    public ExportItem(CcPrevalenceStat ccResult) {
        this.analysisId = ccResult.getAnalysisId();
        this.analysisName = ccResult.getAnalysisName();
        this.strataId = ccResult.getStrataId();
        this.strataName = getStrataNameOrDefault(ccResult.getStrataName());
        this.covariateId = ccResult.getCovariateId();
        this.covariateName = ccResult.getCovariateName();
        this.covariateShortName = extractMeaningfulCovariateName(ccResult.getCovariateName());
        this.faType = ccResult.getFaType();
        this.conceptId = ccResult.getConceptId();
        this.conceptName = ccResult.getConceptName();
    }

    public ExportItem(PrevalenceItem<?> item) {
        this.analysisId = item.getAnalysisId();
        this.analysisName = item.getAnalysisName();
        this.strataId = item.getStrataId();
        this.strataName = getStrataNameOrDefault(item.getStrataName());
        this.covariateId = item.getCovariateId();
        this.covariateName = item.getCovariateName();
        this.covariateShortName = extractMeaningfulCovariateName(item.getCovariateName());
        this.faType = item.getFaType();
        this.conceptId = item.getConceptId();
        this.conceptName = item.getConceptName();
    }

    protected List<String> getValueList() {
        var values = new ArrayList<String>();
        values.add(String.valueOf(this.analysisId));
        values.add(this.analysisName);
        values.add(String.valueOf(this.strataId));
        values.add(this.strataName);
        values.add(String.valueOf(this.covariateId));
        values.add(this.covariateName);
        values.add(this.covariateShortName);
        return values;
    }

    @Override
    public int compareTo(T that) {
        int res = analysisId.compareTo(that.getAnalysisId());
        if (res == 0) {
            res = covariateName.compareToIgnoreCase(that.getCovariateName());
        }
        return res;
    }

    @JsonIgnore
    public String[] getValueArray() {
        List<String> values = getValueList();
        return values.toArray(new String[values.size()]);
    }

    protected String extractMeaningfulCovariateName(String fullName) {
        if (fullName == null) {
            return StringUtils.EMPTY;
        }
        String[] nameParts = fullName.split(":");
        if (nameParts.length < 2) {
            nameParts = fullName.split("=");
        }
        if (nameParts.length != 2) {
            return fullName;
        } else {
            return nameParts[1];
        }
    }

    protected String getStrataNameOrDefault(String value) {
        return StringUtils.isNotEmpty(value) ? value : "All stratas";
    }

    protected double calcDiff(T another) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ExportItem<?> that = (ExportItem<?>) o;

        if (!Objects.equals(analysisId, that.analysisId))
            return false;
        if (!Objects.equals(strataId, that.strataId))
            return false;
        if (!Objects.equals(covariateId, that.covariateId))
            return false;
        if (!Objects.equals(conceptId, that.conceptId))
            return false;
        return Objects.equals(conceptName, that.conceptName);
    }

    @Override
    public int hashCode() {
        int result = analysisId != null ? analysisId.hashCode() : 0;
        result = 31 * result + (strataId != null ? strataId.hashCode() : 0);
        result = 31 * result + (covariateId != null ? covariateId.hashCode() : 0);
        result = 31 * result + (conceptId != null ? conceptId.hashCode() : 0);
        return result;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public Long getStrataId() {
        return strataId;
    }

    public String getStrataName() {
        return strataName;
    }

    public Long getCovariateId() {
        return covariateId;
    }

    public String getCovariateName() {
        return covariateName;
    }

    public String getCovariateShortName() {
        return covariateShortName;
    }

    public String getFaType() {
        return faType;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public Long getConceptId() {
        return conceptId;
    }

    public String getConceptName() {
        return conceptName;
    }
}
