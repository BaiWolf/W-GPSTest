# W-GPSTest

W-GPSTest is an Android GNSS tool based on [GPSTest by Sean Barbeau and contributors](https://github.com/barbeau/gpstest).

The original Status, Map, Sky, Accuracy, and diagnostic tools remain available. Overview and Dashboard appear first in the drawer, followed by the original destinations in their existing order. Both additions use the same GNSS data and Material theme:

- **Overview**: one row per satellite with the existing constellation flag and numeric satellite ID, strongest-signal C/N0 bars, frequency bands, fix participation, azimuth, elevation, and constellation filters. The total counts are followed by flag-marked used/in-view counts for each of the six main GNSS systems; reported SBAS operators are listed separately. Each satellite is counted once across its frequency bands, and the system counts reconcile with the totals. Summary totals use all reported constellations; filters only affect the satellite list. A fresh coordinate without any satellite marked as used shows an amber awaiting-confirmation status rather than a green confirmed fix.
- **Dashboard**: GNSS speed, time-weighted average and maximum speed, course over ground, mean-sea-level altitude when available (otherwise explicitly labeled WGS84 ellipsoid height), horizontal accuracy, local clock, UTC fix timestamp, and coordinates.

All 19 languages offered by the original language selector are covered by the new menus and pages. Persian uses right-to-left layout in the new pages. The compact Dashboard places speed and course side by side and keeps scrolling available for larger fonts or longer translations. Speed and distance units follow Settings. Live values expire after 15 seconds without a fresh position. Session speed statistics exclude pauses and gaps longer than 15 seconds and can be reset separately.

Application IDs: `com.baiwolf.wgpstest` (Google Maps) and `com.baiwolf.wgpstest.osmdroid` (OSM). These install alongside the upstream app. The namespace remains `com.android.gpstest` to preserve the existing implementation.

## Build and verify

The verified toolchain uses Android Gradle Plugin 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Hilt 2.57.2, and Android SDK 36. Gradle daemon JVM criteria select JetBrains JDK 21 through the Foojay resolver. The Google Debug build was compiled, installed over wireless debugging, and checked on a Pixel 11 (Android API 37), including Overview and Dashboard. Nine focused satellite-count, receiver-state, and motion-statistics regression tests passed with Kotlin 2.2.10.

The previous AGP 8.13.2 / Gradle 8.14.5 baseline is preserved in commit `3a70020e3083d5e072629ddfe736dc25c47b1298`. This upgrade retains the legacy Kotlin plugins and AGP DSL through the Upgrade Assistant compatibility flags in `gradle.properties`; these must be migrated before adopting AGP 10. The obsolete library ProGuard default was replaced with the supported optimizing template. OSM and Wear OS builds have not been verified as part of this migration.

The `3.10.6-w.2` Google Release APK and AAB were built and their signatures verified. The APK was checked for alignment and non-debuggable status, installed on a Pixel 11, and verified on-device. Dashboard speed and course headings now align; the course hint, GitHub drawer entry, and feedback drawer entry were removed. Help content stays in English, and About uses an inset-aware toolbar above scrolling content so its first lines remain visible.

The `3.10.6-w.3` Google Release AAB (version code `24103`) uses distributor-specific Maps configuration instead of the upstream API key. The signed bundle and embedded Maps key were verified, and Google Play updates and map loading passed manual testing on both Xiaomi and Pixel phones. For APK distribution outside Google Play, obtain a Play-signed APK without the automatic protection installer check; a protected universal APK can require users to reinstall from Google Play.

The `3.10.6-w.4` release (version code `24104`) adds a Privacy Policy entry to Help and a button below the introduction at the top of About. Both open the [W-GPSTest privacy policy](https://github.com/BaiWolf/W-GPSTest/wiki/Privacy-Policy) in the system browser. The new labels support English and Simplified Chinese. If no browser is available, a dialog displays a selectable policy address for copying.

The `3.10.6-w.5` release (version code `24105`) includes those privacy entries and removes the global `-dontobfuscate` and whole-application keep rules. Jackson names remain stable for reflective antenna JSON serialization while shrinking and optimization remain enabled; existing SlidingUpPanel compatibility rules are retained. The signed Google Release APK/AAB passed signature and packaging checks, all 21 existing unit tests passed, and an R8 9.4.24 host smoke test preserved antenna-style nested JSON data. This host test is not an Android device test. R8 metadata reports 83.91% obfuscation coverage; uncompressed DEX size decreased from 4,122,636 to 3,010,672 bytes compared with w.4. Archive the matching `mapping.txt` for every published version; the AAB also embeds it.

The minified Google Release APK was installed on a Pixel 11 (Android API 37). Startup, live satellite reception, Overview, Dashboard, Status, Sky, the Accuracy form, both privacy entries, CSV status/orientation logging, and the Android share-sheet preview passed device checks without matched fatal, linkage, inflation, or serialization errors. The device does not advertise antenna-info support, so real antenna JSON callbacks could not be exercised; the host smoke test covers serialization only. The Accuracy view inflated successfully, but the ground-truth sliding-panel workflow was not exercised. Local Google Maps loading was blocked by the API key's signing-certificate restriction, with an explicit authorization error; verify map loading through the next Google Play internal-test release, as requested.

Open the project in Android Studio and select the Google Debug variant. From a configured command line:

```shell
./gradlew :GPSTest:assembleGoogleDebug :GPSTest:testGoogleDebugUnitTest
```

Google Maps builds read `MAPS_API_KEY` from the root `secrets.properties` file (ignored by Git), or from the Gradle property of the same name. Copy `secrets.properties.example` to `secrets.properties` and enter your own key. Without a key, the app builds but Google Maps cannot load. Enable Maps SDK for Android and billing for your Google Cloud project, restrict the key to that SDK, and authorize `com.baiwolf.wgpstest` with the SHA-1 fingerprints of every applicable Google Play app signing certificate. For locally installed builds, also authorize their local signing certificate; the upload certificate alone does not authorize Play-installed builds.

Release builds read local signing settings from `.signing/release.properties` by default, or from the file specified by `-Psecure.properties=<path>`. The properties are `key.store` (relative to `GPSTest/`, or an absolute path), `key.alias`, `key.storepassword`, and `key.keypassword`. Keep the keystore and passwords backed up outside this checkout; `.signing/` is ignored by Git. Build the signed Google Maps APK with `./gradlew :GPSTest:assembleGoogleRelease`, or its Android App Bundle with `./gradlew :GPSTest:bundleGoogleRelease`. Use the same signing key for subsequent APK updates.

## App icon

The app uses a white W, blue orbit, and satellite sphere with restrained shading on a dark indigo gradient. Generated artwork is stored in `icons/w-gpstest-launcher-foreground.png`. Adaptive color and monochrome layers use the same silhouette within the central safe circle; round and legacy icons are included. Regenerate the packaged resources with `java tools/GenerateLauncherAssets.java` using JDK 17 or newer. The assets are separate from the upstream GPSTest artwork.

## Attribution and license

This project retains the upstream Apache 2.0 license and copyright notices. W-GPSTest additions do not imply endorsement by the upstream authors. The original project documentation follows below.

---

# <img src="/icons/ic_launcher-playstore.png" width="50"/> GPSTest 
[![Build Status](https://github.com/barbeau/gpstest/actions/workflows/android.yml/badge.svg)](https://github.com/barbeau/gpstest/actions/workflows/android.yml) 
[![GitHub issues](https://img.shields.io/github/issues/barbeau/gpstest?color=red)](https://github.com/barbeau/gpstest/issues)
[![Twitter Follow](https://img.shields.io/twitter/follow/sjbarbeau.svg?style=social&label=Follow)](https://twitter.com/sjbarbeau)
[![GitHub](https://img.shields.io/github/license/barbeau/gpstest)](/LICENSE)

The #1 open-source global navigation satellite system (GNSS) testing app.

## Install

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.android.gpstest)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.android.gpstest.osmdroid/)

## Features

It supports dual-frequency¹ GNSS for:

* GPS (USA Navstar)
* GLONASS (Russia)
* QZSS (Japan)
* BeiDou/COMPASS (China)
* Galileo (European Union)
* IRNSS/NavIC (India)
* Various satellite-based augmentation systems (SBAS):
    * Wide Area Augmentation System (WAAS) (USA)
    * European Geostationary Navigation Overlay Service (EGNOS) (European Union)
    * GPS-aided GEO augmented navigation (GAGAN) (India)
    * Multi-functional Satellite Augmentation System (MSAS) (Japan)
    * System for Differential Corrections and Monitoring (SDCM) (Russia)
    * Satellite Navigation Augmentation System (SNAS) (China)
    * Soluciόn de Aumentaciόn para Caribe, Centro y Sudamérica (SACCSA) (ICAO)
    * Southern Positioning Augmentation Network (SouthPAN) (Australia / New Zealand)
    
¹*Dual-frequency GNSS requires device hardware support and Android 8.0 Oreo or higher. See [Dual-frequency GNSS on Android](https://medium.com/@sjbarbeau/dual-frequency-gnss-on-android-devices-152b8826e1c) for more details.*

</details>

## Screenshots
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" height="500">

<details>
  <summary>More screenshots</summary>
  
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/8.jpg" height="500">
  
</details>

## Contributing

I welcome contributions to the project!  Please see the [Contributing Guide](.github/CONTRIBUTING.md) for details, including Code Style Guidelines and Template.

- Don't know where to start?  Take a look at the issues marked with the [your-first-pr](https://github.com/barbeau/gpstest/labels/your-first-pr) label and comment to let me know if you're interested in working on it.

- Beta testing: Get early access to new GPSTest versions, and help us squash bugs! See the [Testing Guide](BETA_TESTING.md) for details.

- Translating: Want to improve existing translations, or add a new translation?  Translate on [Transifex](https://app.transifex.com/sean-barbeau/gpstest-android/dashboard/) or see the [Translations](/TRANSLATIONS.md) documentation.

## FAQ

Questions?  Check out the [FAQ](FAQ.md), the [Slack group](https://gpstest-android.herokuapp.com/), and [Google Group](https://groups.google.com/forum/#!forum/gpstest_android).

- Crowdsourcing GNSS data: Interested in better understanding the state of GNSS feature support on Android devices? See the article [*Crowdsourcing GNSS features of Android devices*](https://barbeau.medium.com/crowdsourcing-gnss-capabilities-of-android-devices-d4228645cf25).

- Accuracy: Measuring your device GNSS accuracy? Check out the [Measuring Accuracy using GPSTest](ACCURACY.md) page, as well as the corresponding article [*Measuring GNSS accuracy on Android devices*](https://medium.com/@sjbarbeau/measuring-gnss-accuracy-on-android-devices-6824492a1389).

- Data Output and Logging: Want to know how to output GNSS data to the system log for further analysis?  Check out the [Data Output and Logging](LOGGING.md) page.

- Building the project: You can open and build this project using [Android Studio](https://developer.android.com/studio).  For more details, see the [Build documentation](BUILD.MD).

## Trusted by industry experts

Notable appearances of GPSTest:

* Xiaomi - [*Xiaomi Redmi Note 9 Pro Max launch*](https://youtu.be/Y_5cfCZBOV4?t=3035), March 12, 2020.
* European Union Global Navigation Satellite Systems Agency (GSA) - [*Test your Android device’s satellite navigation performance*](https://www.euspa.europa.eu/newsroom-events/news-archive/test-your-android-devices-satellite-navigation-performance), August 21, 2018.
