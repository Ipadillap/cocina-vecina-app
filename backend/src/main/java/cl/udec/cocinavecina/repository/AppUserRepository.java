package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);

    boolean existsByDisplayNameIgnoreCase(String displayName);

    Optional<AppUser> findByEmail(String email);
}
