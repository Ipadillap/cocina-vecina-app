package cl.udec.cocinavecina.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** RB-6: ofuscación de la ubicación del punto de retiro (celda de 400 m). */
class ObfuscationTest {
    private static final double EARTH_RADIUS_M = 6_371_008.8;
    // Rectángulo aproximado del Gran Concepción
    private static final double LAT_MIN = -36.95;
    private static final double LAT_MAX = -36.70;
    private static final double LON_MIN = -73.20;
    private static final double LON_MAX = -72.95;

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLon = Math.toRadians(lon2 - lon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    @Test
    void maxErrorIsHalfTheCellDiagonal() {
        assertEquals(282.84, Obfuscation.maxErrorMeters(), 0.01);
    }

    @Test
    void cellCenterIsDeterministic() {
        final Obfuscation.Cell a = Obfuscation.cellCenter(-36.82701, -73.05012);
        final Obfuscation.Cell b = Obfuscation.cellCenter(-36.82701, -73.05012);
        assertEquals(a, b);
    }

    @Test
    void nearbyPointsInsideTheSameCellShareTheCenter() {
        final Obfuscation.Cell center = Obfuscation.cellCenter(-36.8270, -73.0500);
        final Obfuscation.Cell again = Obfuscation.cellCenter(center.latitude() + 0.0001, center.longitude() + 0.0001);
        assertEquals(center, again);
    }

    @Test
    void publishedCenterNeverExceedsTheoreticalErrorAcrossGranConcepcion() {
        final Random random = new Random(2026);
        double worst = 0;
        for (int i = 0; i < 100_000; i++) {
            final double lat = LAT_MIN + random.nextDouble() * (LAT_MAX - LAT_MIN);
            final double lon = LON_MIN + random.nextDouble() * (LON_MAX - LON_MIN);
            final Obfuscation.Cell c = Obfuscation.cellCenter(lat, lon);
            worst = Math.max(worst, haversine(lat, lon, c.latitude(), c.longitude()));
        }
        // Margen de 1 % por la variación de la longitud dentro del área (referencia fija en -36,827°).
        final double limit = Obfuscation.maxErrorMeters() * 1.01;
        final double observed = worst;
        assertTrue(observed <= limit, () -> "error máximo observado " + observed + " m > " + limit + " m");
    }

    @Test
    void distanceIsRoundedTo100Meters() {
        assertEquals(0, Obfuscation.roundDistance(49));
        assertEquals(100, Obfuscation.roundDistance(50));
        assertEquals(1200, Obfuscation.roundDistance(1234));
        assertEquals(1300, Obfuscation.roundDistance(1250));
    }
}
