package com.ticketwallet.domain.invoice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 票种（api-design §3：special 专票 / general 普票；DB enum 值=枚举名，spike 验证 stringtype 路线）。 */
public enum InvoiceCategory {
    SPECIAL, GENERAL;

    @JsonValue
    public String value() { return name().toLowerCase(); }

    @JsonCreator
    public static InvoiceCategory from(String v) {
        return valueOf(v.toUpperCase());
    }
}
