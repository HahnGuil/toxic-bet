package br.com.hahn.toxicbet.domain.model.enums;

public enum NotificationMessages {

    OPEN_BETS_SINGULAR("Existe %d partida aberta para aposta, para o %s"),
    OPEN_BETS_PLURAL("Existem %d partidas abertas para aposta, para o %s");

    private final String template;

    NotificationMessages(String template) {
        this.template = template;
    }

    public String format(Object... args) {
        return String.format(template, args);
    }

    public static String openBets(long count, String championshipName) {
        NotificationMessages message = count == 1 ? OPEN_BETS_SINGULAR : OPEN_BETS_PLURAL;
        return message.format(count, championshipName);
    }
}
