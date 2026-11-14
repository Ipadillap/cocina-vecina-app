package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
}
