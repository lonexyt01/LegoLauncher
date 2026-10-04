#!/usr/bin/env bash
# Linux/macOS: JDK 17 + Android SDK (ANDROID_HOME) + git kerak
set -e; cd "$(dirname "$0")"
[ -d upstream ] || git clone --depth 1 -b v3_openjdk --recurse-submodules https://github.com/PojavLauncherTeam/PojavLauncher upstream
bash rebrand.sh upstream
echo "sdk.dir=$ANDROID_HOME" > upstream/local.properties
(cd upstream && chmod +x gradlew && ./gradlew assembleDebug --no-daemon)
cp "$(find upstream -path '*build/outputs/apk/debug/*.apk' | head -1)" LegoLauncher.apk && echo "TAYYOR: LegoLauncher.apk"
