package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}
