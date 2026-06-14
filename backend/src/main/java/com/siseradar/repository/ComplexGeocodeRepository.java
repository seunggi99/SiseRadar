package com.siseradar.repository;

import com.siseradar.domain.ComplexGeocode;
import com.siseradar.domain.PropertyType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplexGeocodeRepository extends JpaRepository<ComplexGeocode, Long> {

  /** All cached geocodes for a region+type — joined against the per-building stats. */
  List<ComplexGeocode> findByLawdCdAndPropertyType(String lawdCd, PropertyType propertyType);

  /** Region centroids = average of SUCCESS-geocoded complex coords (no Kakao calls). */
  @Query(
      value =
          """
          SELECT lawd_cd AS lawdCd, AVG(lat) AS lat, AVG(lng) AS lng
          FROM complex_geocode
          WHERE property_type = :pt AND status = 'SUCCESS'
          GROUP BY lawd_cd
          """,
      nativeQuery = true)
  List<RegionCentroidRow> regionCentroids(@Param("pt") String propertyType);
}
