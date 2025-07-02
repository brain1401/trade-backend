package com.hscoderadar.domain.statistics.dto;

import java.util.List;

public record ComprehensiveTradeSummary(
    double totalExportValue,
    double totalImportValue,
    List<TopTradeItem> topExportCategories,
    List<TopTradeItem> topExportProducts,
    List<TopTradeItem> topImportCategories,
    List<TopTradeItem> topImportProducts
) {}