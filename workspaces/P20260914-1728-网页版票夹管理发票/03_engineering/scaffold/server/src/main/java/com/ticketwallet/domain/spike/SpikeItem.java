package com.ticketwallet.domain.spike;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** spike 验证用实体：覆盖 enum 列、numeric(12,2)、timestamptz 三种列类型的读写。D7：Lombok 简化存取器。 */
@Getter
@Setter
@Table("spike_items")
public class SpikeItem {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String title;

    private SpikeKind kind;

    private BigDecimal amount;

    private OffsetDateTime createdAt;
}
