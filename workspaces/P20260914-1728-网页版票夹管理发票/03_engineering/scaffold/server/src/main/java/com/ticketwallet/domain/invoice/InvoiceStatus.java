package com.ticketwallet.domain.invoice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 状态（normal 正常 / voided 作废 / reversed 红冲；默认 NORMAL，DP1）。 */
public enum InvoiceStatus {
    NORMAL, VOIDED, REVERSED;

    @JsonValue
    public String value() { return name().toLowerCase(); }

    @JsonCreator
    public static InvoiceStatus from(String v) {
        return valueOf(v.toUpperCase());
    }
}
