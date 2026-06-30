package br.com.hahn.toxicbet.infrastructure.scheduling;

import br.com.hahn.toxicbet.application.service.MatchService;
import br.com.hahn.toxicbet.application.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationScheduler {

    public static final String ERROR_SCHEDULER = "ApplicationScheduler: Error to updating result matches";
    private static final String BRASILIA_TIME_ZONE = "America/Sao_Paulo";
    private final MatchService matchService;
    private final PushNotificationService pushNotificationService;


    @Scheduled(cron = "10 0/30 * * * *", zone = BRASILIA_TIME_ZONE)
    public void updateMatchesToInProgress(){
        log.info("ApplicationScheduler: Staring updating matches to IN_PROGRESS");
        matchService.updateMatchesToInProgress()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Updating: {} matches to IN_PROCESS", count),
                        error -> log.error(ERROR_SCHEDULER + ": {}", error.getMessage()));
    }

    @Scheduled(cron = "0 0 0 * * *", zone = BRASILIA_TIME_ZONE)
    public void updateMatchesToOpenToBetting() {
        log.info("ApplicationScheduler: Starting updating matches to OPEN_TO_BETTING");
        matchService.autoOpenMatchToBets()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Updating: {} matches to OPEN_TO_BETTING", count),
                        error -> log.error(ERROR_SCHEDULER + ": {}", error.getMessage()));
    }

    @Scheduled(cron = "0 */5 * * * *", zone = BRASILIA_TIME_ZONE)
    public void openPenalMatch() {
        log.info("ApplicationScheduler: Starting openPenalMatch");
        matchService.autoOpenPenalMatchToBets()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Updating: {} penal matches to OPEN_TO_BETTING", count),
                        error -> log.error(ERROR_SCHEDULER + ": {}", error.getMessage()));
    }

    @Scheduled(cron = "0 0 9 * * *", zone = BRASILIA_TIME_ZONE)
    public void deleteOldMatches(){
        log.info("ApplicationScheduler: Starting to delete old matches");
        matchService.deleteOndMatch()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Delete: {} old matchs", count),
                        error -> log.error(ERROR_SCHEDULER + ": {}", error.getMessage()));
    }

    @Scheduled(cron = "0 0 9,14,20 * * *", zone = BRASILIA_TIME_ZONE)
    public void notifyOpenMatchesForBetting() {
        log.info("ApplicationScheduler: Starting open matches push notification");
        pushNotificationService.notifyOpenBets()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Sent {} open match notifications", count),
                        error -> log.error("ApplicationScheduler: Error sending open match notifications: {}", error.getMessage()));
    }
}
