package org.ohdsi.webapi.commondto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ohdsi.webapi.authorization.dto.UserDTO;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class SecuredEntityDTO implements CommonDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UserDTO createdBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UserDTO modifiedBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date createdDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date modifiedDate;

    private boolean hasWriteAccess;
    private boolean hasDeleteAccess;

    public UserDTO getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserDTO createdBy) {
        this.createdBy = createdBy;
    }

    public UserDTO getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(UserDTO modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public boolean isHasWriteAccess() {
        return hasWriteAccess;
    }

    public void setHasWriteAccess(boolean hasWriteAccess) {
        this.hasWriteAccess = hasWriteAccess;
    }

    public boolean isHasDeleteAccess() {
        return hasDeleteAccess;
    }

    public void setHasDeleteAccess(boolean hasDeleteAccess) {
        this.hasDeleteAccess = hasDeleteAccess;
    }
}
