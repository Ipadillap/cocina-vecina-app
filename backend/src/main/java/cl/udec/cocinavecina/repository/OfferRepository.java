package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
}
