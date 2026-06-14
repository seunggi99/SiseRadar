package com.siseradar.repository;

import com.siseradar.domain.RegionCentroid;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionCentroidRepository extends JpaRepository<RegionCentroid, String> {

  /** SUCCESS-geocoded region centroids whose point falls within a lat/lng bbox (viewport). */
  @Query(
      """
      SELECT c FROM RegionCentroid c
      WHERE c.status = com.siseradar.domain.GeocodeStatus.SUCCESS
        AND c.lat BETWEEN :swLat AND :neLat
        AND c.lng BETWEEN :swLng AND :neLng
      """)
  List<RegionCentroid> findInBounds(
      @Param("swLat") double swLat,
      @Param("neLat") double neLat,
      @Param("swLng") double swLng,
      @Param("neLng") double neLng);
}
