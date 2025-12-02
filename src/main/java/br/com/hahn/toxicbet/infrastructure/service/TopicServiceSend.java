package br.com.hahn.toxicbet.infrastructure.service;

import br.com.hahn.toxicbet.domain.model.UserSyncEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TopicServiceSend {

    private final KafkaProducerService kafkaProducerService;

    public void updateOAuthUserApplicatioToToxicBet(UserSyncEvent userSyncEvent){
        log.info("Passando no send");
        kafkaProducerService.sendUserSyncEvent(userSyncEvent);
    }
}
