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
    /** 단지/건물명 (APT). 다른 유형은 추가 시 별도 필드 매핑 필요. */
    @JacksonXmlProperty(localName = "aptNm")
    public String aptNm;

    @JacksonXmlProperty(localName = "umdNm")
    public String umdNm;

    @JacksonXmlProperty(localName = "jibun")
    public String jibun;

    @JacksonXmlProperty(localName = "excluUseAr")
    public String excluUseAr;

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
