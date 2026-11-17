package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);

    boolean existsByDisplayNameIgnoreCase(String displayName);

    Optional<AppUser> findByEmail(String email);

    @Query(
        """
            select u from AppUser u
            where u.deletionRequestedAt is not null
              and u.status <> :deleted
              and not exists (
                  select 1 from Order o
                  where (o.buyer = u or o.seller = u) and o.status in ('ACCEPTED', 'READY_FOR_PICKUP'))
            """
    )
    List<AppUser> findPendingDeletion(@Param("deleted") AccountStatus deleted);
}
