package br.com.hahn.toxicbet.application.controller;

import br.com.hahn.toxicbet.infrastructure.service.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/test")
@AllArgsConstructor
public class ControllerTest {

    private final JwtService jwtService;

    @GetMapping
    public Mono<String> test() {
        return jwtService.updateOAuthUserApplication()
                .thenReturn("OK");
    }
}
