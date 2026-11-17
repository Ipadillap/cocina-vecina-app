package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentRespository extends JpaRepository<Consent, Long> {
    boolean existsByUser_IdAndTypeAndRevokedAtIsNull(Long userId, String type);

    Optional<Consent> findFirstByUser_IdAndTypeAndRevokedAtIsNullOrderByGrantedAtDesc(Long userId, String type);
}
