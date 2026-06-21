package com.siseradar.repository;

import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.PropertyType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplexGeocodeRepository extends JpaRepository<ComplexGeocode, Long> {

  /** All cached geocodes for a region+type — joined against the per-building stats. */
  List<ComplexGeocode> findByLawdCdAndPropertyType(String lawdCd, PropertyType propertyType);

  /** One building's cache row — for in-place re-geocode (주소 폴백 재워밍). */
  Optional<ComplexGeocode> findByLawdCdAndPropertyTypeAndBuildingName(
      String lawdCd, PropertyType propertyType, String buildingName);
}
