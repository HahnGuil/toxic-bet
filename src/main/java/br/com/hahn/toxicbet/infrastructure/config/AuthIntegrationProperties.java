package br.com.hahn.toxicbet.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "integration.auth")
public class AuthIntegrationProperties {

    private String baseUrl;
    private String applicationPublicId;
}
