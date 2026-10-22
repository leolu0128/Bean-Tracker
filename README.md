# Bean Tracker

Android app for tracking how much of each bag of coffee beans is left.
記錄每包咖啡豆剩餘克數的 Android App。

## Features

- Roaster, cover, name, countries, region, farm, variety, process, roast date, remaining grams
- Multiple countries and processes per bag, for blends; roaster plus a name or a country is required
- Quick buttons on every card: `-20` `-15` `undo` `+15` `+20`; grams never go below 0
- Covers belong to the roaster: reusing a roaster name reuses its cover, and changing it updates every bag from that roaster
- Bags at 0 g move to the Finished tab (on the next refresh, so a mistaken tap can still be undone in place)
- Sorted by most recent use, with search and filters by roaster, country and process
- Light/dark theme (follow system or set by hand), English and Traditional Chinese

## Build

Every push to `main` builds a signed release APK in GitHub Actions and attaches it to a GitHub release.
Locally: `./gradlew assembleRelease` (needs the Android SDK).
