package com.siseradar.domain;

/**
 * NEW_TRADE — notify when a watched target gets new transactions.
 * PRICE_CHANGE_PCT — notify when the month-over-month average price moves past {@code threshold} (%).
 */
public enum AlertCondition {
  NEW_TRADE,
  PRICE_CHANGE_PCT
}
