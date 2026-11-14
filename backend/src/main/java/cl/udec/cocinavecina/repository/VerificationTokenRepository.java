package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
}
