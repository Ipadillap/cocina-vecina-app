package cl.udec.cocinavecina.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NominatimGeocoder {

    public record Coordinates(double latitude, double longitude) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimResult(String lat, String lon) {}

    private final RestClient client;
    private final Map<String, Coordinates> cache = new ConcurrentHashMap<>();
    private long lastCall;

    public NominatimGeocoder(@Value("${cocinavecina.nominatim.url}") String url,
                             @Value("${cocinavecina.nominatim.contact}") String contact) {
        this.client = RestClient.builder()
            .baseUrl(url)
            .defaultHeader(HttpHeaders.USER_AGENT, "CocinaVecina/0.1 (" + contact + ")")
            .build();
    }

    public Optional<Coordinates> geocode(String address) {
        String key = address.trim().toLowerCase();
        Coordinates cached = cache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        waitForTurn();
        NominatimResult[] results = client.get()
            .uri(u -> u.path("/search")
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("countrycodes", "cl")
                .build())
            .retrieve()
            .body(NominatimResult[].class);
        if (results == null || results.length == 0) {
            return Optional.empty();
        }
        Coordinates coordinates = new Coordinates(
            Double.parseDouble(results[0].lat()), Double.parseDouble(results[0].lon()));
        cache.put(key, coordinates);
        return Optional.of(coordinates);
    }

    private synchronized void waitForTurn() {
        long wait = 1000 - (System.currentTimeMillis() - lastCall);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCall = System.currentTimeMillis();
    }
}
