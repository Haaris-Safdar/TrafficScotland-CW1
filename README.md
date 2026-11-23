# Traffic Scotland Coursework Application

A professional, fully responsive Android application developed in **Java and XML** providing real-time live traffic incidents, roadworks overview, interactive Google Maps, distance radius searching, and offline caching across Scotland.

---

## 🌟 Architecture & Technical Specifications

- **Language & Frameworks:** Pure **Java** and **Android XML Layouts** (`minSdk` 26, `targetSdk` 35, `compileSdk` 37).
- **Architecture Pattern:** Model-View-ViewModel (MVVM) with single-activity (`MainActivity`) hosting Android Fragments (`DashboardFragment`, `IncidentsFragment`, `RoadworksFragment`, `DetailsFragment`, `MapFragment`).
- **Reactive Data Layer:** `LiveData` and `MediatorLiveData` maintain app state, propagating search queries and distance filters automatically.
- **Asynchronous Parsing:** `ExecutorService` and `XmlPullParser` asynchronously read and parse DATEX II XML feed datasets (`unplannedContentExample.xml` and `roadworksContentExample.xml`) on background threads.
- **State Retention:** `TrafficViewModel` retains datasets, search text, distance filter radii, and selected situations across device rotation (portrait <-> landscape).

---

## 🔑 External Credentials & Configuration Setup

Credentials and API keys are securely loaded from `local.properties` (which is excluded from version control via `.gitignore`).

### 1. Google Maps API Key Setup
To enable Google Maps tile rendering:
1. Open or create `local.properties` in the project root directory.
2. Add your Google Maps Android API Key:
   ```properties
   MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
   ```
3. Gradle will automatically inject this key into `AndroidManifest.xml` via `manifestPlaceholders["MAPS_API_KEY"]`.
4. *Note:* If no key is provided, the application will display a graceful fallback UI card without crashing.

### 2. Traffic Scotland Live API Credentials Setup
To enable live Traffic Scotland feed fetching:
1. Obtain your Client ID and Client Key from the [Traffic Scotland Developer Portal](https://developer.trafficscotland.org/applications/datex-ii/developer-guide).
2. Add them to `local.properties`:
   ```properties
   TRAFFIC_SCOTLAND_CLIENT_ID=your_client_id_here
   TRAFFIC_SCOTLAND_CLIENT_KEY=your_client_key_here
   ```
3. Gradle will generate these fields in `BuildConfig.TRAFFIC_SCOTLAND_CLIENT_ID` and `BuildConfig.TRAFFIC_SCOTLAND_CLIENT_KEY`.
4. *Note:* Client-side credentials in Android APKs should be secured on backend proxies in production; using `local.properties` ensures keys are not committed to version control.

---

## 📊 Data Source Modes & Offline Caching

The application features a flexible data layer supporting three distinct modes (selectable via the Dashboard dropdown):

1. **`SAMPLE` Mode (Default):** Loads local DATEX II XML files from `app/src/main/assets/` (**4** incidents, **405** roadworks).
2. **`LIVE` Mode:** Connects to official Traffic Scotland DATEX II publications via `HttpURLConnection`:
   - **Unplanned Events:** `https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml`
   - **Current Roadworks:** `https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml`
   - Passes `Client-ID`, `Client-Key`, and `Authorization: Basic` headers.
3. **`CACHED` Mode:** Automatically activates when offline or when live requests fail. Reads the last successfully downloaded XML feeds from `SharedPreferences` with a formatted timestamp.

---

## 🚀 Building & Running

### Build Debug APK
```bash
./gradlew app:assembleDebug
```

### Run Automated Unit Tests
```bash
./gradlew app:testDebugUnitTest
```

---

## 🧪 Automated Test Suite & Coverage

The automated test suite in `app/src/test/java/com/example/cw1trafficapp/` includes **8 passing unit tests**:
- `TrafficXmlParserTest`:
  - `testUnplannedIncidentsParsingCount`: Asserts exactly **4** unplanned incidents parsed from XML.
  - `testRoadworksParsingCount`: Asserts exactly **405** roadworks parsed from XML.
  - `testPrimaryRoadDescriptorExtraction`: Asserts primary road name (`A90`) is prioritized over secondary intersection descriptors (`A933`).
- `TrafficLogicTest`:
  - `testHaversineDistanceCalculation`: Verifies distance from Glasgow (`55.8642, -4.2518`) to Edinburgh (~66 km).
  - `testDateFormattingAndDuration`: Verifies ISO 8601 date parsing & UK formatting (`dd MMM yyyy, HH:mm`) and duration calculation (`10 days`).
  - `testRoadNameExtraction`: Verifies road name regex parsing (e.g. `M8`, `A74(M)`).
  - `testIconCategoryClassification`: Verifies situation mapping into 6 distinct categories.

