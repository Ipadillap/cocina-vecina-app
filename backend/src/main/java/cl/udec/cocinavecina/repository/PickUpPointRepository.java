package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.PickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickUpPointRepository extends JpaRepository<PickupPoint, Long> {
}
