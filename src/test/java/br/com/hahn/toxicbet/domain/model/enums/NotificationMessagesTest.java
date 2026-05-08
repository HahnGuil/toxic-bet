package br.com.hahn.toxicbet.domain.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationMessagesTest {

    @Test
    void shouldFormatOpenBetsMessageInSingular() {
        assertEquals(
                "Existe 1 partida aberta para aposta, para o Copa Teste",
                NotificationMessages.openBets(1, "Copa Teste")
        );
    }

    @Test
    void shouldFormatOpenBetsMessageInPlural() {
        assertEquals(
                "Existem 4 partidas abertas para aposta, para o Copa Teste",
                NotificationMessages.openBets(4, "Copa Teste")
        );
    }
}
