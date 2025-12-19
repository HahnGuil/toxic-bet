package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@Data
@Table("betting_pool")
@RequiredArgsConstructor
public class BettingPool {

    @Id
    private Long id;

    @Column("betting_pool_name")
    private String bettingPoolName;

    @Column("betting_pool_key")
    private String bettingPoolKey;

    @Column("betting_pool_owner_id")
    private UUID bettingPoolOwnerId;

    @Column("users")
    private List<UUID> users;
}
