# Clover Multiplatform Inventory & Checkout App

## 📖 In-Depth Project Overview
This application is a cross-platform repository inventory manager and payment checkout client built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It compiles unified declarative user interfaces from a single Kotlin codebase to target both **Android devices** and **Web browsers (compiled to WebAssembly via Kotlin/Wasm)**. 

The application integrates directly with the **Clover Point-of-Sale (POS) Sandbox REST API** for managing inventory items, and communicates with the **Clover.js Ecommerce SDK** to initiate credit card checkouts in Web environments, while referencing stubbing services for the Clover Android SDK on mobile.

---

## 💡 Problem It Solves
Developing separate, native applications for distinct retail and checkout environments (like POS registers, mobile tablets, and desktop websites) is highly inefficient, leading to duplicated application logic, UI design fragmentation, and desynchronized inventory state. Additionally, integrating with proprietary retail networks (like Clover's payment hardware and REST backends) usually requires writing platform-specific integration layers.

This project solves these challenges by:
1. **Sharing 100% of UI and Business Logic**: Building the inventory dashboard and state-machine once in Kotlin, sharing it across Web and Android targets.
2. **Abstracting the Integration Layer**: Standardizing inventory calls to Clover's Sandbox API via Ktor HTTP Client.
3. **Targeting WebAssembly (Wasm)**: Compiling the shared Canvas-rendered Compose UI to run client-side on the Web at near-native performance, while maintaining seamless JavaScript interoperability to call Clover's Checkout SDK scripts.

---

## 🚀 How It Is Useful
* **POS Register & Dashboard Simulation**: Merchants can run this client on an Android device to inspect, add, or delete inventory items from their merchant store, and simulate a checkout experience.
* **Wasm Web Checkout Demo**: Developers can run the compiled WebAssembly build in their browser, load live sandbox items, and trigger mock payment checkouts via the sandbox payment gateway.
* **Testing & Sandbox Validation**: Allows POS operators to test how inventory changes and item modifications sync with the Clover cloud database in real time.

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
