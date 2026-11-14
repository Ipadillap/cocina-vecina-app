package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
}
