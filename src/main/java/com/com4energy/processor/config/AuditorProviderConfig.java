package com.com4energy.processor.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class AuditorProviderConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Más adelante leer del SecurityContextHolder
        return () -> Optional.of("SYSTEM");
    }

}
