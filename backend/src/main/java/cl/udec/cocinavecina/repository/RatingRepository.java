package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByOrder_IdAndAuthor_Id(Long orderId, Long authorId);

    long countByRecipient_Id(Long recipientId);

    @Query("select avg(r.score) from Rating r where r.recipient.id = :recipientId")
    Double averageByRecipient(@Param("recipientId") Long recipientId);

    List<Rating> findByAuthor_IdOrRecipient_IdOrderByCreatedAtAscIdAsc(Long authorId, Long recipientId);
}
