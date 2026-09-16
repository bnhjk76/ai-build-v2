-- W1-D1 spike 专用 schema（验证后保留为 spike 期间工作表，业务表 V2 起（W1③任务）另建）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 方言用例③：PG enum 类型（不适配则降级 varchar + CHECK，仅动迁移脚本）
CREATE TYPE spike_kind AS ENUM ('ELECTRONIC', 'PAPER');

CREATE TABLE spike_items (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    kind       spike_kind   NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 方言用例②：抬头模糊检索的 pg_trgm GIN 索引（architecture §5.3 继承）
CREATE INDEX idx_spike_items_title_trgm ON spike_items USING gin (title gin_trgm_ops);
