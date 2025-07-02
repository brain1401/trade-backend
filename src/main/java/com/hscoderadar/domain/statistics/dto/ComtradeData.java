package com.hscoderadar.domain.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ComtradeData(
    @JsonProperty("refYear") int refYear,
    @JsonProperty("reporterCode") int reporterCode,
    @JsonProperty("reporterDesc") String reporterDesc,
    @JsonProperty("partnerCode") int partnerCode,
    @JsonProperty("partnerDesc") String partnerDesc,
    @JsonProperty("cmdDesc") String cmdDesc,
    @JsonProperty("flowDesc") String flowDesc,
    @JsonProperty("aggrLevel") Integer aggrLevel,
    @JsonProperty("flowCode") String flowCode,
    @JsonProperty("primaryValue") double primaryValue
) {}
