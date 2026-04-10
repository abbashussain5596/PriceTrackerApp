# PriceTrackerApp

A real-time stock price tracker for Android built with **Jetpack Compose**, **Clean Architecture**, and **WebSockets**.

---

## Features

- Live stock price feed over WebSocket — connect, disconnect, and reconnect at will
- Flash animation on price rows when a value changes (green = up, red = down)
- Per-symbol detail screen showing price, change percentage, and company description
- Connection status indicator in every screen's top bar
- Deep link support: `stocks://symbol/{symbol}` opens any detail screen directly

---

## Architecture

The app follows **Clean Architecture** with three distinct layers, each with its own set of responsibilities and no upward dependencies.

```
app/
└── src/main/java/com/abbas/pricetrackerapp/
    ├── data/                       ← Data layer
    │   ├── datasource/             ← WebSocket source (OkHttp)
    │   ├── mapper/                 ← Raw DTO → domain model
    │   ├── model/                  ← Network DTOs (kotlinx-serialization)
    │   └── repository/             ← PriceRepositoryImpl
    │
    ├── domain/                     ← Domain layer (pure Kotlin, no Android)
    │   ├── model/                  ← StockPrice domain model
    │   ├── repository/             ← PriceRepository interface
    │   └── usecase/                ← One use case per operation (6 total)
    │
    ├── presentation/               ← Presentation layer
    │   ├── navigation/             ← AppNavHost, NavRoutes
    │   ├── state/                  ← Sealed UI-state interfaces + ConnectionState
    │   ├── ui/
    │   │   ├── feed/               ← PriceFeedScreen + PriceFeedContent + components
    │   │   └── details/            ← SymbolDetailsScreen + SymbolDetailsContent
    │   └── viewmodel/              ← PriceFeedViewModel, SymbolDetailsViewModel
    │
    └── di/                         ← Koin module (AppModule)
```

### Data flow

```
WebSocket (OkHttp)
    └─► WebSocketDataSourceImpl  (SharedFlow<WebSocketEvent>)
            └─► PriceRepositoryImpl  (aggregates state, tracks previousPrice)
                    └─► Use Cases  (thin wrappers, one responsibility each)
                            └─► ViewModels  (combine flows → sealed UiState)
                                    └─► Compose screens  (collectAsStateWithLifecycle)
```

---

## Tech Stack

| Area | Library | Version |
|---|---|---|
| Language | Kotlin | 2.2.10 |
| UI | Jetpack Compose (BOM) | 2026.02.01 |
| Architecture | ViewModel + Lifecycle | 2.9.0 |
| Navigation | Navigation Compose | 2.8.9 |
| DI | Koin | 3.5.6 |
| Networking | OkHttp (WebSocket) | 4.12.0 |
| Serialization | kotlinx-serialization-json | 1.7.3 |
| Async | Kotlin Coroutines + Flow | 1.9.0 |
| Build | AGP | 9.1.0 |

---

## Deep Linking

The details screen is reachable from any external source:

```
stocks://symbol/AAPL
stocks://symbol/TSLA
```

**Test from the terminal:**

```bash
# Cold start (app not running)
adb shell am start -a android.intent.action.VIEW \
    -d "stocks://symbol/AAPL" com.abbas.pricetrackerapp

# Warm start (app already in foreground — exercises onNewIntent)
adb shell am start -a android.intent.action.VIEW \
    -d "stocks://symbol/NVDA" com.abbas.pricetrackerapp
```

**Supported symbols:** `AAPL` · `GOOG` · `TSLA` · `AMZN` · `MSFT` · `NVDA` · `META` · `NFLX`

When the deep link arrives with no back stack, pressing Back navigates home to the price feed.

---

## Testing

### Unit tests (JVM — no device needed)

```bash
./gradlew test
```

| Test file | What it covers |
|---|---|
| `WebSocketDataSourceImplTest` | WebSocket connect/disconnect/message parsing (MockWebServer + CountDownLatch) |
| `PriceRepositoryImplTest` | Price aggregation, previous-price tracking, idempotent start (MockK) |
| `GetStockSymbolsUseCaseTest` | Delegation to repository |
| `ObservePriceUpdatesUseCaseTest` | Flow passthrough |
| `ObserveConnectionStateUseCaseTest` | Flow passthrough |
| `ObserveFeedRunningStateUseCaseTest` | Flow passthrough |
| `StartPriceFeedUseCaseTest` | Delegation + suspend correctness |
| `StopPriceFeedUseCaseTest` | Delegation + suspend correctness |
| `PriceFeedViewModelTest` | UiState transitions, togglePriceFeed, error handling (Turbine) |
| `SymbolDetailsViewModelTest` | Symbol filtering, price/connection state, auto-start (Turbine) |

### Compose UI tests (instrumented — requires a device or emulator)

```bash
./gradlew connectedDebugAndroidTest
```

Each screen is split into a **stateful** composable (holds the ViewModel) and a **stateless content** composable that accepts plain state. Tests target the stateless composable directly — no ViewModel, no Koin, no fake repository needed.

| Test file | What it covers |
|---|---|
| `PriceFeedScreenTest` | Title, Start/Stop button, empty state, stock rows, click callbacks, error state (12 cases) |
| `SymbolDetailsScreenTest` | Symbol display, price/arrows/change %, back button, about card, error state (12 cases) |
| `StockRowTest` | Symbol, price, click callback, up/down arrows, no-previous-price case (6 cases) |

**Total: 53 unit tests + 30 instrumented UI tests = 83 tests**

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 11+
- A running WebSocket price-feed server (the app connects to the same server as the reference `PriceTracker` project)

### Build & run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device / emulator
./gradlew installDebug

# Run all unit tests
./gradlew test

# Run all instrumented tests (device/emulator required)
./gradlew connectedDebugAndroidTest
```

### Project structure highlights

- **`WebSocketDataSourceImpl`** — accepts `OkHttpClient` and URL as constructor parameters so `MockWebServer` can inject a test server without any mocking framework.
- **`PriceRepositoryImpl`** — `startPriceFeed()` is **idempotent**: calling it multiple times (e.g. from a deep-link entry) is safe and starts only one WebSocket connection.
- **`AppModule` (Koin)** — data-layer objects are singletons; use cases are factories (stateless); ViewModels use Koin's `viewModel { }` DSL which automatically injects `SavedStateHandle` from the Navigation back stack.
- **Screens** — every screen is composed of a thin stateful wrapper (`PriceFeedScreen`) and a testable stateless content composable (`PriceFeedContent`). Tests call the content composable directly with a pre-built sealed `UiState` value.
