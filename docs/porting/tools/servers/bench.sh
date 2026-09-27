#!/bin/bash
# usage: bench.sh <server dir> <label> <seed> <level type> <preset json or -> <area x> <area z>
# Run from Git Bash on Windows (process CPU time comes from PowerShell), in a server folder set up for the loader, with
# UltraTerraForged and any mods under test in mods/. JAVA picks the Java, SERVER_ARGS replaces "-jar server.jar" (Forge:
# "@libraries/net/minecraftforge/forge/<version>/win_args.txt"), JAVA_EXTRA adds JVM options such as a flight recording.
# Fresh world; records the spawn preparation time, then forceloads 32x32 chunks at the area and measures wall time and
# process CPU time until generation goes idle. Appends a line to bench-results.txt in the server folder.
dir=$1; label=$2; seed=$3; type=$4; preset=$5; ax=$6; az=$7
# forceload areas must line up with chunks, or each command covers 17 chunks a side, over the limit of 256
ax=$(( ax / 16 * 16 )); az=$(( az / 16 * 16 ))
cd "$dir" || exit 1
rm -rf world logs
sed -i -e "s/^level-seed=.*/level-seed=$seed/" -e "s/^level-type=.*/level-type=${type//:/\\\\:}/" server.properties
if [ "$preset" != "-" ]; then cp "$preset" config/ultraterraforged/server-preset.json; fi
# the Java the server runs on; 17 for 1.20.1
J=${JAVA:-java}
log=$PWD/bench-$label-$seed.log
: > "$log"

cpu() { powershell.exe -NoProfile -Command "(Get-Process -Id $1).TotalProcessorTime.TotalMilliseconds" | tr -d '\r'; }
now() { date +%s%3N; }

(
  for i in $(seq 1 300); do
    grep -qE 'Done \(|Exception in server tick loop|Crash report saved|Failed to start the minecraft server' logs/latest.log 2>/dev/null && break
    sleep 2
  done
  pid=$(powershell.exe -NoProfile -Command "(Get-CimInstance Win32_Process | Where-Object { \$_.CommandLine -like '*utfbench*' -and \$_.Name -like 'java*' }).ProcessId" | tr -d '\r' | head -1)
  sleep 5
  startCpu=$(cpu $pid); start=$(now)
  echo "pid $pid cpu at done $startCpu" >> "$log"
  x2=$((ax + 255)); x3=$((ax + 256)); x4=$((ax + 511))
  z2=$((az + 255)); z3=$((az + 256)); z4=$((az + 511))
  echo "forceload add $ax $az $x2 $z2"
  echo "forceload add $x3 $az $x4 $z2"
  echo "forceload add $ax $z3 $x2 $z4"
  echo "forceload add $x3 $z3 $x4 $z4"
  last=$startCpu; lastT=$start; busyT=$start; busyCpu=$startCpu; idle=0
  while [ $idle -lt 3 ]; do
    sleep 2
    c=$(cpu $pid); t=$(now)
    # cores busy over the interval
    load=$(awk -v c=$c -v l=$last -v t=$t -v lt=$lastT 'BEGIN { printf "%.2f", (c - l) / (t - lt) }')
    echo "$((t - start)) ms load $load" >> "$log"
    if awk -v x=$load 'BEGIN { exit !(x < 1.0) }'; then idle=$((idle + 1)); else idle=0; busyT=$t; busyCpu=$c; fi
    last=$c; lastT=$t
    [ $((t - start)) -gt 900000 ] && break
  done
  echo "forceload done wall $((busyT - start)) cpu $(awk -v a=$busyCpu -v b=$startCpu 'BEGIN { printf "%.0f", a - b }')" >> "$log"
  echo stop
  sleep 5
) | "$J" -Xmx4G -Dutfbench=1 $JAVA_EXTRA ${SERVER_ARGS:--jar server.jar} nogui > console.log 2>&1

spawn=$(grep -o 'Time elapsed: [0-9]* ms' logs/latest.log | grep -o '[0-9]*' | head -1)
done_s=$(grep -o 'Done ([0-9.]*s)' logs/latest.log | grep -o '[0-9.]*' | head -1)
fl=$(grep 'forceload done' "$log")
loaded=$(grep -c 'Marked\|marked' logs/latest.log)
errors=$(grep -cE 'ERROR|Exception' logs/latest.log)
echo "$label seed=$seed spawn_ms=$spawn done_s=$done_s $fl errors=$errors" | tee -a bench-results.txt
