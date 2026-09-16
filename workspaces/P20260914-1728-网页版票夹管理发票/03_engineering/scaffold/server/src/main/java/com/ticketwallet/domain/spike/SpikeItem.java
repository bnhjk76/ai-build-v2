package com.ticketwallet.domain.spike;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** spike 验证用实体：覆盖 enum 列、numeric(12,2)、timestamptz 三种列类型的读写。 */
@Table("spike_items")
public class SpikeItem {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String title;

    private SpikeKind kind;

    private BigDecimal amount;

    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public SpikeKind getKind() { return kind; }
    public void setKind(SpikeKind kind) { this.kind = kind; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
