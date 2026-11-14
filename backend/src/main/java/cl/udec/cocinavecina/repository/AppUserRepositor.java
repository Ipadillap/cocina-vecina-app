package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepositor extends JpaRepository<AppUser, Integer> {
}
