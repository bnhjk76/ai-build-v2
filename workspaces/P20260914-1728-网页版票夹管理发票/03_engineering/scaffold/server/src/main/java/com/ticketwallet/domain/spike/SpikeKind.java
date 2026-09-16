package com.ticketwallet.domain.spike;

/**
 * spike 方言用例③：Java enum ↔ PostgreSQL enum 类型的 TypeHandler 映射验证。
 * PG 侧类型 spike_kind AS ENUM ('ELECTRONIC','PAPER')，见 V1__spike_init.sql。
 */
public enum SpikeKind {
    ELECTRONIC,
    PAPER
}
