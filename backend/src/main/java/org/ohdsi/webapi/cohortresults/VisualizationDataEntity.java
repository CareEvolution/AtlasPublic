package org.ohdsi.webapi.cohortresults;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.*;

@Entity(name = "VisualizationData")
@Table(name = "heracles_visualization_data")
public class VisualizationDataEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -567692689983359944L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "visualization_generator")
    @SequenceGenerator(name = "visualization_generator", sequenceName = "heracles_vis_data_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "cohort_definition_id")
    private int cohortDefinitionId;

    @Column(name = "source_id")
    private int sourceId;

    @Column(name = "visualization_key")
    private String visualizationKey;

    @Column(name = "data")
    private String data;

    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "drilldown_id")
    private int drilldownId;

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id
     *         the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the cohortDefinitionId
     */
    public int getCohortDefinitionId() {
        return cohortDefinitionId;
    }

    /**
     * @param cohortDefinitionId
     *         the cohortDefinitionId to set
     */
    public void setCohortDefinitionId(int cohortDefinitionId) {
        this.cohortDefinitionId = cohortDefinitionId;
    }

    /**
     * @return the source_id
     */
    public int getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId
     *         the source_id to set
     */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the visualizationKey
     */
    public String getVisualizationKey() {
        return visualizationKey;
    }

    /**
     * @param visualizationKey
     *         the visualizationKey to set
     */
    public void setVisualizationKey(String visualizationKey) {
        this.visualizationKey = visualizationKey;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data
     *         the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * @return the endTime
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * @param endTime
     *         the endTime to set
     */
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * @return the drilldownId
     */
    public int getDrilldownId() {
        return drilldownId;
    }

    /**
     * @param drilldownId
     *         the drilldownId to set
     */
    public void setDrilldownId(int drilldownId) {
        this.drilldownId = drilldownId;
    }

}
