package com.siseradar.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * Maps the data.go.kr RTMS XML responses. Superset of fields across operations (all optional);
 * APT 매매 uses {@code dealAmount}, APT 전월세 uses {@code deposit}+{@code monthlyRent}. Verified live.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public class RtmsApiResponse {

  @JacksonXmlProperty(localName = "header")
  public Header header;

  @JacksonXmlProperty(localName = "body")
  public Body body;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Header {
    @JacksonXmlProperty(localName = "resultCode")
    public String resultCode;

    @JacksonXmlProperty(localName = "resultMsg")
    public String resultMsg;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Body {
    @JacksonXmlProperty(localName = "items")
    public Items items;

    @JacksonXmlProperty(localName = "totalCount")
    public int totalCount;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Items {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    public List<Item> item;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    /** 건물명 — 유형별로 필드가 다름: 아파트/분양권 aptNm, 오피스텔 offiNm, 연립다세대 mhouseNm. */
    @JacksonXmlProperty(localName = "aptNm")
    public String aptNm;

    @JacksonXmlProperty(localName = "offiNm")
    public String offiNm;

    @JacksonXmlProperty(localName = "mhouseNm")
    public String mhouseNm;

    @JacksonXmlProperty(localName = "umdNm")
    public String umdNm;

    @JacksonXmlProperty(localName = "jibun")
    public String jibun;

    /** 전용면적 ㎡ (아파트·오피스텔·연립·분양권). */
    @JacksonXmlProperty(localName = "excluUseAr")
    public String excluUseAr;

    /** 연면적 ㎡ (단독·다가구). */
    @JacksonXmlProperty(localName = "totalFloorAr")
    public String totalFloorAr;

    /** 건물면적 ㎡ (상업업무용·산업용). */
    @JacksonXmlProperty(localName = "buildingAr")
    public String buildingAr;

    /** 거래면적 ㎡ (토지). */
    @JacksonXmlProperty(localName = "dealArea")
    public String dealArea;

    @JacksonXmlProperty(localName = "floor")
    public String floor;

    @JacksonXmlProperty(localName = "buildYear")
    public String buildYear;

    /** 매매 거래금액 (만원, 콤마 포함). */
    @JacksonXmlProperty(localName = "dealAmount")
    public String dealAmount;

    /** 전월세 보증금 (만원, 콤마 포함). */
    @JacksonXmlProperty(localName = "deposit")
    public String deposit;

    /** 전월세 월세 (만원, 0=전세). */
    @JacksonXmlProperty(localName = "monthlyRent")
    public String monthlyRent;

    @JacksonXmlProperty(localName = "dealYear")
    public String dealYear;

    @JacksonXmlProperty(localName = "dealMonth")
    public String dealMonth;

    @JacksonXmlProperty(localName = "dealDay")
    public String dealDay;

    @JacksonXmlProperty(localName = "sggCd")
    public String sggCd;
  }
}
