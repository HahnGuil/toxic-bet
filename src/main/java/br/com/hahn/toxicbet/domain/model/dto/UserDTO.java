package br.com.hahn.toxicbet.domain.model.dto;

import java.util.UUID;

public record UserDTO(UUID userId, String userEmail) {
}