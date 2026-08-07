# CloudVault - Application Context

CloudVault is a secure, high-performance Android media gallery and vault application designed for privacy-conscious users. It features an elegant Material 3 UI, robust encryption for "vaulted" content, and optimized performance for large media collections.

---

## 🏗️ Architecture & Design Patterns

The application follows **Clean Architecture** principles combined with the **MVVM (Model-View-ViewModel)** pattern to ensure scalability, testability, and maintainability.

- **UI Layer (Compose)**: Uses Jetpack Compose for a fully declarative UI. ViewModels manage state and interact with Use Cases.
- **Domain Layer**: Contains Business Logic (Use Cases) and Repository Interfaces. This layer is pure Kotlin and has no Android dependencies.
- **Data Layer**: Implements Repository Interfaces. Manages data from the MediaStore, Room database, Preferences (DataStore), and Network sources.

---

## 📦 Project Structure

The project is divided into three main modules:

1.  **`:app`**: The main entry point. Contains the UI, ViewModels, and integration of all components.
2.  **`:cloudvault-sdk`**: A library module intended for core cloud synchronization and encryption logic (currently in development/integration phase).
3.  **`:image-editor-sdk`**: A library module intended for advanced image editing capabilities.

### Key Packages in `:app`
- `com.shamil.cloudvault.ui`: Compose screens and components (`GalleryScreen`, `VaultScreen`, `SettingsScreen`).
- `com.shamil.cloudvault.domain.usecase`: Specific business logic units (`GetGalleryItemsUseCase`, `MoveToBinUseCase`).
- `com.shamil.cloudvault.data.repository`: Implementation of data access logic.
- `com.shamil.cloudvault.data.local`: Room database setup and Entities (`MediaEntity`, `GalleryDatabase`).
- `com.shamil.cloudvault.data.network`: Workers for updates and background sync.

---

## ✨ Key Features

- **Dynamic Gallery**: High-performance scrolling through local media using **Paging 3** and **Coil**.
- **Secure Vault**: (v1.1) A protected space for sensitive media (placeholder in v1.0).
- **Recycle Bin**: Soft-delete functionality with automatic cleanup via **WorkManager**.
- **Advanced Theming**: Supports Material 3 Dynamic Color, Dark/Light modes, and custom theme styles.
- **Media Viewer**: Full-screen viewer with zoom support and video playback via **Media3 ExoPlayer**.
- **Background Sync**: Automated checks for app updates and repository maintenance.

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room (with Paging support)
- **Image Loading**: Coil
- **Video Playback**: Media3 ExoPlayer
- **Background Tasks**: WorkManager
- **Asynchrony**: Kotlin Coroutines & Flow
- **Dependency Injection**: Manual injection (Hilt migration planned for Phase 2)
- **Preferences**: Jetpack DataStore
- **Build System**: Gradle Kotlin DSL with Version Catalogs

---

## 🔄 Data Flow

1.  **MediaStore**: Source of truth for local images and videos.
2.  **GalleryRepository**: Observes MediaStore and syncs metadata into the **Room Database**.
3.  **Use Cases**: Request data from the repository (often as a `Flow<PagingData>`).
4.  **ViewModel**: Collects the Flow and exposes it as a state to the UI.
5.  **Compose UI**: Renders the state and triggers events (user actions).

---

## 🔒 Security & Performance

- **R8/ProGuard**: Enabled in release builds for code obfuscation and resource shrinking.
- **Database Indices**: Optimized for fast querying of large datasets.
- **Memory Management**: Optimized Coil caching and Paging to maintain low memory footprint.
- **Production Logging**: Centralized `Logger` utility that disables logs in release builds.

---

## 🚀 Roadmap

- **Phase 2**: Implement Hilt for DI, full Unit Test coverage, and move to Paging 3 (started).
- **v1.1**: Full implementation of the Encrypted Vault.
- **v1.2**: Integration of the Image Editor SDK.

---

*This document was generated to provide context for AI-assisted development and auditing.*
