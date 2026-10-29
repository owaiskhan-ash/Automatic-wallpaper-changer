# AutoWall

**An automatic wallpaper changer for Android — minimal, dark, no ads, no accounts.**

Pick the photos you like, set how often they should change, and AutoWall quietly rotates
them in the background. Every wallpaper is rendered for your exact screen size, with
rounded corners, an optional colour wash, and your choice of crop.

[![Build APK](https://github.com/owaiskhan-ash/Automatic-wallpaper-changer/actions/workflows/android.yml/badge.svg)](https://github.com/owaiskhan-ash/Automatic-wallpaper-changer/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/owaiskhan-ash/Automatic-wallpaper-changer?color=111111)](https://github.com/owaiskhan-ash/Automatic-wallpaper-changer/releases/latest)

### [⬇ Download AutoWall.apk](https://github.com/owaiskhan-ash/Automatic-wallpaper-changer/releases/latest/download/AutoWall.apk)

Android 8.0 (API 26) and newer · ~7 MB · release-signed in CI

---

## Try the UI in your browser

The app's single screen is reproduced as a working, interactive preview — toggle rotation,
pick a photo, drag the corner radius, switch tones:

```bash
python3 -m http.server 4173 --directory docs
# then open http://localhost:4173
```

Or just open [`docs/index.html`](docs/index.html) directly in a browser.

## What it does

| | |
|---|---|
| **Rotate on a schedule** | 5 minutes → 24 hours. Intervals under 15 minutes use a self-chaining worker, the rest use WorkManager periodic work. Survives reboots and app sweeps. |
| **Shape the wallpaper** | Renders each photo at your screen's exact resolution: corner radius 0–64 dp, `Fill` (crop) or `Fit` (whole photo over a blurred backdrop). |
| **Tone it down** | Seven colour washes (Midnight, Dusk, Forest, Ember, Ocean, Mono) with adjustable strength. |
| **Your photos only** | Picked through the Android photo picker and copied into app-private storage, so rotation keeps working even if you delete the original. No internet permission is requested — nothing leaves the device. |
| **Lock screen** | Optionally set the same wallpaper on the lock screen. |
| **Apply now** | Tap any thumbnail (or the refresh button) to set that photo immediately. |

## Install

1. Download [`AutoWall.apk`](https://github.com/owaiskhan-ash/Automatic-wallpaper-changer/releases/latest/download/AutoWall.apk).
2. Open the file on your phone; when Android asks, allow *install unknown apps* for your browser or file manager.
3. Pick photos → flip the switch. The first wallpaper applies straight away.
4. Optional: exclude AutoWall from aggressive battery managers (Xiaomi, Oppo, Vivo) so rotation is never delayed.

Verify the download if you like:

```bash
sha256sum -c sha256sums.txt   # file attached to each release
```

## Build it yourself

The APK on the release page is built by [`.github/workflows/android.yml`](.github/workflows/android.yml)
on every push, then attached to a release.

```bash
git clone https://github.com/owaiskhan-ash/Automatic-wallpaper-changer.git
cd Automatic-wallpaper-changer
./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Requirements: JDK 17 and the Android SDK (platform 34, build-tools 34). `local.properties`
must point at your SDK, or set `ANDROID_HOME`.

## Signing

Release builds are signed with the keystore in [`signing/`](signing). It is committed on
purpose so CI can produce upgrades that install over the previous build, and so the project
is reproducible. It is **not** a secret Play Store key — generate your own if you plan to
publish commercially.

```
SHA-256: 71:C8:03:4E:68:32:BE:21:57:22:C7:7C:B6:4F:AE:38:C7:2C:83:6A:FD:7C:73:34:7F:AF:05:0C:B8:D6:DA:D9
```

## Permissions

| Permission | Why |
|---|---|
| `SET_WALLPAPER`, `SET_WALLPAPER_HINTS` | The whole point. |
| `RECEIVE_BOOT_COMPLETED` | Re-arm the schedule after a restart. |
| Photo picker access | Read-only access to only the photos you select. |
| — | No internet, no location, no contacts, no ads SDK. |

## How it works

```
MainActivity ── Compose UI (HomeScreen) ── HomeViewModel ─┬─ SettingsRepository (DataStore)
                                                          ├─ GalleryStore (app-private copies)
                                                          ├─ WallpaperEngine (render + setBitmap)
                                                          └─ Scheduler ── RotationWorker (WorkManager)
                                                                          └─ BootReceiver re-arms after reboot
```

* **< 15 min intervals** — WorkManager's minimum periodic period is 15 minutes, so shorter
  intervals enqueue a one-time worker that re-enqueues itself when it finishes.
* **Rendering** — the photo is sampled down to screen size, composited onto an ARGB bitmap
  with a rounded-corner mask, optional blurred backdrop (`Fit`) and tone overlay, then handed
  to `WallpaperManager.setBitmap(..., FLAG_SYSTEM | FLAG_LOCK)`.

## Project layout

```
app/src/main/java/com/autowall/changer/
├── MainActivity.kt          # edge-to-edge Compose host
├── BootReceiver.kt          # re-arms rotation after boot / update
├── data/                    # DataStore settings + private photo copies
├── model/                   # settings, fit modes, tones, intervals
├── ui/                      # single screen, components, theme
├── wallpaper/               # bitmap renderer + WallpaperManager bridge
└── work/                    # WorkManager scheduler + rotation worker
docs/                        # interactive UI preview (static HTML)
signing/                     # release keystore used by CI
```
