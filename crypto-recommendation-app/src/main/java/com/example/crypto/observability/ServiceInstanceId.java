package com.example.crypto.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides an identifier for the running service instance (pod/container).
 */
@Component
public class ServiceInstanceId {

    private final String id;

    public ServiceInstanceId(@Value("${service.instance-id:}") String configured) {
        this.id = Optional.ofNullable(configured)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> Optional.ofNullable(System.getenv("SERVICE_INSTANCE_ID"))
                        .filter(s -> !s.isBlank())
                        .orElseGet(() -> Optional.ofNullable(System.getenv("HOSTNAME"))
                                .filter(s -> !s.isBlank())
                                .orElse(UUID.randomUUID().toString())));
    }

    /**
     * @return service instance id
     */
    public String id() {
        return id;
    }
}
