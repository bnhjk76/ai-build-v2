#!/bin/bash
# CI 契约三道防线（tech-stack §7.3；本地等价实现，CI 接入后同名挂载）
# 防线一：重导契约 → git diff（后端改注解未同步契约必红）
# 防线二：orval 重生成 → git diff（契约更新未重新生成必红）
# 防线三：tsc 类型对齐（手写 schema/调用与生成类型漂移编译期拦截）
set -e
cd "$(dirname "$0")/.."

echo "== 防线一：契约工件 diff =="
./server/scripts/export-openapi.sh >/dev/null
git add -N contracts/openapi.json 2>/dev/null || true
if ! git diff --exit-code contracts/; then
  echo "❌ 防线一：contracts/openapi.json 与 Controller 注解不一致，运行 server/scripts/export-openapi.sh 后提交"
  exit 1
fi
echo "✅ 防线一通过"

echo "== 防线二：生成物 diff =="
cd web && pnpm gen:api >/dev/null
if ! git diff --exit-code src/api/generated/; then
  echo "❌ 防线二：web/src/api/generated/ 与契约工件不一致，运行 pnpm gen:api 后提交"
  exit 1
fi
echo "✅ 防线二通过"

echo "== 防线三：类型对齐（typecheck） =="
pnpm typecheck
echo "✅ 防线三通过"
echo "🎉 契约三道防线全部通过"
