# Hamyra 2.1 — Android Streaming Application & Cloud Build

[![Hamyra 2.1 - Android Cloud Build & Release](https://github.com/errordrive/Hamyra/actions/workflows/android-build.yml/badge.svg)](https://github.com/errordrive/Hamyra/actions/workflows/android-build.yml)

Complete source code, API backend, and cloud build pipeline for **Hamyra 2.1** (Application ID: `com.aistudio.bdstream.vfxrtq`).

---

## 🚀 Live Hosted Cloudflare Workers

Both microservices are hosted on Cloudflare Workers edge network:

1. **Main API Backend (`hamyra-api`)**
   - **Base URL:** `https://hamyra-api.hamyra.workers.dev`
   - **Healthcheck:** [`https://hamyra-api.hamyra.workers.dev/healthz`](https://hamyra-api.hamyra.workers.dev/healthz)
   - **4K HD Hub:** `https://hamyra-api.hamyra.workers.dev/tmdb/4khdhub?tmdbId=550&type=movie`
   - **Hindi Dubbed Anime:** `https://hamyra-api.hamyra.workers.dev/anime/hindi-dub?kind=movie&limit=40`
   - **Channels (HMAC Signed):** `https://hamyra-api.hamyra.workers.dev/channels`
   - **AI Recommendations:** `https://hamyra-api.hamyra.workers.dev/ai/recommend?q=action`

2. **Stream & HLS Proxy (`streambd-proxy`)**
   - **Base URL:** `https://streambd-proxy.hamyra.workers.dev`
   - **Toffee Cookie Generator:** [`https://streambd-proxy.hamyra.workers.dev/?get_cookie=1`](https://streambd-proxy.hamyra.workers.dev/?get_cookie=1)
   - **Token Encryptor:** `https://streambd-proxy.hamyra.workers.dev/encrypt?url=<stream_url>`
   - **HLS Manifest Proxy:** `https://streambd-proxy.hamyra.workers.dev/hls?t=<token>`

---

## 📱 What's New in Version 2.1

- **LibVLC 3.x Hardware Engine:** Bundled native VLC media stack (`libvlc.so` 46 MB) for hardware-accelerated playback of TS streams, RTMP, and AC3/DTS audio.
- **Offline Download Engine:** Foreground `DownloadService` with notification progress controls (`hamyra_downloads`).
- **4K HD Hub API:** Multi-tier ultra-HD link resolver prioritizing Cloudflare R2 / Hubcloud CDNs.
- **Bangla Subtitles API (`BnSubApi`):** Direct WebVTT subtitle tracks from `px.talkoraai.com`.
- **Dynamic Failover (`DynamicBackendConfig`):** Probes `/healthz` and automatically rotates between primary and secondary workers.
- **Android TV & Leanback:** Direct Android TV launcher and voice recognition support.

---

## 🛠️ Repository Structure

```
├── .github/workflows/
│   └── android-build.yml       # Cloud build workflow (AAPT2 + apksigner + Release)
├── apktool_project/            # Decoded APK resources, manifest, native libs, DEX
│   ├── AndroidManifest.xml
│   ├── res/                    # 109 decoded resource categories
│   ├── assets/                 # Fonts, app assets
│   └── lib/arm64-v8a/          # LibVLC native engine binaries
├── app_src/                    # Clean Java models, repositories, and configs
│   └── sources/com/example/
│       ├── config/             # AppConfig, DynamicBackendConfig
│       ├── data/download/      # DownloadService
│       ├── data/repository/    # FourKHDHubApi, BnSubApi, TmdbApi, BackendChannels...
│       └── data/model/         # ChannelItem, PlayableSource, Subtitle...
├── cloudflare_backend/         # Deployable hamyra-api Worker
└── cloudflare_proxy_worker/    # Deployable streambd-proxy Worker
```

---

## 📦 Cloud Build Workflow

The APK is built automatically in GitHub Actions on every push:
1. Compiles resources with **AAPT2** and assembles the APK.
2. Performs 4-byte boundary **zipalign**.
3. Signs with v1, v2, and v3 signature schemes using `apksigner`.
4. Uploads build artifacts and publishes a GitHub Release with `Hamyra-v2.1-signed.apk`.
