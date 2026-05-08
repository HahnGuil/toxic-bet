package br.com.hahn.toxicbet.domain.model.dto;

public record OpenBetNotificationTarget(
        Long subscriptionId,
        String endpoint,
        String p256dh,
        String auth,
        String championshipName,
        long openMatches
) {
}
