package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Offer;
import cl.udec.cocinavecina.projection.NearbyOfferProjection;
import cl.udec.cocinavecina.projection.OfferProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    String OFFER_VIEW = """
        SELECT o.id, o.seller_id, u.display_name AS seller, o.title, o.description, o.category,
               o.photo_key, o.base_price, o.min_price, o.available_portions,
               (SELECT string_agg(a.allergen, ',' ORDER BY a.allergen)
                FROM offer_allergen a
                WHERE a.offer_id = o.id) AS allergens,
               o.prepared_at, o.expires_at, o.status, p.cell_lat, p.cell_lon
        FROM offer o
        JOIN app_user u ON u.id = o.seller_id
        JOIN pickup_point p ON p.id = o.pickup_point_id
        """;

    @Query(value = OFFER_VIEW + "where o.id = :id", nativeQuery = true)
    Optional<OfferProjection> findViewById(@Param("id") Long id);

    @Query(value = OFFER_VIEW + "where o.seller_id = :sellerId order by o.published_at desc", nativeQuery = true)
    List<OfferProjection> findViewsBySeller(@Param("sellerId") Long sellerId);

    @Query(value = """
        SELECT r.*
        FROM (
            SELECT o.id, o.title, o.category, o.photo_key, o.base_price, o.min_price,
                   o.available_portions, o.expires_at, p.cell_lat, p.cell_lon,
                   ST_Distance(p.cell_center,
                               CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography)) AS distance
            FROM offer o
            JOIN pickup_point p ON p.id = o.pickup_point_id
            WHERE o.status = 'ACTIVE'
              AND o.expires_at > now()
              AND o.available_portions > 0
              AND ST_DWithin(p.cell_center,
                             CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :radius)
              AND (:category = '' OR o.category = :category)
              AND NOT EXISTS (
                  SELECT 1
                  FROM offer_allergen a
                  WHERE a.offer_id = o.id
                    AND a.allergen = ANY (string_to_array(:excludedAllergens, ','))
              )
        ) r
        ORDER BY CASE WHEN :sort = 'TIME_LEFT' THEN EXTRACT(EPOCH FROM r.expires_at) ELSE r.distance END
        LIMIT 200
        """, nativeQuery = true)
    List<NearbyOfferProjection> findNearby(@Param("lat") double lat,
                                           @Param("lon") double lon,
                                           @Param("radius") int radius,
                                           @Param("category") String category,
                                           @Param("excludedAllergens") String excludedAllergens,
                                           @Param("sort") String sort);

    @Modifying
    @Query(value = """
        UPDATE offer
        SET status = 'CLOSED', closed_at = now()
        WHERE seller_id = :sellerId
          AND status IN ('ACTIVE', 'SOLD_OUT', 'HIDDEN')
        """, nativeQuery = true)
    int closeAllBySeller(@Param("sellerId") Long sellerId);
}
