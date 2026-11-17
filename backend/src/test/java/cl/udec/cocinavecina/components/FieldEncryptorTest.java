package cl.udec.cocinavecina.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** RNF-S-3: cifrado AES-256-GCM de la dirección, coordenadas y teléfono. */
class FieldEncryptorTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY;

    static {
        final byte[] other = new byte[32];
        other[0] = 1;
        OTHER_KEY = Base64.getEncoder().encodeToString(other);
    }

    private final FieldEncryptor encryptor = new FieldEncryptor(KEY);

    @Test
    void roundTripPreservesText() {
        final String address = "Los Carrera 1234, depto 5, Concepción";
        assertEquals(address, encryptor.decrypt(encryptor.encrypt(address)));
    }

    @Test
    void ciphertextDoesNotContainThePlaintext() {
        final String phone = "+56912345678";
        final String stored = encryptor.encrypt(phone);
        assertFalse(stored.contains(phone));
        final String decoded = new String(Base64.getDecoder().decode(stored), StandardCharsets.ISO_8859_1);
        assertFalse(decoded.contains(phone));
    }

    @Test
    void sameInputGivesDifferentCiphertextBecauseOfRandomIv() {
        assertNotEquals(encryptor.encrypt("-36.8270"), encryptor.encrypt("-36.8270"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        final byte[] data = Base64.getDecoder().decode(encryptor.encrypt("Barros Arana 500"));
        data[data.length - 1] ^= 0x01;
        final String tampered = Base64.getEncoder().encodeToString(data);
        assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tampered));
    }

    @Test
    void anotherKeyCannotDecrypt() {
        final String stored = encryptor.encrypt("O'Higgins 100");
        assertThrows(IllegalStateException.class, () -> new FieldEncryptor(OTHER_KEY).decrypt(stored));
    }

    @Test
    void rejectsKeysThatAreNot256Bits() {
        final String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalStateException.class, () -> new FieldEncryptor(shortKey));
    }

    @Test
    void nullStaysNull() {
        assertNull(encryptor.encrypt(null));
        assertNull(encryptor.decrypt(null));
    }
}
