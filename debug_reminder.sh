#!/data/data/com.termux/files/usr/bin/bash
set -e

cd ~/TaskPulse

echo "[1/5] Building APK..."
./gradlew assembleDebug --console=plain || true

echo "[2/5] Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk || true

echo "[3/5] Clearing logcat..."
adb logcat -c || true

echo "[4/5] Capturing TaskPulse crash logs..."
mkdir -p logs
ts=$(date +%Y%m%d-%H%M%S)
adb logcat -v time | grep -i -E "AndroidRuntime|FATAL EXCEPTION|TaskPulse|com.taskpulse.app" > "logs/reminder-$ts.log"
