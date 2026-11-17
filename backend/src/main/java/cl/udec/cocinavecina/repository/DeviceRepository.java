package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByPushToken(String pushToken);

    void deleteByPushTokenAndUser_Id(String pushToken, Long userId);

    void deleteByUser_Id(Long userId);
}
