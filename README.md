# Clover Multiplatform Inventory & Checkout App

## 📖 In-Depth Project Overview
This application is a cross-platform inventory management, smart promotion, and payment checkout client built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It compiles unified declarative user interfaces from a single Kotlin codebase to target both **Android devices** (POS terminals/tablets) and **Web browsers (compiled to WebAssembly via Kotlin/Wasm)**. 

The application empowers **Clover merchants** to dynamically oversee their store catalogs, utilize AI to discover and recommend smart combo deals on item combinations, and automatically generate high-converting item descriptions to streamline manual input. It communicates directly with the **Clover Point-of-Sale (POS) Sandbox REST API** for inventory state, and integrates with the **Clover.js Ecommerce SDK** to handle web-based payment checkouts.

---

## 💡 Problem It Solves
Operating a retail store or online shop presents severe operational bottlenecks:
1. **Time-Consuming Manual Catalog Entry**: Merchants waste hours manually writing and inputting descriptions for new inventory items. This application integrates an **AI-driven description generator** to resolve this manual pain point instantly.
2. **Stale Inventory & Low Average Order Value (AOV)**: Individual products often sit on shelves, losing value. The app leverages **AI to package items into smart combo deals**, matching slower-moving items with high-velocity ones to clear shelf space.
3. **Customer Acquisition Friction**: Unoptimized item listings and lack of promotional bundles make it hard to bring new customers to the store. Auto-generated descriptions and structured combos make the store's inventory more discoverable and enticing.
4. **Platform Fragmentation**: Developing separate applications for Android registers, store tablets, and websites creates fragmented logic. KMP compiles a single shared UI and service layer across both Web (Wasm) and Android.

---

## 🚀 How It Is Useful
* **AI-Generated Item Descriptions**: Instantly creates SEO-friendly, professional descriptions for new stock items with a single click, eliminating manual data entry.
* **Smart Combo Promotions**: Analyzes items in the Clover catalog and suggests attractive bundle deals to boost sales and attract new customers.
* **Dynamic Inventory Control**: Allows merchants to inspect, add, or delete items from their live Clover merchant sandbox in real time.
* **Wasm Web Checkout Demo**: Enables developers to run a lightweight, high-performance WebAssembly build client-side, pull live items, and validate payment transactions via the Clover checkout SDK.

---

## 🛠️ Technical Architecture & Stack
* **UI Framework**: Compose Multiplatform (shared UI canvas)
* **Networking**: Ktor Client (for fetching/mutating inventory elements)
* **Serialization**: `kotlinx-serialization-json` for parsing Clover REST API payloads
* **Build Tool**: Gradle Kotlin DSL (`build.gradle.kts`)
* **Asynchronous Flow**: Kotlin Coroutines (`Dispatchers.Default` / main thread orchestration)

---

## 📂 Repository Structure
```
├── app/
│   ├── src/
│   │   ├── commonMain/         # Shared Compose UI screens, InventoryService, models, and constants
│   │   ├── androidMain/        # Android host application, launcher setup, and Android PaymentService
│   │   └── wasmJsMain/         # WebAssembly target index.html, JS-interop wrappers for Clover.js SDK
│   └── build.gradle.kts        # Multiplatform configuration (Android and WasmJS compilation targets)
├── build.gradle.kts            # Root project plugin configurations
└── settings.gradle.kts         # Dependency resolution configuration
```

---

## ⚙️ Local Setup and Configuration

### Prerequisites
* JDK 17 or higher
* Android SDK (if compiling for Android)

### Run the WebAssembly App (Web)
To launch the dev server and view the application in your browser:
```bash
./gradlew :app:wasmJsBrowserRun
```
*The dev server will build the code to WASM and start on `http://localhost:8080/`.*

### Run the Android App (Mobile)
Ensure an emulator is active or a device is plugged in, then run:
```bash
./gradlew :app:installDebug
```
