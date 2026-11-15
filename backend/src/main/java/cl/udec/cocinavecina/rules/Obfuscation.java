package cl.udec.cocinavecina.rules;

public final class Obfuscation {

    public static final double CELL_SIZE_METERS = 400.0;
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;
    private static final double REFERENCE_LATITUDE = -36.827;
    private static final double LATITUDE_STEP = CELL_SIZE_METERS / METERS_PER_DEGREE_LATITUDE;
    private static final double LONGITUDE_STEP =
        CELL_SIZE_METERS / (METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(REFERENCE_LATITUDE)));

    private Obfuscation() {}

    public record Cell(double latitude, double longitude) {}

    public static Cell cellCenter(double latitude, double longitude) {
        double lat = (Math.floor(latitude / LATITUDE_STEP) + 0.5) * LATITUDE_STEP;
        double lon = (Math.floor(longitude / LONGITUDE_STEP) + 0.5) * LONGITUDE_STEP;
        return new Cell(lat, lon);
    }

    public static long roundDistance(double meters) {
        return Math.round(meters / 100.0) * 100;
    }

    public static double maxErrorMeters() {
        return CELL_SIZE_METERS * Math.sqrt(2) / 2;
    }
}
