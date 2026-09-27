#!/bin/bash
# usage: smoke2.sh <dir> <commands file> <java args...>; fresh world, waits for load, runs console commands, stops
dir=$1; cmds=$2; shift 2
cd "$dir" || exit 1
rm -rf world logs
J=${JAVA:-java}
(
  for i in $(seq 1 300); do
    grep -qE 'Done \(|Exception in server tick loop|Crash report saved|Failed to start the minecraft server' logs/latest.log 2>/dev/null && break
    sleep 2
  done
  sleep 3
  while IFS= read -r cmd; do echo "$cmd"; sleep 8; done < "$cmds"
  echo stop
  sleep 60
) | "$J" -Xmx3G "$@" nogui > console.log 2>&1
echo "$dir exit=$?"
