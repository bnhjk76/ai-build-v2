package com.ticketwallet.domain.events;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** events 表（埋点，90 天保留）。 */
@Getter
@Setter
@Table("events")
public class Event {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String name;
    private String reason;
    private String userId;
    private String dims;
    private OffsetDateTime createdAt;
}
