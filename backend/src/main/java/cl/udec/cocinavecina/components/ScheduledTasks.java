package cl.udec.cocinavecina.components;

import cl.udec.cocinavecina.service.AppUserService;
import cl.udec.cocinavecina.service.MaintenanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.IntSupplier;

@Slf4j
@Component
@ConditionalOnProperty(name = "cocinavecina.tasks.enabled", havingValue = "true")
public class ScheduledTasks {
    private final MaintenanceService maintenance;
    private final AppUserService users;

    public ScheduledTasks(MaintenanceService maintenance, AppUserService users) {
        this.maintenance = maintenance;
        this.users = users;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void closeExpired() {
        run("expireOffers", maintenance::expireOffers);
        run("expireRequests", maintenance::expireRequests);
        run("expirePickupWindows", maintenance::expirePickupWindows);
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 30_000)
    public void finalizeDeletions() {
        run("finalizePendingDeletions", users::finalizePendingDeletions);
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Santiago")
    public void cleanup() {
        run("purgePhotos", maintenance::purgePhotos);
        run("purgeAudit", maintenance::purgeAudit);
        run("purgeVerificationTokens", maintenance::purgeVerificationTokens);
        run("purgeRefreshTokens", maintenance::purgeRefreshTokens);
    }

    private static void run(String name, IntSupplier task) {
        try {
            final int affected = task.getAsInt();
            if (affected > 0) {
                log.info("Tarea {}: {} registros", name, affected);
            }
        } catch (RuntimeException e) {
            log.error("Falló la tarea {}", name, e);
        }
    }
}
