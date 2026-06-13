package com.siseradar.collect;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** What the scheduler collects and how politely it pages. */
@ConfigurationProperties(prefix = "siseradar.collection")
public record CollectionProperties(
    List<String> lawdCodes, int recentMonths, int numOfRows, long pageDelayMs) {}
