package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByRequest_IdInOrderByCreatedAtAscIdAsc(Collection<Long> requestIds);
}
