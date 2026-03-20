package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Team;
import br.com.hahn.toxicbet.domain.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository repository;

    @InjectMocks
    private TeamService service;

    @Test
    void shouldFindTeamById() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Brazil");

        when(repository.findById(10L)).thenReturn(Mono.just(team));

        StepVerifier.create(service.findById(10L))
                .expectNext(team)
                .verifyComplete();

        verify(repository).findById(10L);
    }
}

