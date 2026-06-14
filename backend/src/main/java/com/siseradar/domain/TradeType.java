package com.siseradar.domain;

/** 거래 유형. 전세/월세는 RENT 안에서 monthlyRent==0 여부로 파생. */
public enum TradeType {
  SALE, // 매매 (거래금액)
  RENT // 전월세 (보증금 + 월세)
}
