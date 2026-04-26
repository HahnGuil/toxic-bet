package br.com.hahn.toxicbet.infrastructure.service;

import br.com.hahn.toxicbet.domain.model.UserSyncEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserSyncEvent(UserSyncEvent event) {
        kafkaTemplate.send("sync-application", event.getUuid(), event);
    }
}
