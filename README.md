# RPS Clash (Batu Gunting Kertas) - Native Android Game (C++ NDK)

> **Tactical Rock Paper Scissors Mobile Game with Native C++ Core Engine & Real-Time Multiplayer**

---

## 🌟 Tentang Proyek

**RPS Clash** adalah mobile game Android native bertema **Batu Gunting Kertas (Rock Paper Scissors)** yang dirancang dengan standar produksi game mobile indie profesional:
- **Core Engine C++20 & NDK**: Logika pertandingan, kalkulasi skor, state machine, dan algoritma AI berjalan 100% pada native shared library (`libgamecore.so`).
- **AI Adaptif (Markov Chain)**: Bukan sekadar bot acak. Tersedia tingkat kesulitan **Mudah**, **Normal** (heuristik psikologis Win-Stay Lose-Shift), dan **Hard** (prediksi urutan pola Markov Chain).
- **Online Multiplayer Real-Time**: Duel real-time via WebSocket dengan arsitektur server-authoritative (anti-cheat), matchmaking instan, private room kode 6 digit, dan auto-reconnect grace period.
- **Sistem Audio Native Modular**: Background music ambient synth lo-fi loop dan sound effect dinamis (tap, select, countdown, win, lose, draw, match end).
- **Subtle Haptic Feedback Bridge**: Integrasi getaran taktil halus melalui C++ ↔ Android Native `Vibrator` / `VibrationEffect`.
- **Statistik Lengkap & Persisten**: Disimpan di internal storage lokal C++ tanpa reset tidak sengaja, terpisah untuk mode VS AI dan Online, lengkap dengan visualisasi persentase penggunaan elemen (Batu, Kertas, Gunting).
- **Desain UI Game-like & Elegan**: Visual bertema dark modern/neon, custom vector drawables (tanpa emoji placeholder), arena clash dinamis 60 FPS, dan navigasi mulus.

---

## 🚀 Cara Build Menjadi APK Menggunakan GitHub Actions

Proyek ini telah dilengkapi dengan workflow CI/CD otomatis di `.github/workflows/build-apk.yml`. Anda **tidak perlu menginstal Android SDK atau NDK di komputer lokal** untuk menghasilkan APK!

### Langkah-langkah:
1. **Inisialisasi Git dan Push ke Repository GitHub**:
   ```bash
   cd RPSClash
   git init
   git add .
   git commit -m "feat: initial commit RPS Clash native android game"
   git branch -M main
   git remote add origin https://github.com/USERNAME/REPO_NAME.git
   git push -u origin main
   ```
2. **Workflow Berjalan Otomatis**:
   - Begitu Anda melakukan `git push`, GitHub Actions akan otomatis mendeteksi file `.github/workflows/build-apk.yml`.
   - Workflow akan mengunduh Java 17, Android SDK 34, Android NDK `26.1.10909125`, dan CMake `3.22.1`.
   - Menjalankan `./gradlew assembleDebug` dan mengompilasi shared library C++ `libgamecore.so`.
3. **Mengunduh File APK**:
   - Buka tab **Actions** di repository GitHub Anda.
   - Klik workflow run yang sedang atau selesai berjalan.
   - Di bagian bawah halaman (bagian **Artifacts**), Anda dapat langsung mengunduh:
     - `RPSClash-Debug-APK` (File `.apk` siap diinstal di ponsel Android mana pun).

---

## 💻 Cara Build Lokal (Android Studio / CLI)

### Prasyarat
- **JDK 17** (OpenJDK / Temurin)
- **Android SDK** (API Level 34)
- **Android NDK** (`26.1.10909125` atau versi r25+)
- **CMake** (`3.22.1`+)

### Build via Command Line:
```bash
# Memberikan izin eksekusi gradlew
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Output APK berlokasi di:
# app/build/outputs/apk/debug/app-debug.apk
```

### Membuka di Android Studio:
1. Buka Android Studio.
2. Pilih **Open** lalu arahkan ke folder `RPSClash`.
3. Biarkan Gradle Sync selesai mengunduh dependensi dan mengonfigurasi CMake C++ NDK.
4. Hubungkan perangkat Android via USB Debugging atau gunakan Emulator.
5. Klik tombol **Run (Shift + F10)**.

---

## 🏛️ Arsitektur Proyek

```
RPSClash/
├── .github/
│   └── workflows/
│       └── build-apk.yml            # CI/CD GitHub Actions Build APK
├── app/
│   ├── build.gradle                 # Konfigurasi Gradle & integrasi CMake
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  # Manifest aplikasi
│           ├── cpp/                 # Native C++20 Core
│           │   ├── CMakeLists.txt
│           │   ├── game/            # Game state, Move & Result kalkulasi
│           │   │   ├── Move.hpp
│           │   │   ├── Result.hpp
│           │   │   ├── GameCore.hpp
│           │   │   └── GameCore.cpp
│           │   ├── ai/              # AI Engine (Markov Chain & Heuristics)
│           │   │   ├── Difficulty.hpp
│           │   │   ├── AIEngine.hpp
│           │   │   └── AIEngine.cpp
│           │   ├── stats/           # Statistics Engine
│           │   │   ├── StatsManager.hpp
│           │   │   └── StatsManager.cpp
│           │   ├── storage/         # Local Storage persistence
│           │   │   ├── LocalStorage.hpp
│           │   │   └── LocalStorage.cpp
│           │   ├── audio/           # Audio configuration engine
│           │   │   ├── AudioEngine.hpp
│           │   │   └── AudioEngine.cpp
│           │   ├── haptic/          # Haptic feedback dispatcher
│           │   │   ├── HapticBridge.hpp
│           │   │   └── HapticBridge.cpp
│           │   ├── online/          # Online match controller
│           │   │   ├── OnlineMatchController.hpp
│           │   │   └── OnlineMatchController.cpp
│           │   ├── network/         # Network message protocol definitions
│           │   │   ├── NetworkProtocol.hpp
│           │   │   └── NetworkProtocol.cpp
│           │   ├── utils/           # Logger & Mersenne Twister Random
│           │   │   ├── Logger.hpp
│           │   │   ├── Random.hpp
│           │   │   └── Random.cpp
│           │   └── jni/             # JNI export bridge
│           │       ├── JniHelpers.hpp
│           │       └── JniBridge.cpp
│           ├── java/com/kyuu/rpsclash/
│           │   ├── RPSApplication.kt
│           │   ├── MainActivity.kt  # View manager, animations, event bus
│           │   ├── nativebridge/    # JNI interface ke libgamecore.so
│           │   ├── audio/           # SoundManager & real-time sound synthesizer
│           │   ├── haptic/          # HapticManager (VibrationEffect)
│           │   ├── network/         # WebSocketManager & NetworkMonitor
│           │   ├── data/            # Preferences & GameStats models
│           │   └── ui/              # Custom views, arena clash, graphs, dialogs
│           └── res/                 # Vector drawables, colors, styles, strings
├── build.gradle                     # Top-level build script
├── settings.gradle
└── gradle.properties
```

---

## 🎮 Logika AI (Bukan Random Biasa)

- **Mudah (Easy)**:
  - 65% langkah acak.
  - 35% mengulang langkah pemain sebelumnya (mudah di-counter pemain).
- **Normal**:
  - Berdasarkan prinsip psikologi game *Win-Stay, Lose-Shift (WSLS)*.
  - Jika pemain baru saja menang, ada kecenderungan kuat mereka bertahan dengan elemen tersebut. AI meng-counter elemen pemenang pemain.
  - Jika pemain kalah, mereka biasanya beralih ke elemen yang mengalahkan elemen pemenang AI sebelumnya. AI mengantisipasi perpindahan ini.
- **Hard (Markov Chain N-Gram)**:
  - Merekam matriks transisi langkah $P(\text{Move}_t \mid \text{Move}_{t-1})$.
  - Mendeteksi pola siklus teratur (seperti Batu $\to$ Kertas $\to$ Gunting).
  - Menghitung frekuensi global dan memilih counter mutlak terhadap elemen dengan probabilitas tertinggi yang akan dikeluarkan pemain.

---

## 🌐 Koneksi ke Server Online

Backend server WebSocket terpisah berada pada direktori `../rps-clash-server`.
1. Jalankan server WebSocket di komputer atau VPS Anda (`npm start`).
2. Masuk ke **Pengaturan** di dalam game.
3. Masukkan WebSocket Server URL:
   - Jika bermain di Android Emulator dan server berjalan di komputer host: `ws://10.0.2.2:8080`
   - Jika bermain di HP fisik dalam jaringan Wi-Fi lokal yang sama: `ws://IP_KOMPUTER_ANDA:8080` (contoh: `ws://192.168.1.5:8080`)
   - Jika server telah dideploy ke cloud/VPS: `wss://rps.domainanda.com`
4. Tekan **SIMPAN SERVER URL**.

---

## 👨‍💻 Developer & Kredit

- **Developer**: Kyuu
- **GitHub**: [@KyuuX444](https://github.com/KyuuX444)
- **YouTube**: [@mommyykyuu](https://youtube.com/@momnykyuu)
- **Instagram**: [@kyzzapis](https://instagram.com/kyzzapis)
