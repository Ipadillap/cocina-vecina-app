package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Offer;
import cl.udec.cocinavecina.projection.NearbyOfferProjection;
import cl.udec.cocinavecina.projection.OfferProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import cl.udec.cocinavecina.model.OfferStatus;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    @Query(
        value = """
            select o.id, o.seller_id, u.display_name as seller, o.title, o.description, o.category,
                   o.photo_key, o.base_price, o.min_price, o.available_portions,
                   (select string_agg(a.allergen, ',' order by a.allergen)
                    from offer_allergen a
                    where a.offer_id = o.id) as allergens,
                   o.prepared_at, o.expires_at, o.status, p.cell_lat, p.cell_lon
            from offer o
            join app_user u on u.id = o.seller_id
            join pickup_point p on p.id = o.pickup_point_id
            where o.id = :id
            """,
        nativeQuery = true
    )
    Optional<OfferProjection> findViewById(@Param("id") Long id);

    @Query(
        value = """
            select o.id, o.seller_id, u.display_name as seller, o.title, o.description, o.category,
                   o.photo_key, o.base_price, o.min_price, o.available_portions,
                   (select string_agg(a.allergen, ',' order by a.allergen)
                    from offer_allergen a
                    where a.offer_id = o.id) as allergens,
                   o.prepared_at, o.expires_at, o.status, p.cell_lat, p.cell_lon
            from offer o
            join app_user u on u.id = o.seller_id
            join pickup_point p on p.id = o.pickup_point_id
            where o.seller_id = :sellerId
            order by o.published_at desc
            """,
        nativeQuery = true
    )
    List<OfferProjection> findViewsBySeller(@Param("sellerId") Long sellerId);

    @Query(
        value = """
            select r.*
            from (
                select o.id, o.title, o.category, o.photo_key, o.base_price, o.min_price,
                       o.available_portions, o.expires_at, p.cell_lat, p.cell_lon,
                       st_distance(p.cell_center,
                                   cast(st_setsrid(st_makepoint(:lon, :lat), 4326) as geography)) as distance
                from offer o
                join pickup_point p on p.id = o.pickup_point_id
                where o.status = 'ACTIVE'
                  and o.expires_at > now()
                  and o.available_portions > 0
                  and st_dwithin(p.cell_center,
                                 cast(st_setsrid(st_makepoint(:lon, :lat), 4326) as geography), :radius)
                  and (:category = '' or o.category = :category)
                  and not exists (
                      select 1
                      from offer_allergen a
                      where a.offer_id = o.id
                        and a.allergen = any (string_to_array(:excludedAllergens, ','))
                  )
            ) r
            order by case when :sort = 'TIME_LEFT' then extract(epoch from r.expires_at) else r.distance end
            limit 200
            """,
        nativeQuery = true
    )
    List<NearbyOfferProjection> findNearby(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("radius") int radius,
        @Param("category") String category,
        @Param("excludedAllergens") String excludedAllergens,
        @Param("sort") String sort
    );

    @Modifying
    @Query(
        value = """
            update offer
            set status = 'CLOSED', closed_at = now()
            where seller_id = :sellerId
              and status in ('ACTIVE', 'SOLD_OUT', 'HIDDEN')
            """,
        nativeQuery = true
    )
    void closeAllBySeller(@Param("sellerId") Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offer o WHERE o.id = :id")
    Optional<Offer> lockById(@Param("id") Long id);

    List<Offer> findByStatusOrderByPublishedAtAsc(OfferStatus status);

    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            update offer
            set available_portions = least(total_portions, available_portions + :portions),
                status = case when status = 'SOLD_OUT' and expires_at > now() then 'ACTIVE' else status end
            where id = :offerId
            """,
        nativeQuery = true
    )
    void restock(@Param("offerId") Long offerId, @Param("portions") int portions);

    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            update offer
            set available_portions = available_portions - :portions,
                status = case when available_portions - :portions = 0 then 'SOLD_OUT' else status end
            where id = :offerId
              and status = 'ACTIVE'
              and expires_at > now()
              and available_portions >= :portions
            """,
        nativeQuery = true
    )
    int reserve(@Param("offerId") Long offerId, @Param("portions") int portions);
}
