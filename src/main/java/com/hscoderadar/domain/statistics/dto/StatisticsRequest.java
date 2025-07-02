package com.hscoderadar.domain.statistics.dto;

public record StatisticsRequest(
    String reporterCode,
    String partnerCode,
    String StartPeriod,
    String EndPeriod) {
}
