package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRespository extends JpaRepository<Consent,Long> {
}
