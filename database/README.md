# Database Module

The database module provides conversation persistence for the Sophia philosophical discussion application.

## Features

- **Simple JSON-based storage** - Conversations saved as JSON files for easy inspection and debugging
- **Async operations** - All database operations are suspend functions for non-blocking I/O
- **Thread-safe** - Uses mutex for safe concurrent access
- **Comprehensive testing** - Full test coverage with mocked scenarios

## Components

### SimpleConversationStorage

The main storage class that persists conversations to the file system as JSON files.

**Key methods:**
- `saveConversation(conversationState, existingId?)` - Save any conversation state (in-progress, completed, error)
- `loadConversation(id)` - Load a conversation by ID
- `getAllConversations()` - Get all conversations sorted by creation date
- `getConversationsByStatus(status)` - Filter conversations by status ("in_progress", "completed", "error")
- `deleteConversation(id)` - Remove a conversation and its file

### Data Models

**StoredConversation** - Serializable representation with:
- Conversation metadata (topic, participants, settings)
- Status tracking (in_progress, completed, error)
- Timestamps for creation and completion
- All philosopher contributions

**StoredPhilosopher** - Philosopher info without system prompts (security)

**StoredContribution** - Individual philosopher responses with metadata

## Usage

```kotlin
val storage = SimpleConversationStorage("conversations")

// Save an in-progress conversation
val conversationId = storage.saveConversation(conversationState)

// Load it back
val saved = storage.loadConversation(conversationId)

// Get all completed conversations
val completed = storage.getConversationsByStatus("completed")

// Delete when no longer needed
storage.deleteConversation(conversationId)
```

## Storage Format

Conversations are stored as JSON files in the specified directory:
- File naming: `{conversationId}.json`
- Human-readable format with pretty printing
- Contains full conversation history and metadata

## Testing

Run tests with:
```bash
./gradlew :database:test
```

Tests cover:
- Saving and loading different conversation states
- Status filtering and retrieval
- File-based persistence verification
- Deletion and cleanup operations

## Integration

To use in other modules, add to `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":database"))
}
```

## Future Enhancements

The module includes foundation code for SQL-based storage using Exposed and SQLite, but currently focuses on the simpler JSON-based approach for reliability and ease of use.