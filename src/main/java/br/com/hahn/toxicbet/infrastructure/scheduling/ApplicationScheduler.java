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

    private final MatchService matchService;

    @Scheduled(cron = "0 * * * * *")
    public void updateMatchesToInProgress(){
        log.info("ApplicationScheduler: Staring updating matches to IN_PROGRESS");
        matchService.updateMatchesToInProgress()
                .subscribe(
                        count -> log.info("ApplicationScheduler: Updating: {} matches to IN_PROCESS", count),
                        error -> log.error("ApplicationScheduler: Error to updating result matches: {}", error.getMessage()));
    }
}
