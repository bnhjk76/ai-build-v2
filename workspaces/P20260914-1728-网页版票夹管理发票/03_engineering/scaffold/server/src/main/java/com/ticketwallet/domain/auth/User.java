package com.ticketwallet.domain.auth;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** users 表（ER §5.1）：登录锁定字段留在业务表，不交给 Spring Session。D7：Lombok 简化存取器。 */
@Getter
@Setter
@Table("users")
public class User {

    @Id
    private String id;                 // UUID

    private String account;

    private Integer accountType;       // 1=email 2=phone

    private String passwordHash;

    private Integer failedAttempts;

    private OffsetDateTime lockedUntil;

    private OffsetDateTime createdAt;
}
