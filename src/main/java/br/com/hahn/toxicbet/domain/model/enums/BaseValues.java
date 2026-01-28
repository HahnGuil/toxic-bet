package br.com.hahn.toxicbet.domain.model.enums;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum BaseValues {
    ODD_BASE_VALUE(2.0),
    PLUS_USER_VALUE(1),
    INITIAL_ZERO(0);

    private final Integer intValue;
    private final Double doubleValue;

    BaseValues(Double doubleValue) {
        this.doubleValue = doubleValue;
        this.intValue = null;
    }

    BaseValues(Integer intValue) {
        this.intValue = intValue;
        this.doubleValue = null;
    }

}
