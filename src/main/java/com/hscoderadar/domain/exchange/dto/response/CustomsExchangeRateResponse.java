package com.hscoderadar.domain.exchange.dto.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * 관세청 환율 정보 API의 전체 XML 응답을 매핑하는 최상위 DTO. Jackson 애노테이션을 사용하여 XML 구조와 정확히 매핑함.
 */
@JacksonXmlRootElement(localName = "trifFxrtInfoQryRtnVo")
public record CustomsExchangeRateResponse(
    @JacksonXmlProperty(localName = "tCnt") int totalCount,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "trifFxrtInfoQryRsltVo") List<Item> itemList) {

  /** 개별 환율 정보를 담는 DTO. */
  public record Item(
      @JacksonXmlProperty(localName = "currSgn") String currencyCode, // 통화부호
      @JacksonXmlProperty(localName = "mtryUtNm") String currencyName, // 화폐단위명
      @JacksonXmlProperty(localName = "fxrt") String exchangeRate, // 환율
      @JacksonXmlProperty(localName = "aplyBgnDt") String notifiedDate, // 적용개시일자
      @JacksonXmlProperty(localName = "imexTp") String rateType, // 수출입구분 (1:수출, 2:수입)
      @JacksonXmlProperty(localName = "cntySgn") String countrySign // 국가부호
  ) {
  }
}