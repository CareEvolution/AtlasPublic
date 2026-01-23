package org.ohdsi.webapi.authorization.dto;

public record UserMeDTO(
        Long id,
        String login,
        String name) {}
