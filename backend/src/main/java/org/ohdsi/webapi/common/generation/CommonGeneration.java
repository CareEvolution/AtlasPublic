package org.ohdsi.webapi.common.generation;

import jakarta.persistence.*;
import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.source.SourceEntity;

import java.util.Date;

@MappedSuperclass
public abstract class CommonGeneration {

    @Id
    @Column
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    protected SourceEntity source;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    protected Date startTime;

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    protected Date endTime;

    @Column
    protected String status;

    @Column(name = "exit_message")
    private String exitMessage;

    //@Embedded
    //protected AnalysisGenerationBaseInfo info;

    public Long getId() {
        return id;
    }

    public SourceEntity getSource() {
        return source;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public Integer getHashCode() {
        //return this.info != null ? this.info.getHashCode() : null;
        return 1;
    }

    public UserEntity getCreatedBy() {
        //return this.info != null ? this.info.getCreatedBy() : null;
        return null;
    }
}
