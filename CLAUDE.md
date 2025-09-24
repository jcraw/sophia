# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sophia is a philosophical discussion application that allows users to input topics and watch AI-powered philosophers engage in multi-round conversations. Built with Kotlin and Compose Multiplatform for desktop, it integrates with OpenAI's models to simulate conversations between famous historical philosophers.

### Key Features
- **Modern UI** - Split-panel interface with conversation history sidebar and main content area
- **Conversation History** - Persistent storage and browsing of all past philosophical discussions
- **Multi-Philosopher Discussions** - Support for 1-6 philosophers in simultaneous conversations
- **Flexible Configuration** - Adjustable rounds, word limits, and participant selection
- **AI-Powered Summarization** - Convert full conversations into video-optimized summaries for YouTube Shorts
- **Smart Content Management** - Persistent storage for both conversations and summaries with easy access
- **Video Production Ready** - Summaries designed specifically for short-form video content creation

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
- `./gradlew :discussion:compileKotlin` - Compile Kotlin sources in discussion module
- `./gradlew :config:compileKotlin` - Compile Kotlin sources in config module

## Project Structure

### Key Configuration Files
- `build.gradle.kts` - Main build configuration with Java plugin and JUnit 5 dependencies
- `settings.gradle.kts` - Project settings with app, config, discussion, and llm-wrapper modules
- `app/build.gradle.kts` - App module build configuration with Compose Multiplatform
- `config/build.gradle.kts` - Config module build configuration
- `discussion/build.gradle.kts` - Discussion module build configuration
- `gradle/wrapper/` - Gradle wrapper (version 8.14)

### Modules
- **Root module** - Base Java project with JUnit 5
- **app module** - UI layer with Compose components and service integration
- **config module** - Centralized configuration for LLM models and prompts
- **database module** - Conversation persistence using JSON file storage
- **discussion module** - Domain logic for philosophical conversations and philosopher management
- **llm-wrapper module** - Wrapper around external LLM client for OpenAI integration

### Source Structure
- `src/main/java/` - Root module Java sources (currently empty)
- `app/src/main/kotlin/com/jcraw/sophia/` - UI application code
  - `ui/` - Compose UI components with modern split-panel design
    - `MainScreen.kt` - Main application layout with history sidebar and navigation
    - `ConversationHistoryScreen.kt` - History panel with conversation list, summary chips, and management
    - `HistoricConversationView.kt` - Detailed view of past conversations with summarization
    - `SummaryViewScreen.kt` - Dedicated screen for viewing conversation summaries
    - `ConversationSetupScreen.kt` - New conversation configuration
    - `ConversationScreen.kt` - Live conversation display
  - `service/` - Integration layer between UI and domain logic
    - `PhilosopherService.kt` - Enhanced with conversation history management
- `app/src/test/kotlin/com/jcraw/sophia/` - Test code
  - `LLMIntegrationTest.kt` - Integration tests for OpenAI API functionality
- `config/src/main/kotlin/com/jcraw/sophia/config/` - Configuration management
  - `LLMModels.kt` - LLM model definitions and pricing information
  - `PhilosopherPrompts.kt` - All philosopher system prompts, conversation templates, and summarization prompts
  - `LLMBridge.kt` - Bridge between config and llm-wrapper modules
- `database/src/main/kotlin/com/jcraw/sophia/database/` - Conversation persistence
  - `SimpleConversationStorage.kt` - JSON-based storage for conversations and summaries with async operations
  - `entities/ConversationEntities.kt` - Database entity definitions for conversations and summaries
  - `repository/ConversationRepository.kt` - Repository pattern for conversation data access
- `discussion/src/main/kotlin/com/jcraw/sophia/discussion/` - Domain logic
  - `Models.kt` - Domain models (Philosopher, ConversationState, ConversationSummary, etc.)
  - `ConversationEngine.kt` - Core conversation orchestration
  - `SummarizationEngine.kt` - AI-powered conversation summarization for video content
  - `PhilosopherRepository.kt` - Philosopher data and personalities
- `llm-wrapper/src/main/kotlin/` - LLM client implementation (copied from external project)

### Dependencies
- **Root**: JUnit 5 (jupiter) for testing
- **App**: Compose Multiplatform, Material 3, discussion module, database module, Kotlin 2.0.21, Coroutines, Kotlinx DateTime
- **Config**: LLM model definitions, prompts, and configuration settings
- **Database**: Discussion module, Kotlinx Serialization, Kotlinx Coroutines, Kotlinx DateTime for JSON-based storage
- **Discussion**: Config module, LLM wrapper, Kotlinx Coroutines, Kotlinx Serialization
- **LLM Wrapper**: Kotlinx Serialization, Ktor Client for OpenAI API calls

## Setup

**API Key Configuration** (choose one method):

1. **App Resources** (recommended): Add API key to `app/src/main/resources/config.properties`:
   ```
   openai.api.key=your_actual_api_key_here
   ```

2. **Local Properties**: Create `local.properties` in root directory:
   ```
   openai.api.key=your_actual_api_key_here
   ```

3. **Environment Variable**: Set `OPENAI_API_KEY` environment variable.

All methods are gitignored and won't be committed to version control.

## Testing

The project includes comprehensive integration tests that verify the entire LLM pipeline:

### LLM Integration Tests
- **Basic API Test**: Verifies OpenAI API connectivity and response parsing
- **Philosopher Conversation Test**: Tests single philosopher responses
- **Complete UI Workflow Test**: End-to-end test simulating full user experience (3 philosophers, 2 rounds)
- **Prerequisites**: Tests automatically use API key from config or environment
- **Cost**: Tests use minimal tokens (~$0.001 per full workflow test) with `gpt-4.1-nano` model

### Running Tests
```bash
# Run all tests
./gradlew test

# Run only LLM integration tests
./gradlew :app:test --tests "*LLMIntegrationTest*"

# Run complete UI workflow test specifically
./gradlew :app:test --tests "*LLMIntegrationTest*test complete UI workflow*"

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
- **Authentication issues**: Check API key in `config.properties`, `local.properties`, or environment
- **Network timeouts**: API calls have 2-minute timeout configured

## Important Notes

- Uses Kotlin 2.0.21 with Compose Multiplatform for desktop UI
- JVM target set to version 21 for compatibility
- Main entry point: `com.jcraw.sophia.MainKt` in app module
- Uses GPT4_1Nano model for cost-effective philosophical discussions and summarization
- Features 6 predefined philosophers: Socrates, Nietzsche, Kant, Aristotle, Sartre, Confucius
- **Modern Split-Panel UI** with conversation history sidebar and main content area
- **Conversation Persistence** - All discussions and summaries automatically saved as JSON in `conversations/` directory
- **Video Content Creation** - Built-in summarization for YouTube Shorts and social media content
- **Smart Navigation** - Easy access to summaries through conversation history with visual indicators
- Group ID: `com.jcraw`

## Configuration Management

The **config module** centralizes all LLM-related configuration:

### LLM Models
- **LLMModels.kt** - Defines available models with pricing information
- **LLMConfig.defaultPhilosophicalModel** - Currently set to GPT4_1Nano for cost efficiency
- **LLMConfig.summarizationModel** - Also set to GPT4_1Nano for cost-effective summarization
- **LLMBridge** - Converts config models to llm-wrapper models to avoid circular dependencies

### Prompts
- **PhilosopherPrompts** - All philosopher system prompts in one location
- **ConversationPrompts** - Templates for initial and follow-up conversation prompts
- **SummarizationPrompts** - System and user prompts for video-optimized conversation summarization
- Easy to modify prompts for all philosophers and summarization in single files

### Benefits
- **Centralized Control** - Change models or prompts in one place
- **Cost Management** - Easy to switch between models for different use cases
- **Video Production Ready** - Specialized prompts for creating YouTube Shorts content
- **Expandable Framework** - Ready for additional LLM functionality beyond discussions and summarization

## User Interface

### Layout Design
The application features a modern split-panel interface:
- **Window Size**: 1400x800px to accommodate the split layout
- **History Sidebar** (320px): Left panel showing conversation history with status indicators
- **Main Content Area**: Right panel with dynamic content based on current mode

### UI Modes
1. **Setup Mode** - New conversation configuration
   - Philosopher selection with descriptions
   - Topic input and discussion parameters
   - Rounds and word limit configuration

2. **Current Conversation Mode** - Active discussion in progress
   - Real-time conversation display with philosopher contributions
   - Progress indicators and thinking animations
   - Auto-scroll to latest contributions

3. **Historic Conversation Mode** - Past conversation viewing
   - Complete conversation replay with metadata
   - Participant information and discussion summary
   - "Create Summary" button for video content generation
   - "Restart Similar" functionality for easy continuation

4. **Summary View Mode** - Video-optimized conversation summaries
   - Condensed philosophical discussions for YouTube Shorts
   - Video production notes and metadata
   - Navigation between original conversation and summary
   - Streamlined content optimized for short-form video

### History Management
- **Persistent Storage** - All conversations and summaries automatically saved to `conversations/` directory
- **Status Tracking** - Visual indicators for completed, active, and error states
- **Conversation Preview** - Topic, participant count, contribution count, and dates
- **Summary Chips** - Visual indicators showing available summaries with round/word counts
- **Quick Actions** - Delete conversations, create summaries, and start new discussions
- **Smart Navigation** - Direct access to summaries from conversation history
- **Auto-Save** - Conversations and summaries automatically persist when completed

## Architecture

The application follows unidirectional data flow with immutable state:
- **ConversationStateManager** - Manages conversation state transitions including summarization
- **ConversationEngine** - Orchestrates philosopher turns and LLM interactions (uses config module)
- **SummarizationEngine** - AI-powered conversation condensing for video content
- **PhilosopherRepository** - Provides philosopher data (uses config prompts)
- **PhilosopherService** - Integration layer with history management and summarization support
- **SimpleConversationStorage** - JSON-based persistence for conversations and summaries
- UI observes state changes through StateFlow and sends events to service

### Video Content Creation Features
The architecture supports complete video production workflow:
- **AI-Powered Summarization** - Automatic conversion of full conversations to short-form content
- **Video Production Metadata** - Notes on what makes conversations compelling for video
- **Flexible Content Management** - Easy access to both original discussions and video summaries
- **Production Ready Output** - Summaries designed specifically for YouTube Shorts and social media

## Conversation Summarization

Sophia includes a comprehensive AI-powered summarization system designed specifically for creating YouTube Shorts and social media content from philosophical discussions.

### How Summarization Works

1. **Source Content**: Any completed conversation can be summarized
2. **AI Processing**: Uses GPT4_1Nano with specialized prompts to identify key moments
3. **Video Optimization**: Focuses on quotable moments, dramatic exchanges, and philosophical insights
4. **Content Condensing**: Reduces discussions to 3 rounds with ~50 words per response
5. **Metadata Generation**: Includes video production notes and condensed topics

### Summarization Configuration

- **Model**: Uses `LLMConfig.summarizationModel` (GPT4_1Nano for cost efficiency)
- **Temperature**: 0.3 for precise yet creative summarization
- **Default Output**: 3 rounds, 50 words per philosopher response
- **Customizable**: `SummarizationConfig` allows adjustment of rounds and word limits

### Storage Structure

Summaries are stored separately from conversations:
- **Location**: `conversations/summaries/` directory
- **Format**: JSON files with structured metadata
- **Naming**: `summary_{timestamp}_{random}.json`
- **Content**: Includes original topic, condensed topic, participants, rounds, video notes

### User Workflow

1. **Create**: Click "Create Summary" on any completed conversation
2. **Process**: AI analyzes full conversation and generates condensed version
3. **View**: Automatically navigates to beautiful summary display
4. **Access**: Summaries appear as chips in conversation history sidebar
5. **Manage**: Easy navigation between original conversations and summaries

### Video Production Features

- **Condensed Topics**: Shorter, punchier versions of original topics
- **Production Notes**: AI-generated insights on what makes content video-ready
- **Word Counts**: Precise tracking for video timing and pacing
- **Round Structure**: Organized for easy video editing and flow
- **Participant Metadata**: Ready for video credits and descriptions

## Additional Guidelines

Always read and follow the guidelines in CLAUDE_GUIDELINES.md before starting any work.