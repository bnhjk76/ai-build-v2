package com.ticketwallet.domain.invoice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 介质（electronic 电子 / paper 纸质）。 */
public enum InvoiceMedium {
    ELECTRONIC, PAPER;

    @JsonValue
    public String value() { return name().toLowerCase(); }

    @JsonCreator
    public static InvoiceMedium from(String v) {
        return valueOf(v.toUpperCase());
    }
}
