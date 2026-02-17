package br.com.hahn.toxicbet.domain.model.enums;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum BaseValues {
    ODD_BASE_VALUE(10.0),
    PLUS_VALUE(1),
    INITIAL_ZERO(0),
    ADJUSTMENT_VALUE(1.0),
    MINIMAL_ODD_VALEU(1.0);

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
