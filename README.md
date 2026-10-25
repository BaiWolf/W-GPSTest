# W-GPSTest

W-GPSTest is an Android GNSS tool based on [GPSTest by Sean Barbeau and contributors](https://github.com/barbeau/gpstest).

The original Status, Map, Sky, Accuracy, and diagnostic tools remain available. Overview and Dashboard appear first in the drawer, followed by the original destinations in their existing order. Both additions use the same GNSS data and Material theme:

- **Overview**: one row per satellite with the existing constellation flag and numeric satellite ID, strongest-signal C/N0 bars, frequency bands, fix participation, azimuth, elevation, and constellation filters. The total counts are followed by flag-marked used/in-view counts for each of the six main GNSS systems; reported SBAS operators are listed separately. Each satellite is counted once across its frequency bands, and the system counts reconcile with the totals. Summary totals use all reported constellations; filters only affect the satellite list. A fresh coordinate without any satellite marked as used shows an amber awaiting-confirmation status rather than a green confirmed fix.
- **Dashboard**: GNSS speed, time-weighted average and maximum speed, course over ground, mean-sea-level altitude when available (otherwise explicitly labeled WGS84 ellipsoid height), horizontal accuracy, local clock, UTC fix timestamp, and coordinates.

All 19 languages offered by the original language selector are covered by the new menus and pages. Persian uses right-to-left layout in the new pages. The compact Dashboard places speed and course side by side and keeps scrolling available for larger fonts or longer translations. Speed and distance units follow Settings. Live values expire after 15 seconds without a fresh position. Session speed statistics exclude pauses and gaps longer than 15 seconds and can be reset separately.

Application IDs: `com.baiwolf.wgpstest` (Google Maps) and `com.baiwolf.wgpstest.osmdroid` (OSM). These install alongside the upstream app. The namespace remains `com.android.gpstest` to preserve the existing implementation.

## Build and verify

The verified build baseline uses Android Gradle Plugin 8.13.2, Gradle 8.14.5, Kotlin 2.1.0, Hilt 2.56.2, and Android SDK 36. Gradle daemon JVM criteria select JetBrains JDK 21 through the Foojay resolver. The Google Debug build was compiled, installed over wireless debugging, and checked on a Pixel 11 (Android API 37), including Overview and Dashboard.

Open the project in Android Studio and select the Google Debug variant. From a configured command line:

```shell
./gradlew :GPSTest:assembleGoogleDebug :GPSTest:testGoogleDebugUnitTest
```

The upstream map-provider setup and any API-key configuration still apply. For a custom Google Maps API key, authorize the W-GPSTest application ID and signing certificate.

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
