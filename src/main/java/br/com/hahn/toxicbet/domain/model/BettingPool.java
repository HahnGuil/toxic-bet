package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@Table("betting_pool")
public class BettingPool {

    @Id
    private Long id;
    private String bettingPoolName;
    private String bettingPoolKey;
    private Long bettingPoolOwnerId;
    private List<Long> users;
}
