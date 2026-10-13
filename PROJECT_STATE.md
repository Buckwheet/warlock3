# Project State: Warlock 3 Single-Instance Architecture

## Goal
Implement single-instance multi-window architecture in Warlock 3 so multiple CLI launches (e.g. 9 characters) run as independent OS desktop windows hosted in a single JVM process, cutting memory from ~5 GB to under 800 MB.

## What changed
- Created GitHub fork at `https://github.com/Buckwheet/warlock3`.
- Reconfigured git remotes: `origin` points to `git@github.com:Buckwheet/warlock3.git` and `upstream` points to `sproctor/warlock3`.
- Switched to feature branch `feature/single-instance`.
- Installed portable Temurin JDK 21 (`C:\Users\rpgfi\.jdks\jdk-21.0.12.1+1`).
- Implemented `SingleInstance.kt`: TCP loopback socket listener/client with port file discovery (`%LOCALAPPDATA%/WarlockFE/warlock/single_instance.port`) and query-string-based zero-dependency payload serialization.
- Added `WindowBounds` data class, `initialBounds`, and `exitOnDisconnect` fields to `GameState.kt`.
- Updated `Main.kt` with `--single-instance` CLI flag (default true), launch delegation logic (~14ms handoff), dynamic `GameState` injection via `SingleInstanceHelper.startServer`, and independent multi-window Compose bounds rendering.
- Tuned runtime JVM ergonomics in `desktopApp/build.gradle.kts` (`-Xms48m -Xmx256m -XX:+UseG1GC -XX:MinHeapFreeRatio=15 -XX:MaxHeapFreeRatio=30`).
- Built standalone distributable binary via `:desktopApp:createDistributable` at `D:\GSIV Development\warlock3\desktopApp\build\potassium\binaries\main\app\warlock\warlock.exe`.
- Verified CLI options (`warlock.exe --help`) and tested handoff delegation.

## Commands run + results
- `gh repo fork sproctor/warlock3 --remote=true` -> Forked to `Buckwheet/warlock3`.
- `git remote rename origin upstream; git remote add origin git@github.com:Buckwheet/warlock3.git` -> Configured remotes.
- Portable Temurin JDK 21 downloaded and extracted to `C:\Users\rpgfi\.jdks\jdk-21.0.12.1+1`.
- `.\gradlew.bat :desktopApp:createDistributable` -> Build succeeded in 56s.
- `cmd.exe /c "warlock.exe --help"` -> Successfully displayed help with `--single-instance` flag and options.

## Files touched
- `compose/src/commonMain/kotlin/warlockfe/warlock3/compose/model/GameState.kt`
- `desktopApp/build.gradle.kts`
- `desktopApp/src/main/kotlin/warlockfe/warlock3/app/Main.kt`
- `desktopApp/src/main/kotlin/warlockfe/warlock3/app/SingleInstance.kt`
- `gradle.properties`
- `PROJECT_STATE.md`

## Next 3 actions
- [ ] Commit all single-instance changes to `feature/single-instance`.
- [ ] Push branch `feature/single-instance` to fork `origin` (`git@github.com:Buckwheet/warlock3.git`).
- [ ] Test live launch of multiple windows with the user and verify memory / window placement behavior.
