package cl.udec.cocinavecina.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
@ConditionalOnWebApplication
public class LocalPhotosConfig implements WebMvcConfigurer {
    private final Path directory;

    public LocalPhotosConfig(@Value("${cocinavecina.photos.directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        final String location = directory.toUri().toString();
        registry.addResourceHandler("/photos/**")
            .addResourceLocations(location.endsWith("/") ? location : location + "/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic());
    }
}
