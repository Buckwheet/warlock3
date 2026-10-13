# Project State: Warlock 3 Single-Instance Architecture

## Goal
Implement single-instance multi-window architecture in Warlock 3 so multiple CLI launches (e.g. 9 characters) run as independent OS desktop windows hosted in a single JVM process, cutting memory from ~5 GB to under 800 MB.

## What changed
- Created GitHub fork at `https://github.com/Buckwheet/warlock3`.
- Reconfigured git remotes: `origin` points to `git@github.com:Buckwheet/warlock3.git` and `upstream` points to `sproctor/warlock3`.
- Switched to feature branch `feature/single-instance`.
- Installed portable Temurin JDK 21 (`C:\Users\rpgfi\.jdks\jdk-21.0.12.1+1`).
- Implemented `SingleInstance.kt`: TCP loopback socket listener/client with port file discovery (`%LOCALAPPDATA%/WarlockFE/warlock/single_instance.port`) and query-string-based zero-dependency payload serialization.
- Hardened IPC parser against UTF-8 byte-order marks (`\uFEFF`) and whitespace trimming.
- Optimized startup sequence in `Main.kt`: moved `SingleInstanceHelper.tryDelegateToExisting` to the very top of `run()`, allowing secondary CLI invocations to bypass database access, settings loading, and FileKit initialization completely.
- Added `WindowBounds` data class, `initialBounds`, and `exitOnDisconnect` fields to `GameState.kt`.
- Updated `Main.kt` with `--single-instance` CLI flag (default true), dynamic `GameState` injection via `SingleInstanceHelper.startServer`, and independent multi-window Compose bounds rendering.
- Tuned runtime JVM ergonomics in `desktopApp/build.gradle.kts` (`-Xms48m -Xmx256m -XX:+UseG1GC -XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=30`).
- Built and validated standalone distributable binary via `:desktopApp:createDistributable` at `D:\GSIV Development\warlock3\desktopApp\build\potassium\binaries\main\app\warlock\warlock.exe`.
- Successfully ran live multi-window tests:
  - Master instance opened at port 53640.
  - Secondary instance handed off window parameters in **15.8 ms** with zero disk I/O.
  - Master instance hosted both windows inside a single process consuming only **390 MB** committed memory (compared to ~1,075 MB for 2 un-shared instances, an ~64% immediate reduction).
- Committed and pushed initial single-instance feature to `origin/feature/single-instance`.

## Commands run + results
- `.\gradlew.bat :desktopApp:createDistributable` -> Build succeeded in 32s.
- `Measure-Command { & $warlockExe --width 900 --height 650 -x 200 -y 200 }` -> Delegation completed in 15.8 ms.
- `Get-Process -Name warlock` -> Confirmed 2 windows running inside PID 13360 with 390 MB committed memory.

## Files touched
- `compose/src/commonMain/kotlin/warlockfe/warlock3/compose/model/GameState.kt`
- `desktopApp/build.gradle.kts`
- `desktopApp/src/main/kotlin/warlockfe/warlock3/app/Main.kt`
- `desktopApp/src/main/kotlin/warlockfe/warlock3/app/SingleInstance.kt`
- `gradle.properties`
- `PROJECT_STATE.md`

## Next 3 actions
- [ ] Commit optimized early-delegation and BOM-hardening changes to `feature/single-instance`.
- [ ] Push latest commits to `origin/feature/single-instance`.
- [ ] Present test results and options to the user for running the new build or packaging an NSIS installer.
