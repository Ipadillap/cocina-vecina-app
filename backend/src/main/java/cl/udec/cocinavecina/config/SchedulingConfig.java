package cl.udec.cocinavecina.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cocinavecina.tasks.enabled", havingValue = "true")
public class SchedulingConfig {
}
