package br.com.hahn.toxicbet.domain.model.dto;

import br.com.hahn.toxicbet.domain.model.enums.Result;

public record BetResultResponseDTO(
        String homeTeamName,
        String visitingTeamName,
        Result matchResult,
        Result betResult,
        Double betOdds,
        Double betPoints,
        Integer homeTeamScore,
        Integer visitingTeamScore,
        Result resultado
) {}
