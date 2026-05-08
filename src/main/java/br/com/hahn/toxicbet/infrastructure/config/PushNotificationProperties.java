package br.com.hahn.toxicbet.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifications.push")
public record PushNotificationProperties(
        boolean enabled,
        String publicKey,
        String privateKey,
        String vapidSubject,
        int ttlSeconds,
        int sendConcurrency
) {
    public boolean hasVapidKeys() {
        return publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }

    public int effectiveTtlSeconds() {
        return ttlSeconds > 0 ? ttlSeconds : 3600;
    }

    public int effectiveSendConcurrency() {
        return sendConcurrency > 0 ? sendConcurrency : 8;
    }
}
