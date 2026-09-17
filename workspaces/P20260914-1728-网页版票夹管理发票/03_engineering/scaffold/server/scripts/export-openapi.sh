#!/bin/bash
# 契约导出（tech-stack §7.2）：起应用 → 抓 /v3/api-docs → contracts/openapi.json → 停应用
# 用法：在 scaffold/ 下执行 ./server/scripts/export-openapi.sh
# 说明：springdoc-openapi-maven-plugin 对 Boot 4 的适配未核实（tech-stack §3.7 待核实），
#       先以等价脚本导出（机制一致：集成阶段抓取落盘），插件升级排 W1 验收轮。
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SERVER="$ROOT/server"
CONTRACTS="$ROOT/contracts"
mkdir -p "$CONTRACTS"

echo "[1/4] 构建并启动应用…"
cd "$SERVER"
./mvnw -q -B package -DskipTests
java -jar target/server-*.jar > /tmp/openapi-export-app.log 2>&1 &
APP_PID=$!
trap 'kill $APP_PID 2>/dev/null || true' EXIT

for i in $(seq 1 30); do
  sleep 2
  curl -sf http://localhost:8080/api/v1/health >/dev/null 2>&1 && break
done

echo "[2/4] 抓取 /v3/api-docs…"
curl -sf http://localhost:8080/v3/api-docs -o "$CONTRACTS/openapi.json"

echo "[3/4] 格式化（稳定 diff）…"
python3 -c "
import json
with open('$CONTRACTS/openapi.json') as f: d = json.load(f)
with open('$CONTRACTS/openapi.json', 'w') as f:
    json.dump(d, f, ensure_ascii=False, indent=2, sort_keys=True)
    f.write('\n')
print('openapi', d.get('openapi', '?'), '| paths:', len(d.get('paths', {})))
"

echo "[4/4] 完成：$CONTRACTS/openapi.json"
