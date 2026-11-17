package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.dto.UploadUrl;
import cl.udec.cocinavecina.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PhotoService {
    private static final int MAX_BYTES = 1_048_576;
    private static final Pattern KEY = Pattern.compile(
        "^([0-9]+)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|webp)$"
    );

    private final Path directory;

    public PhotoService(@Value("${cocinavecina.photos.directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public UploadUrl prepareUpload(Long userId, String contentType) {
        final String extension = extension(contentType);
        if (extension == null) {
            throw ApiException.rule("RF-2", "Solo se admite JPEG o WebP");
        }
        final String key = userId + "-" + UUID.randomUUID() + "." + extension;
        return new UploadUrl(key, "/v1/photos/local/" + key, "PUT", contentType);
    }

    public void storeLocal(Long userId, String key, String contentType, InputStream body) {
        final Matcher matcher = KEY.matcher(key);
        if (!matcher.matches() || !matcher.group(1).equals(userId.toString())) {
            throw ApiException.forbidden("La clave de subida no pertenece al usuario");
        }
        final String mediaType = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
        final String extension = extension(mediaType);
        if (extension == null || !extension.equals(matcher.group(2))) {
            throw ApiException.rule("RF-2", "El tipo de contenido debe ser JPEG o WebP y coincidir con la clave");
        }
        final byte[] data;
        try {
            data = body.readNBytes(MAX_BYTES + 1);
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer el cuerpo de la solicitud");
        }
        if (data.length == 0 || data.length > MAX_BYTES) {
            throw ApiException.rule("RF-2", "La foto debe pesar entre 1 byte y 1 MB");
        }
        if (!hasMagicBytes(extension, data)) {
            throw ApiException.rule("RF-2", "El archivo no es una imagen " + extension.toUpperCase() + " válida");
        }
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(key), data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException e) {
            throw ApiException.conflict("Esa foto ya fue subida");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la foto", e);
        }
    }

    private static String extension(String contentType) {
        return switch (contentType == null ? "" : contentType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> null;
        };
    }

    private static boolean hasMagicBytes(String extension, byte[] d) {
        if ("jpg".equals(extension)) {
            return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
        }
        return d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
            && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
    }
}
