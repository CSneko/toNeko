chmod +x gradlew
./gradlew runDatagen &      # 将命令放入后台运行
pid=$!              # 获取该命令的PID
sleep 60
kill -9 $pid       # 60s后强制kill

./gradlew build
mkdir result
cp -r fabric/build/libs/* result
cp -r neoforge/build/libs/* result
