package org.ohdsi.webapi.authorization.dto;

import org.ohdsi.webapi.authorization.model.AccessType;

public record AccessRequestDTO(AccessType accessType) {}
