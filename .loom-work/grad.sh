#!/usr/bin/env bash
# usage: grad.sh <logfile> [gradle args...]
LOG=$1; shift
cd /media/crystalneko/enkoo/java/toNeko
GRADLE_USER_HOME=/media/crystalneko/enkoo/java/toNeko/.gradle-home nohup ./gradlew "$@" --console=plain > "$LOG" 2>&1 &
echo $! > /tmp/grad.pid
echo "started pid $(cat /tmp/grad.pid) log $LOG"
