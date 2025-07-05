package com.hscoderadar.domain.customs.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "cargCsclPrgsInfoQryRtnVo")
public class CargoClearanceProgressResponse {

    @JacksonXmlProperty(localName = "tCnt")
    private int totalCount;

    // 화물 통관 진행 기본 정보 (단일 건)
    @JacksonXmlProperty(localName = "cargCsclPrgsInfoQryVo")
    private CargoProgressInfo baseInfo;

    // 화물 통관 진행 상세 정보 (리스트 - 여러 건 처리)
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "cargCsclPrgsInfoDtlQryVo")
    private List<CargoProgressDetail> detailList;

    @Data
    public static class CargoProgressInfo {
        @JacksonXmlProperty(localName = "cargMtNo")
        private String cargoManagementNumber; // 화물관리번호

        @JacksonXmlProperty(localName = "prgsStts")
        private String progressStatus; // 진행상태

        @JacksonXmlProperty(localName = "csclPrgsStts")
        private String clearanceProgressStatus; // 통관진행상태

        @JacksonXmlProperty(localName = "shipNatNm")
        private String shipNationalityName; // 선박국적명

        @JacksonXmlProperty(localName = "shcoFlco")
        private String shippingCompany; // 선사항공사

        @JacksonXmlProperty(localName = "mblNo")
        private String mblNo; // MBL번호

        @JacksonXmlProperty(localName = "hblNo")
        private String hblNo; // HBL번호

        @JacksonXmlProperty(localName = "dsprNm")
        private String portOfDischargeName; // 양륙항명

        @JacksonXmlProperty(localName = "etprDt")
        private String entryDate; // 입항일자

        @JacksonXmlProperty(localName = "pckGcnt")
        private Integer packageCount; // 포장개수

        @JacksonXmlProperty(localName = "ttwg")
        private Double totalWeight; // 총중량
    }

    @Data
    public static class CargoProgressDetail {
        @JacksonXmlProperty(localName = "cargTrcnRelaBsopTpcd")
        private String processType; // 처리구분 (반출신고 등)

        @JacksonXmlProperty(localName = "rlbrDttm")
        private String processDateTime; // 처리일시

        @JacksonXmlProperty(localName = "shedNm")
        private String warehouseName; // 장치장명

        @JacksonXmlProperty(localName = "rlbrCn")
        private String processContent; // 반출입내용

        @JacksonXmlProperty(localName = "pckGcnt")
        private Integer packageCount; // 포장개수

        @JacksonXmlProperty(localName = "wght")
        private Double weight; // 중량
    }
}