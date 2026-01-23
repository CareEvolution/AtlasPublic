package org.ohdsi.webapi.i18n;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AtlasLocale(String code, String name, @JsonProperty("default") boolean isDefault) { }
