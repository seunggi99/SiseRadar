package com.siseradar.collect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * Maps the data.go.kr {@code getRTMSDataSvcAptTrade} XML response. Field names mirror the
 * <b>actual</b> API payload (verified live), not the Korean labels in the spec.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public class AptTradeApiResponse {

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

    @JacksonXmlProperty(localName = "numOfRows")
    public int numOfRows;

    @JacksonXmlProperty(localName = "pageNo")
    public int pageNo;

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

    /** 거래금액, 만원, 콤마 포함 (e.g. "186,000"). */
    @JacksonXmlProperty(localName = "dealAmount")
    public String dealAmount;

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
