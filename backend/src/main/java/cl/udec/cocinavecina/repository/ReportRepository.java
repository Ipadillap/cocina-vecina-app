package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
