package com.hscoderadar.domain.exchange.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 관세청 환율 정보 API의 전체 XML 응답을 매핑하는 최상위 DTO
 * JAXB 어노테이션을 사용하여 XML 구조와 정확히 매핑
 */
@Getter
@Setter
@ToString
@XmlRootElement(name = "trifFxrtInfoQryRtnVo")
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomsExchangeRateResponse {

    @XmlElement(name = "tCnt")
    private int totalCount;

    @XmlElement(name = "trifFxrtInfoQryRsltVo")
    private List<Item> itemList;

    /**
     * 개별 환율 정보를 담는 DTO
     */
    @Getter
    @Setter
    @ToString
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Item {
        @XmlElement(name = "currSgn")
        private String currencyCode; // 통화부호

        @XmlElement(name = "mtryUtNm")
        private String currencyName; // 화폐단위명

        @XmlElement(name = "fxrt")
        private String exchangeRate; // 환율

        @XmlElement(name = "aplyBgnDt")
        private String notifiedDate; // 적용개시일자

        @XmlElement(name = "imexTp")
        private String rateType; // 수출입구분 (1:수출, 2:수입)

        @XmlElement(name = "cntySgn")
        private String countrySign; // 국가부호
    }
}