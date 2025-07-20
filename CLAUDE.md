# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform project using Compose Multiplatform targeting Android, iOS, and Web (WASM). The app appears to be called "Kukbuk" and uses the package name `net.shamansoft.kukbuk`.

## Project Structure

- `/composeApp` - Main multiplatform code module
  - `commonMain` - Shared code across all platforms
  - `androidMain` - Android-specific code and platform implementations
  - `iosMain` - iOS-specific code and platform implementations  
  - `wasmJsMain` - Web/WASM-specific code and platform implementations
  - `commonTest` - Shared test code
- `/iosApp` - iOS application entry point and SwiftUI integration
- `/gradle` - Gradle configuration files including version catalog (`libs.versions.toml`)

## Development Commands

### Building
- `./gradlew build` - Build all modules and targets
- `./gradlew composeApp:build` - Build just the composeApp module
- `./gradlew assemble` - Assemble all outputs

### Running Applications
- `./gradlew composeApp:wasmJsBrowserDevelopmentRun` - Run web application in development mode
- `./gradlew composeApp:installDebug` - Install debug build on connected Android device
- iOS app should be run from Xcode using the `/iosApp` project

### Testing
- `./gradlew test` - Run unit tests for all platforms
- `./gradlew composeApp:iosX64Test` - Run iOS simulator tests (x64)
- `./gradlew composeApp:iosSimulatorArm64Test` - Run iOS simulator tests (ARM64)
- `./gradlew composeApp:wasmJsBrowserTest` - Run web tests in browser
- `./gradlew composeApp:allTests` - Run all tests and create aggregated report

### Code Quality
- `./gradlew composeApp:lint` - Run Android lint
- `./gradlew composeApp:lintFix` - Auto-fix lint issues where possible
- `./gradlew check` - Run all verification tasks

### iOS Development
- Use Xcode to open `/iosApp/iosApp.xcodeproj`
- The shared Kotlin code is compiled into a framework for iOS
- Run `./gradlew composeApp:embedAndSignAppleFrameworkForXcode` to prepare framework for Xcode

## Architecture Notes

### Multiplatform Setup
- Uses Kotlin 2.2.0 and Compose Multiplatform 1.8.2
- Targets Android (API 24+), iOS (x64, ARM64, Simulator ARM64), and Web (WASM)
- Shared UI code in `commonMain` using Compose Multiplatform
- Platform-specific implementations in respective `*Main` source sets

### Key Dependencies
- Compose runtime, foundation, material3, and UI
- androidx.lifecycle (viewmodel and runtime-compose)
- Compose resources for multiplatform asset management

### Resource Management
- Uses Compose Resources for cross-platform asset handling
- Resources are defined in `composeApp/src/commonMain/composeResources/`
- Generated resource accessors are available through `Res` object

### Platform Integration
- Android: Standard Android app with Compose Activity
- iOS: Swift app that integrates Kotlin framework via `MainViewController`
- Web: WASM target with development server support