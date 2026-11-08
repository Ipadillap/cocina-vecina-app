package cl.udec.cocinavecina.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class DeliveryCodeTest {

    private final DeliveryCode codes =
            new DeliveryCode("test-key".getBytes(StandardCharsets.UTF_8), new SecureRandom());

    @Test
    void generatesSixDigits() {
        for (int i = 0; i < 1000; i++) {
            assertThat(codes.generate()).matches("\\d{6}");
        }
    }

    @Test
    void hmacOnlyValidatesCorrectCodeForSameOrder() {
        String code = codes.generate();
        String hmac = codes.hmac(10, code);
        assertThat(codes.matches(10, code, hmac)).isTrue();
        assertThat(codes.matches(11, code, hmac)).isFalse();
        assertThat(codes.matches(10, "abc123", hmac)).isFalse();
        assertThat(hmac).doesNotContain(code);
    }

    @Test
    void guessProbabilityIsFivePerMillion() {
        assertThat(DeliveryCode.guessProbability()).isEqualTo(5e-6);
    }
}
