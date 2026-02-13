# BinItRight Mobile App

Android client for BinItRight (scan, questionnaire, nearby bins, check-in, rewards, and profile flows).

## 1. Quick Start (Professor Reproduction)

### Prerequisites
- Android Studio (latest stable)
- JDK 17
- Android SDK installed by Android Studio

### Run in Android Studio
1. Open the folder `BinItRightMobileApp`.
2. Let Gradle sync complete.
3. Select build variant:
   - `localDebug` for local backend testing
   - `stagingDebug` for staging server testing
4. Run the app on an emulator/device.

### Run from terminal
```bash
./gradlew assembleLocalDebug
```

## 2. Testing

### Unit tests
```bash
./gradlew testLocalDebugUnitTest
```

### Coverage report (JaCoCo)
```bash
./gradlew jacocoLocalDebugUnitTestReport
```

Coverage outputs:
- XML: `app/build/reports/jacoco/jacocoLocalDebugUnitTestReport/jacocoLocalDebugUnitTestReport.xml`
- HTML: `app/build/reports/jacoco/jacocoLocalDebugUnitTestReport/html/index.html`

## 3. Project Structure (high level)
- `app/src/main/java/...`
  - Fragments and adapters for UI features
  - Repository/viewmodel logic for scan, questionnaire, history, achievements
  - Network layer (`ApiService`, `RetrofitClient`, interceptors)
- `app/src/main/res/`
  - Layouts, strings, drawables, themes
- `app/src/main/assets/`
  - Local assets (for example tier1 ONNX model and questionnaire config)
- `app/src/test/java/...`
  - JVM/Robolectric unit tests for core business logic

## 4. Notes
- Scan flow supports Tier-1 local model and Tier-2 cloud escalation.
- CI and Sonar use the generated unit-test and JaCoCo reports above.
- If local API endpoints differ, configure flavor URLs via project settings/local properties.