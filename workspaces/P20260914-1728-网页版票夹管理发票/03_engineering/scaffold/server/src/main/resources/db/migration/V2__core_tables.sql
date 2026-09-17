-- W1③ 业务表（architecture v2.0 §5.1 ER + §5.2 索引 1:1 落地）
-- spike 结论：PG enum + stringtype=unspecified 已验证可行（SPIKE-W1D1 方言用例③）
-- SPRING_SESSION/SPRING_SESSION_ATTRIBUTE 由 Spring Session 官方 schema 管理，不在此重复

CREATE TYPE invoice_category AS ENUM ('SPECIAL', 'GENERAL');
CREATE TYPE invoice_medium  AS ENUM ('ELECTRONIC', 'PAPER');
CREATE TYPE invoice_status  AS ENUM ('NORMAL', 'VOIDED', 'REVERSED');

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account         VARCHAR(200) NOT NULL,
    account_type    SMALLINT     NOT NULL,               -- 1=email 2=phone
    password_hash   VARCHAR(200) NOT NULL,               -- {argon2}/{bcrypt} 前缀共存
    failed_attempts INT          NOT NULL DEFAULT 0,     -- 连续失败计数（登录锁定）
    locked_until    TIMESTAMPTZ,                         -- 锁定截止，NULL=未锁
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_users_account ON users (account);

CREATE TABLE invoices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id),
    invoice_code   VARCHAR(20),                          -- 选填（数电票兼容）
    invoice_number VARCHAR(20) NOT NULL,                 -- 8-20 位数字
    issued_date    DATE         NOT NULL,                -- ≤今天（东八区业务口径）
    title          VARCHAR(100) NOT NULL,                -- 抬头 1-100
    amount         NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    tax_amount     NUMERIC(12,2) NOT NULL CHECK (tax_amount >= 0),
    total_amount   NUMERIC(12,2) NOT NULL,               -- 冗余=amount+tax_amount（服务端重算，不信前端）
    category       invoice_category NOT NULL,
    medium         invoice_medium   NOT NULL,
    status         invoice_status   NOT NULL DEFAULT 'NORMAL',
    remark         VARCHAR(200),
    deleted_at     TIMESTAMPTZ,                          -- 非空=回收站态
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE attachments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,                            -- 冗余：越权双保险
    filename    VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(20)  NOT NULL,                    -- image/jpeg|png|webp; application/pdf
    size        INT          NOT NULL,                    -- 字节 ≤10MB
    storage_key VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    reason     VARCHAR(200),
    user_id    UUID,
    dims       TEXT,                                       -- 筛选维度等 JSON
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ===== 索引设计（architecture §5.2，G2 达成路径）=====
-- 列表/筛选主索引：账号内按开票日期倒序（覆盖排序 + 软删过滤）
CREATE INDEX idx_inv_user_date ON invoices (user_id, issued_date DESC, created_at DESC)
  WHERE deleted_at IS NULL;
-- 抬头 contains 检索（ILIKE '%kw%'，pg_trgm 已随 V1 spike 迁移创建）
CREATE INDEX idx_inv_title_trgm ON invoices USING gin (title gin_trgm_ops)
  WHERE deleted_at IS NULL;
-- 金额区间筛选（作用于 total_amount）
CREATE INDEX idx_inv_total ON invoices (user_id, total_amount) WHERE deleted_at IS NULL;
-- 回收站倒序
CREATE INDEX idx_inv_recycle ON invoices (user_id, deleted_at DESC) WHERE deleted_at IS NOT NULL;
-- 附件计数前置校验
CREATE INDEX idx_att_invoice ON attachments (invoice_id);
