package com.com4energy.processor.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Genera un identificador único para esta instancia de la aplicación.
 * Se utiliza para marcar locks distribuidos en la base de datos.
 */
@Component
@Getter
public class InstanceIdentifier {
    private final String instanceId = UUID.randomUUID().toString();
}

