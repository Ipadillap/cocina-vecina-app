package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Device;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.ConsentRespository;
import cl.udec.cocinavecina.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class DeviceService {
    private final DeviceRepository devices;
    private final AppUserRepository users;
    private final ConsentRespository consents;

    public DeviceService(DeviceRepository devices, AppUserRepository users, ConsentRespository consents) {
        this.devices = devices;
        this.users = users;
        this.consents = consents;
    }

    @Transactional
    public void register(Long userId, String pushToken) {
        final AppUser user = users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("La cuenta debe estar verificada y activa");
        }
        if (!consents.existsByUser_IdAndTypeAndRevokedAtIsNull(userId, "NOTIFICATIONS")) {
            throw ApiException.forbidden("Falta el consentimiento de notificaciones");
        }
        final Device device = devices.findByPushToken(pushToken).orElseGet(() -> Device.builder()
            .pushToken(pushToken)
            .platform("ANDROID")
            .build());
        device.setUser(user);
        device.setRegisteredAt(OffsetDateTime.now());
        devices.save(device);
    }

    @Transactional
    public void unregister(Long userId, String pushToken) {
        devices.deleteByPushTokenAndUser_Id(pushToken, userId);
    }
}
