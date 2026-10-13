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
- Optimized startup sequence in `Main.kt`: moved `SingleInstanceHelper.tryDelegateToExisting` to the very top of `run()`, allowing secondary CLI invocations to bypass database access, settings loading, and FileKit initialization completely (~15ms handoff).
- Added `WindowBounds` data class, `initialBounds`, and `exitOnDisconnect` fields to `GameState.kt`.
- Updated `Main.kt` with `--single-instance` CLI flag (default true), dynamic `GameState` injection via `SingleInstanceHelper.startServer`, and independent multi-window Compose bounds rendering.
- Tuned runtime JVM ergonomics in `desktopApp/build.gradle.kts` (`-Xms48m -Xmx256m -XX:+UseG1GC -XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=30`).
- Built and validated standalone distributable binary via `:desktopApp:createDistributable` at `D:\GSIV Development\warlock3\desktopApp\build\potassium\binaries\main\app\warlock\warlock.exe`.
- Cloned user's AutoIt Warlock Manager script (`C:\Lich5\WarlockManage 1.2 - Warlock.au3`) into:
  - Source script: `C:\Lich5\WarlockManage_SingleInstance.au3`
  - Compiled executable: `C:\Lich5\WarlockManage_SingleInstance.exe`
  - Desktop shortcut: `C:\Users\rpgfi\OneDrive\Desktop\Warlock Manager (Single-Instance).lnk`
- Configured cloned manager to:
  - Point `$g_sWarlockExe` directly to the new single-instance binary `D:\GSIV Development\warlock3\desktopApp\build\potassium\binaries\main\app\warlock\warlock.exe`.
  - Use port 7081 for its HTTP listener (avoiding conflict with existing manager on port 7080).
  - Scan both `%LOCALAPPDATA%\WarlockFE\warlock\` and package caches for saved character connections.
  - Show `"Warlock Manager (Single-Instance)"` in tray tooltip and notifications.
- Started `WarlockManage_SingleInstance.exe` (PID 12732); verified active status endpoint reporting existing characters (`Boiler: UP`, `Millhouse: UP`, `Ersetling: UP`).

## Commands run + results
- `.\gradlew.bat :desktopApp:createDistributable` -> Build succeeded in 18s.
- `Aut2Exe_x64.exe /in C:\Lich5\WarlockManage_SingleInstance.au3 /out C:\Lich5\WarlockManage_SingleInstance.exe` -> Successfully compiled standalone manager executable.
- `Invoke-CimMethod -ClassName Win32_Process -MethodName Create ...` -> Started PID 12732 as independent desktop process.
- `Invoke-WebRequest http://127.0.0.1:7081` -> Confirmed manager is live and detecting connected characters.

## Files touched
- `compose/src/commonMain/kotlin/warlockfe/warlock3/compose/model/GameState.kt`
- `desktopApp/build.gradle.kts`
- `desktopApp/src/main/kotlin/warlockfe/warlock3/app/Main.kt`
- `desktopApp/src/main/kotlin/warlockfe/warlock3/app/SingleInstance.kt`
- `gradle.properties`
- `PROJECT_STATE.md`
- `C:\Lich5\WarlockManage_SingleInstance.au3`
- `C:\Lich5\WarlockManage_SingleInstance.exe`
- `C:\Users\rpgfi\OneDrive\Desktop\Warlock Manager (Single-Instance).lnk`

## Next 3 actions
- [ ] Have user test launching a character from the new "Warlock Manager (Single-Instance)" tray icon.
- [ ] Confirm both windows run under single master PID and verify placement / memory usage.
- [ ] Retire or keep previous manager as desired.
