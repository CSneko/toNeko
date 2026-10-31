#!/usr/bin/env bash
# 任何一步失败立即退出，避免把旧构建产物当成果发布
set -euo pipefail

chmod +x gradlew

# CI 上不跑 datagen（产物已入库），只做编译打包；跳过 check/test 里依赖开发环境的回归用例
./gradlew build -x check -x test

mkdir -p result
cp -r fabric/build/libs/* result
cp -r neoforge/build/libs/* result
