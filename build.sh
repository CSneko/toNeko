#!/usr/bin/env bash
# 任何一步失败立即退出，避免把旧构建产物当成果发布
set -euo pipefail

chmod +x gradlew
./gradlew runDatagen &      # 将命令放入后台运行
pid=$!              # 获取该命令的PID
sleep 60
kill -9 $pid       # 60s后强制kill

./gradlew build
mkdir -p result
cp -r fabric/build/libs/* result
cp -r neoforge/build/libs/* result
