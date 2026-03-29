# TaskPulse 🔔

A premium Android task reminder app with full-screen overlay reminders (Truecaller-style).

## Tech Stack
- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM** + Clean Architecture
- **Hilt** (Dependency Injection)
- **Room** (Local database)
- **WorkManager** + **AlarmManager** (Exact alarms)
- **Coroutines + Flow**

## Features
- ✅ Create tasks with date, time, recurrence
- ✅ Full-screen overlay reminder at exact scheduled time
- ✅ Works on lock screen
- ✅ Snooze (5/10/15/30 min / 1hr / 2hr)
- ✅ Priority levels (Low / Medium / High / Critical)
- ✅ Categories with color tags
- ✅ Calendar view
- ✅ Statistics screen
- ✅ Auto-reschedule on device reboot
- ✅ Dark theme

## Setup

### 1. Clone & Open
```bash
git clone <your-repo>
```
Open in **Android Studio Ladybug** or newer.

### 2. Build
```bash
./gradlew assembleDebug
```

### 3. Required Permissions (first launch)
The app will guide you through:
1. **Post Notifications** (Android 13+)
2. **Display over other apps** → Settings → Apps → TaskPulse → Display over other apps
3. **Schedule exact alarms** → Settings → Apps → TaskPulse → Alarms & reminders
4. **Battery optimization** → Ignore battery optimization for best reliability

### 4. Testing Overlay on Emulator
Overlay windows may not work correctly on all emulators.
**Test on a physical device** for the overlay reminder feature.

### 5. Quick local validation
Use the helper script for a basic sanity pass before PR/release:

```bash
./scripts/validate_local.sh
```

It runs:
- `:app:assembleDebug`
- `:app:lintDebug`
- `:app:testDebugUnitTest`

If it fails with `JAVA_HOME is not set`, install JDK 17+ and export `JAVA_HOME`.

## Architecture

```
app/
├── data/          # Room DB, Repositories, DataStore
├── domain/        # Models, UseCases, Repository interfaces
├── presentation/  # Compose screens + ViewModels
├── overlay/       # OverlayService + OverlayScreen
└── worker/        # AlarmScheduler, BroadcastReceivers, WorkManager
```

## Key Files
| File | Purpose |
|------|---------|
| `OverlayService.kt` | Foreground service that shows full-screen reminder |
| `OverlayScreen.kt` | Compose UI for the overlay popup |
| `ExactAlarmScheduler.kt` | Schedules exact alarms via AlarmManager |
| `TaskAlarmReceiver.kt` | BroadcastReceiver triggered by alarm |
| `BootReceiver.kt` | Re-schedules alarms after device reboot |

## Notes
- Min SDK: 26 (Android 8.0)
- Uses `AlarmManager.setAlarmClock` for reliable reminders on OEM-customized devices
- On Android 12+: requests `SCHEDULE_EXACT_ALARM` permission (user grant in Alarms & reminders is still required for exact delivery)
- Font: Plus Jakarta Sans (via Google Fonts)
