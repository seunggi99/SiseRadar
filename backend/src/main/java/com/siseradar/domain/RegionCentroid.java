package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Persistent cache of a 시군구's own centroid (from a one-time address-search geocode of the
 * region name). 250 codes max, geocoded once — independent of complex geocoding. Region bubbles
 * use this so they appear for every region with transaction data, not just geocoded complexes.
 */
@Entity
@Table(name = "region_centroid")
public class RegionCentroid {

  @Id
  @Column(name = "lawd_cd", length = 5)
  private String lawdCd;

  @Column(name = "lat")
  private Double lat;

  @Column(name = "lng")
  private Double lng;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private GeocodeStatus status;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected RegionCentroid() {}

  public RegionCentroid(String lawdCd, Double lat, Double lng, GeocodeStatus status) {
    this.lawdCd = lawdCd;
    this.lat = lat;
    this.lng = lng;
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public String getLawdCd() {
    return lawdCd;
  }

  public Double getLat() {
    return lat;
  }

  public Double getLng() {
    return lng;
  }

  public GeocodeStatus getStatus() {
    return status;
  }
}
