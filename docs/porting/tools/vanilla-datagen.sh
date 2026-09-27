#!/usr/bin/env bash
# Run the vanilla data generator for a Minecraft version, to get the exact built-in worldgen JSON
# (noise_settings, density functions, features, ...) and reports/registries.json for that version.
#
#   docs/porting/tools/vanilla-datagen.sh 26.3 [out-dir]
#
# Uses the server jar Loom already cached (run any Gradle build for that version first), and
# JDK 25 (it can also run the 1.20.1/1.21.1 generators).
# Output: <out-dir>/<version>/out/{data,reports}. Takes about a minute per version.
set -euo pipefail
ver="$1"
out="${2:-build/vanilla-datagen}"
java="${JAVA25:-$HOME/.jdks/openjdk-25.0.1/bin/java}"
jar="$HOME/.gradle/caches/fabric-loom/$ver/minecraft-server.jar"
[ -f "$jar" ] || { echo "no cached server jar at $jar; run a Gradle build for $ver first" >&2; exit 1; }
mkdir -p "$out/$ver"
cd "$out/$ver"
"$java" -DbundlerMainClass=net.minecraft.data.Main -jar "$jar" --server --reports --output out > gen.log 2>&1
echo "$ver: $(find out -type f | wc -l) files in $out/$ver/out"
