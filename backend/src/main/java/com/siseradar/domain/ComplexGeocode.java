package com.siseradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Geocode cache for a (region, property type, building) — coords + status. Failures are cached so
 * we don't retry forever. Validated: the geocoded point must reverse-resolve to the same 시군구.
 */
@Entity
@Table(
    name = "complex_geocode",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_complex_geocode",
            columnNames = {"lawd_cd", "property_type", "building_name"}))
public class ComplexGeocode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lawd_cd", nullable = false, length = 5)
  private String lawdCd;

  @Enumerated(EnumType.STRING)
  @Column(name = "property_type", nullable = false, length = 20)
  private PropertyType propertyType;

  @Column(name = "building_name", nullable = false)
  private String buildingName;

  @Column(name = "lat")
  private Double lat;

  @Column(name = "lng")
  private Double lng;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private GeocodeStatus status;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ComplexGeocode() {}

  public ComplexGeocode(
      String lawdCd,
      PropertyType propertyType,
      String buildingName,
      Double lat,
      Double lng,
      GeocodeStatus status) {
    this.lawdCd = lawdCd;
    this.propertyType = propertyType;
    this.buildingName = buildingName;
    this.lat = lat;
    this.lng = lng;
    this.status = status;
    this.updatedAt = Instant.now();
  }

  /** Re-geocode an existing cache row in place (예: FAILED → 주소 폴백으로 SUCCESS). */
  public void refresh(Double lat, Double lng, GeocodeStatus status) {
    this.lat = lat;
    this.lng = lng;
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getBuildingName() {
    return buildingName;
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
