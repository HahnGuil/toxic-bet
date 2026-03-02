package br.com.hahn.toxicbet.domain.model.dto;

import java.util.UUID;

public record BettingPoolDTO(UUID betPoolOwnerId, String betPoolName, String betPoolKey) {
}
