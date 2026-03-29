# Regression Checklist (Core Flows)

Use this checklist before release candidates. Keep it quick and focused.

## 1) AI Chat Core
- [ ] Open **AI Scheduler Chat** from Home and return via back button.
- [ ] Send a normal text request and verify:
  - [ ] User and AI bubbles remain visually distinct (no redesign drift).
  - [ ] Message rhythm/grouping is readable across consecutive messages.
  - [ ] Typing indicator appears only while AI response is pending.
- [ ] Send blank input attempt and confirm graceful error state.
- [ ] Create Task CTA appears only when draft is present and is actionable.

## 2) Voice Note UX
- [ ] Start recording with microphone permission already granted.
- [ ] Verify recording state banner and timer are visible while active.
- [ ] Stop recording after >0.5s and confirm voice note appears in thread.
- [ ] Stop recording too quickly (<0.5s) and verify clear short-note feedback.
- [ ] Deny microphone permission and confirm fallback message is clear and actionable.
- [ ] Play/pause a recorded voice note and verify progress indicator + timestamps update.
- [ ] Verify missing/unplayable voice file shows safe fallback messaging (no crash).

## 3) Trust Messaging / Reminder Accuracy
- [ ] Create task with alarm permission ON → message must state reminder scheduled.
- [ ] Create task with alarm permission OFF → message must not claim reminder scheduled.
- [ ] Simulate/observe schedule failure path → message must state task created but reminder unconfirmed.

## 4) Task Flow No-Regression
- [ ] Home list still loads tasks and due highlights render as before.
- [ ] Task details open/edit/delete still work.
- [ ] Calendar and Stats screens open without crash.
- [ ] Settings screen still reachable and permission flows intact.

## 5) Local Validation Commands
Run from project root:

```bash
./scripts/validate_local.sh
# If needed, run individually:
./gradlew :app:assembleDebug --console=plain
./gradlew :app:lintDebug --console=plain
./gradlew :app:testDebugUnitTest --console=plain
```

If Java/JDK is unavailable, install JDK 17+ and set `JAVA_HOME`, then rerun the commands above.
