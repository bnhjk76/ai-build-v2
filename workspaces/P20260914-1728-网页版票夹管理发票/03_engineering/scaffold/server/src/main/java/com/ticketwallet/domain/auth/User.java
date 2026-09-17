package com.ticketwallet.domain.auth;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;

import java.time.OffsetDateTime;

/** users 表（ER §5.1）：登录锁定字段留在业务表，不交给 Spring Session。 */
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public Integer getAccountType() { return accountType; }
    public void setAccountType(Integer accountType) { this.accountType = accountType; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
    public OffsetDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(OffsetDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
