package br.com.hahn.toxicbet.infrastructure.scheduling;

import br.com.hahn.toxicbet.application.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationScheduler {

    public static final String ERROR_SCHEDULER = "ApplicationScheduler: Error to updating result matches";
    private final MatchService matchService;


    @Scheduled(cron = "0 * * * * *")
    public void updateMatchesToInProgress(){
        log.info("ApplicationScheduler: Staring updating matches to IN_PROGRESS");
        matchService.updateMatchesToInProgress()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Updating: {} matches to IN_PROCESS", count),
                        error -> log.error(ERROR_SCHEDULER + ": {}", error.getMessage()));
    }

    @Scheduled(cron = "0 * * * * *")
    public void updateMatchesToOpenToBetting(){
        log.info("ApplicationScheduler: Starting updating matches to OPEN_TO_BETTING");
        matchService.autoOpenMatchToBets()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Updating: {} matches to OPENT_TO_BETTING", count),
                        error -> log.error(ERROR_SCHEDULER + ": {}", error.getMessage()));
    }
}
