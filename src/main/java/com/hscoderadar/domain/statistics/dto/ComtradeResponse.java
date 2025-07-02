package com.hscoderadar.domain.statistics.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ComtradeResponse(
    @JsonProperty("count") int count,
    @JsonProperty("data") List<ComtradeData> data
) {

    public ComtradeResponse(List<Object> emptyList) {
        this(0, null);
    }}
