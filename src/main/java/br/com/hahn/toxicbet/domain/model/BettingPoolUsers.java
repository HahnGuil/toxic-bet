package br.com.hahn.toxicbet.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("betting_pool_users")
@RequiredArgsConstructor
public class BettingPoolUsers {

    @Id
    private Long id;

    @NotNull
    @Column("betting_pool_id")
    private Long bettingPoolId;

    @NotNull
    @Column("user_id")
    private UUID userId;

    @Column("joined_at")
    private LocalDateTime jointAt;
}
