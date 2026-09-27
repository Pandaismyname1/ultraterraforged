#!/usr/bin/env bash
# Compile the current common sources against another Minecraft version, in a throwaway copy,
# to get a list of compile errors ("compile census"). Nothing in the repo is changed.
#
#   docs/porting/tools/census-build.sh <mc-version> <terrablender-common-version> <work-dir>
#   e.g. census-build.sh 1.21.1 1.21.1-4.1.0.8 /tmp/rtf-census
#        census-build.sh 26.3   26.3-26.3.0.0.7 /tmp/rtf-census
#
# Then: python docs/porting/tools/census.py <out> 1.21.1=<work>/1.21.1.log 26.1=<work>/26.1.log ...
#
# 1.21.1 builds with the remapping Loom plugin (obfuscated game, Mojang mappings, Java 21).
# 26.x builds with dev.architectury.loom-no-remap (unobfuscated game, no mappings, Java 25).
# Needs JDK 21 and 25 in ~/.jdks (override with JDK21 / JDK25).
set -euo pipefail
ver="$1"; tb="$2"; work="$3"
repo="$(git rev-parse --show-toplevel)"
dir="$work/src-$ver"
rm -rf "$dir"; mkdir -p "$dir"
git -C "$repo" archive HEAD | tar -x -C "$dir"
cd "$dir"
rm -rf .github forge fabric
sed -i 's/include("common", "fabric", "forge")/include("common")/' settings.gradle

case "$ver" in
  1.*)
    jdk="${JDK21:-$HOME/.jdks/graalvm-ce-21.0.2}"
    sed -i "s/^minecraft_version=.*/minecraft_version=$ver/; s/^java_version=.*/java_version=21/; s/^terrablender_version=.*/terrablender_version=${tb#*-}/" gradle.properties
    sed -i 's/common("forge", "fabric")/common("fabric", "neoforge")/; s/TerraBlender-forge/TerraBlender-common/' common/build.gradle
    cat >> build.gradle <<'EOF'

allprojects {
    tasks.withType(JavaCompile).configureEach {
        options.compilerArgs += ["-Xmaxerrs", "20000", "-Xmaxwarns", "0"]
    }
}
EOF
    ;;
  *)
    jdk="${JDK25:-$HOME/.jdks/openjdk-25.0.1}"
    echo "toolchainVersion=25" > gradle/gradle-daemon-jvm.properties
    sed -i 's/gradle-9\.[0-9.]*-bin/gradle-9.5.0-bin/' gradle/wrapper/gradle-wrapper.properties
    # no mappings: the access widener must use the "official" namespace
    sed -i '1s/.*/accessWidener\tv2\tofficial/' common/src/main/resources/reterraforged.accesswidener
    cat > build.gradle <<'EOF'
plugins {
    id "architectury-plugin" version "3.5.170"
    id "dev.architectury.loom-no-remap" version "1.17.493" apply false
}
architectury { minecraft = rootProject.minecraft_version }
allprojects {
    apply plugin: "java"
    apply plugin: "architectury-plugin"
    version = "0"
    repositories { maven { url = "https://maven.minecraftforge.net/" } }
    tasks.withType(JavaCompile).configureEach {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs += ["-Xmaxerrs", "20000", "-Xmaxwarns", "0"]
    }
}
EOF
    cat > common/build.gradle <<EOF
apply plugin: "dev.architectury.loom-no-remap"
architectury { common("fabric", "neoforge") }
loom { accessWidenerPath = file("src/main/resources/reterraforged.accesswidener") }
dependencies {
    minecraft "com.mojang:minecraft:\${rootProject.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:\${rootProject.fabric_loader_version}"
    compileOnly "com.github.glitchfiend:TerraBlender-common:$tb"
}
EOF
    sed -i "s/^minecraft_version=.*/minecraft_version=$ver/" gradle.properties
    ;;
esac

JAVA_HOME="$jdk" ./gradlew :common:compileJava --console=plain --no-daemon > "$work/$ver.log" 2>&1 || true
echo "$ver: $(grep -c 'error:' "$work/$ver.log") error lines (each printed twice) -> $work/$ver.log"
