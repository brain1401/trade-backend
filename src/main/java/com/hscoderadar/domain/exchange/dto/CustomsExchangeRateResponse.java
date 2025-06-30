package com.hscoderadar.domain.exchange.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * 관세청 환율 정보 API의 전체 XML 응답을 매핑하는 최상위 DTO. JAXB 어노테이션을 사용하여 XML 구조와 정확히 매핑함.
 */
@XmlRootElement(name = "trifFxrtInfoQryRtnVo")
@XmlAccessorType(XmlAccessType.FIELD)
public record CustomsExchangeRateResponse(
    @XmlElement(name = "tCnt") int totalCount,
    @XmlElement(name = "trifFxrtInfoQryRsltVo") List<Item> itemList) {

  /** 개별 환율 정보를 담는 DTO. */
  @XmlAccessorType(XmlAccessType.FIELD)
  public record Item(
      @XmlElement(name = "currSgn") String currencyCode, // 통화부호
      @XmlElement(name = "mtryUtNm") String currencyName, // 화폐단위명
      @XmlElement(name = "fxrt") String exchangeRate, // 환율
      @XmlElement(name = "aplyBgnDt") String notifiedDate, // 적용개시일자
      @XmlElement(name = "imexTp") String rateType, // 수출입구분 (1:수출, 2:수입)
      @XmlElement(name = "cntySgn") String countrySign // 국가부호
  ) {
  }
}