package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("push_subscription")
@RequiredArgsConstructor
public class PushSubscription {

    @Id
    private Long id;

    @Column("user_id")
    private UUID userId;

    @Column("endpoint")
    private String endpoint;

    @Column("p256dh")
    private String p256dh;

    @Column("auth")
    private String auth;

    @Column("user_agent")
    private String userAgent;

    @Column("active")
    private Boolean active;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column("last_failure_at")
    private LocalDateTime lastFailureAt;
}
