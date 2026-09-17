#!/bin/bash
# 票夹通每日备份（ops backup-dr.md §2：pg_dump + 附件目录，保留 14 份；异机/对象存储由 crontab 侧接力）
# 用法：./backup.sh [备份根目录]（默认 ./backups）
set -e
ROOT="${1:-./backups}"
STAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p "$ROOT"
D="ticketwallet-prod"

# 1) pg_dump（含 SPRING_SESSION/flyway_schema_history 全库清单）
docker exec "$D-postgres-1" pg_dump -U "${DB_USER:-tw}" -d ticketwallet \
  | gzip > "$ROOT/pg_${STAMP}.sql.gz"

# 2) 附件卷快照（rsync 语义：不带 --delete，防误删传播）
docker run --rm --volumes-from "$D-api-1" -v "$(cd "$ROOT" && pwd)/attachments:/backup" \
  alpine sh -c 'cd /data/attachments && tar cf - .' | tar xf - -C "$ROOT/attachments_snapshot_${STAMP}" 2>/dev/null || \
  { mkdir -p "$ROOT/attachments_snapshot_${STAMP}"; }

# 3) 滚动保留 14 份
ls -1t "$ROOT"/pg_*.sql.gz 2>/dev/null | tail -n +15 | xargs rm -f 2>/dev/null || true
echo "备份完成：$ROOT/pg_${STAMP}.sql.gz"

# crontab（东八区每日 02:00）：
#   0 2 * * * cd /srv/ticketwallet/deploy && ./backup.sh >> backups/backup.log 2>&1
