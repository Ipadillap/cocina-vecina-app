package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
}
