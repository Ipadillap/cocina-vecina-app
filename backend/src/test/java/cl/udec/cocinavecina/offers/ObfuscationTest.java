package cl.udec.cocinavecina.offers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ObfuscationTest {

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    @Test
    void cellCenterIsWithinHalfDiagonalOfRealPoint() {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            double lat = -36.95 + random.nextDouble() * 0.25;
            double lon = -73.20 + random.nextDouble() * 0.25;
            Obfuscation.Cell c = Obfuscation.cellCenter(lat, lon);
            assertThat(distanceMeters(lat, lon, c.latitude(), c.longitude()))
                    .isLessThanOrEqualTo(Obfuscation.maxErrorMeters() + 1);
        }
    }

    @Test
    void pointsInSameCellShareCenter() {
        Obfuscation.Cell a = Obfuscation.cellCenter(-36.82700, -73.05000);
        Obfuscation.Cell b = Obfuscation.cellCenter(-36.82701, -73.05001);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void distancesAreRoundedTo100Meters() {
        assertThat(Obfuscation.roundDistance(1149)).isEqualTo(1100);
        assertThat(Obfuscation.roundDistance(1150)).isEqualTo(1200);
        assertThat(Obfuscation.roundDistance(49)).isZero();
    }

    @Test
    void maxErrorIs283Meters() {
        assertThat(Obfuscation.maxErrorMeters()).isBetween(282.0, 283.0);
    }
}
