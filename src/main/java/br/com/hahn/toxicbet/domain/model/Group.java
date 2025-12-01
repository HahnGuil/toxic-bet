package br.com.hahn.toxicbet.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@Table("group")
public class Group {

    @Id
    private Long id;
    private String groupName;
    private String groupKey;
    private Long groupOwnerId;
    private List<Long> users;
}
