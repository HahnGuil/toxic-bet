package br.com.hahn.toxicbet.infrastructure.config.kafka;

import br.com.hahn.toxicbet.application.service.UserService;
import br.com.hahn.toxicbet.domain.model.UserUpdateEvent;
import br.com.hahn.toxicbet.model.UserRequestDTO;
import br.com.hahn.toxicbet.util.DateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserUpdateConsumer {

    private final UserService userService;

    @Value("${spring.application.name}")
    private String applicationName;

    @KafkaListener(
            topics = "user-update",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userUpdateKafkaListenerContainerFactory"
    )
    public void updateUsersConsumer(UserUpdateEvent userUpdateEvent) {
        log.info("UserUpdateConsumer received: {}", userUpdateEvent);

        if (!applicationName.equals(userUpdateEvent.getApplicationName())) {
            log.debug("Ignoring event for applicationName={}, current={}",
                    userUpdateEvent.getApplicationName(), applicationName);
            return;
        }

        userService.updateUseradmin(userUpdateEvent.getEmail())
                .doOnSuccess(v -> log.info("User role updated to ADMIN for email={}", userUpdateEvent.getEmail()))
                .doOnError(e -> log.error("Error updating user role for email={}: {}", userUpdateEvent.getEmail(), e.getMessage(), e))
                .subscribe();
    }
}
