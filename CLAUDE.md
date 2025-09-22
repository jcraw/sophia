# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sophia is a philosophical discussion application that allows users to input topics and watch AI-powered philosophers engage in multi-round conversations. Built with Kotlin and Compose Multiplatform for desktop, it integrates with OpenAI's models to simulate conversations between famous historical philosophers.

## Common Commands

### Building and Testing
- `./gradlew build` - Build the entire project
- `./gradlew :app:build` - Build just the app module
- `./gradlew test` - Run all tests including LLM integration tests
- `./gradlew :app:test --tests "*LLMIntegrationTest*"` - Run LLM integration tests specifically
- `./gradlew clean` - Clean build artifacts

### Running the Application
- `./gradlew :app:run` - Launch the Sophia desktop application
- Requires OpenAI API key in `local.properties` file (see Setup section below)

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
- **app module** - Main application with philosopher conversation system and Compose UI
- **llm-wrapper module** - Wrapper around external LLM client for OpenAI integration

### Source Structure
- `src/main/java/` - Root module Java sources (currently empty)
- `app/src/main/kotlin/com/jcraw/sophia/` - Main application code
  - `philosophers/` - Domain models, conversation engine, and philosopher repository
  - `ui/` - Compose UI components for setup and conversation screens
  - `service/` - Integration layer between UI and core logic
- `app/src/test/kotlin/com/jcraw/sophia/` - Test code
  - `LLMIntegrationTest.kt` - Integration tests for OpenAI API functionality
- `llm-wrapper/src/main/kotlin/` - LLM client implementation (copied from external project)

### Dependencies
- **Root**: JUnit 5 (jupiter) for testing
- **App**: Compose Multiplatform, Material 3, Kotlin 2.0.21, Coroutines
- **LLM Wrapper**: Kotlinx Serialization, Ktor Client for OpenAI API calls

## Setup

1. **API Key Configuration**: Create a `local.properties` file in the root directory:
   ```
   openai.api.key=your_actual_api_key_here
   ```
   This file is gitignored and won't be committed to version control.

2. **Alternative**: Set the `OPENAI_API_KEY` environment variable.

## Testing

The project includes comprehensive integration tests that verify the entire LLM pipeline:

### LLM Integration Tests
- **Basic API Test**: Verifies OpenAI API connectivity and response parsing
- **Philosopher Conversation Test**: Tests the full philosopher conversation flow
- **Prerequisites**: Tests automatically use API key from `local.properties` or environment
- **Cost**: Tests use minimal tokens (~$0.0000 per run) with `gpt-4.1-nano` model

### Running Tests
```bash
# Run all tests
./gradlew test

# Run only LLM integration tests
./gradlew :app:test --tests "*LLMIntegrationTest*"

# View test results
open app/build/reports/tests/test/index.html
```

## Debugging

The application includes comprehensive logging for troubleshooting:

### Logging Features
- **🔑 API Key Validation**: Shows partial key and length for verification
- **🎭 Service Level**: Logs philosopher selection and conversation setup
- **🚀 Engine Level**: Tracks conversation flow and state transitions
- **💭 Per-Philosopher**: Detailed logging for each response generation
- **🌐 API Level**: Full OpenAI API request/response details including:
  - Model and parameters used
  - Token usage and costs
  - HTTP status codes
  - Error messages with stack traces

### Common Issues
- **"Missing fields" error**: Usually indicates API error response instead of success
- **Authentication issues**: Check API key in `local.properties`
- **Network timeouts**: API calls have 2-minute timeout configured

## Important Notes

- Uses Kotlin 2.0.21 with Compose Multiplatform for desktop UI
- JVM target set to version 21 for compatibility
- Main entry point: `com.jcraw.sophia.MainKt` in app module
- Uses GPT4_1Nano model for cost-effective philosophical discussions
- Features 6 predefined philosophers: Socrates, Nietzsche, Kant, Aristotle, Sartre, Confucius
- Group ID: `com.jcraw`

## Architecture

The application follows unidirectional data flow with immutable state:
- **ConversationStateManager** - Manages conversation state transitions
- **ConversationEngine** - Orchestrates philosopher turns and LLM interactions
- **PhilosopherRepository** - Provides philosopher personalities and system prompts
- **PhilosopherService** - Integration layer between UI and core logic
- UI observes state changes through StateFlow and sends events to service
## Additional Guidelines

Always read and follow the guidelines in CLAUDE_GUIDELINES.md before starting any work.