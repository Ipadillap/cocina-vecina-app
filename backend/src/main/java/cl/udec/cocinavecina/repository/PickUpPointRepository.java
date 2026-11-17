package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.PickupPoint;
import cl.udec.cocinavecina.projection.PickUpPointProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PickUpPointRepository extends JpaRepository<PickupPoint, Long> {
    @Query(
        value = """
            select p.id, p.alias, p.cell_lat, p.cell_lon
            from pickup_point p
            where p.seller_id = :sellerId
            order by p.created_at desc
            """,
        nativeQuery = true
    )
    List<PickUpPointProjection> findViewsBySeller(@Param("sellerId") Long sellerId);

    Optional<PickupPoint> findByIdAndSeller_Id(Long id, Long sellerId);
}
