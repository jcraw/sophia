# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module Kotlin project named "sophia" using Gradle as the build system. The main application uses Compose Multiplatform for the desktop UI.

## Common Commands

### Building and Testing
- `./gradlew build` - Build the entire project
- `./gradlew :app:build` - Build just the app module
- `./gradlew test` - Run tests using JUnit 5
- `./gradlew clean` - Clean build artifacts

### Running the Application
- `./gradlew :app:run` - Launch the Sophia desktop application

### Development
- `./gradlew compileJava` - Compile main Java sources
- `./gradlew compileTestJava` - Compile test sources
- `./gradlew :app:compileKotlin` - Compile Kotlin sources in app module

## Project Structure

### Key Configuration Files
- `build.gradle.kts` - Main build configuration with Java plugin and JUnit 5 dependencies
- `settings.gradle.kts` - Project settings with app module
- `app/build.gradle.kts` - App module build configuration with Compose Multiplatform
- `gradle/wrapper/` - Gradle wrapper (version 8.14)

### Modules
- **Root module** - Base Java project with JUnit 5
- **app module** - Kotlin + Compose Multiplatform desktop application

### Source Structure
- `src/main/java/` - Root module Java sources (currently empty)
- `app/src/main/kotlin/` - App module Kotlin sources with Compose UI
- `app/src/test/kotlin/` - App module test sources

### Dependencies
- **Root**: JUnit 5 (jupiter) for testing
- **App**: Compose Multiplatform, Material 3, Kotlin 2.0.21

## Important Notes

- Uses Kotlin 2.0.21 with Compose Multiplatform for desktop UI
- JVM target set to version 21 for compatibility
- Main entry point: `com.jcraw.sophia.MainKt` in app module
- Group ID: `com.jcraw`
## Additional Guidelines

Always read and follow the guidelines in CLAUDE_GUIDELINES.md before starting any work.