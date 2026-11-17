package cl.udec.cocinavecina.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class FieldEncryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(@Value("${cocinavecina.encryption.key-base64}") String keyBase64) {
        final byte[] bytes = Base64.getDecoder().decode(keyBase64);
        if (bytes.length != 32) {
            throw new IllegalStateException("cocinavecina.encryption.key-base64 debe decodificar a 32 bytes");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            final byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            final byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el campo", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            final ByteBuffer data = ByteBuffer.wrap(Base64.getDecoder().decode(stored));
            final byte[] iv = new byte[IV_LENGTH];
            data.get(iv);
            final byte[] cipherText = new byte[data.remaining()];
            data.get(cipherText);
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el campo", e);
        }
    }
}
