package br.com.hahn.toxicbet.application.service;

import br.com.hahn.toxicbet.domain.model.Championship;
import br.com.hahn.toxicbet.domain.repository.ChampionshipRepository;
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
class ChampionshipServiceTest {

    @Mock
    private ChampionshipRepository repository;

    @InjectMocks
    private ChampionshipService service;

    @Test
    void shouldFindChampionshipById() {
        Championship championship = new Championship();
        championship.setId(20L);
        championship.setName("World Cup");

        when(repository.findById(20L)).thenReturn(Mono.just(championship));

        StepVerifier.create(service.findById(20L))
                .expectNext(championship)
                .verifyComplete();

        verify(repository).findById(20L);
    }
}

