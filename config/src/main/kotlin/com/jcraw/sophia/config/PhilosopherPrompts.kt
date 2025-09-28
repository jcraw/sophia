package com.jcraw.sophia.config

object PhilosopherPrompts {
    // Delegate to the dedicated system prompts file
    fun getPrompt(philosopherId: String): String = PhilosopherSystemPrompts.getPrompt(philosopherId)
    val allPhilosopherIds = PhilosopherSystemPrompts.allPhilosopherIds
}

object ConversationPrompts {

    fun buildInitialPrompt(philosopherName: String, topic: String): String = buildString {
        appendLine("Topic for discussion: \"$topic\"")
        appendLine()
        appendLine("This is the start of a philosophical discussion on the topic: \"$topic\"")
        appendLine("Please provide your initial thoughts on this topic as $philosopherName. ")
        appendLine("Share your philosophical perspective and approach to this question. ")
        appendLine("Keep your response concise but substantive (around 100-150 words).")
    }

    fun buildFollowUpPrompt(
        philosopherName: String,
        topic: String,
        previousContributions: String
    ): String = buildString {
        appendLine("Topic for discussion: \"$topic\"")
        appendLine()
        appendLine("Previous contributions to this philosophical discussion on \"$topic\":")
        appendLine()
        appendLine(previousContributions)
        appendLine("Please respond to the discussion as $philosopherName. ")
        appendLine("Build upon or challenge the previous points made, staying true to your philosophical perspective. ")
        appendLine("Keep your response concise but substantive (around 100-150 words).")
    }
}

object SummarizationPrompts {

    val systemPrompt = """You are a philosophical conversation summarizer. Your task is to analyze a full philosophical discussion and create a condensed version suitable for short-form video content (like YouTube Shorts).

Your goals:
- Identify the most impactful and insightful moments from each philosopher
- Extract the strongest arguments and most memorable quotes
- Maintain the philosophical depth while making it accessible
- Create punchy, engaging exchanges that work well in video format
- Preserve each philosopher's distinctive voice and perspective

Focus on:
- Moments of profound insight or wisdom
- Sharp disagreements or contrasts between philosophies
- Quotable lines that capture each philosopher's essence
- Clear, concise arguments that don't need extensive context
- Exchanges that build dramatic tension or resolution"""

    fun buildSummarizationPrompt(
        originalTopic: String,
        participants: List<String>,
        originalConversation: String,
        targetRounds: Int = 3,
        maxWordsPerResponse: Int = 50
    ): String = buildString {
        appendLine("ORIGINAL CONVERSATION TO SUMMARIZE:")
        appendLine("Topic: \"$originalTopic\"")
        appendLine("Participants: ${participants.joinToString(", ")}")
        appendLine()
        appendLine("Full conversation:")
        appendLine(originalConversation)
        appendLine()
        appendLine("SUMMARIZATION TASK:")
        appendLine("Create a condensed version of this philosophical discussion optimized for short-form video content.")
        appendLine("Requirements:")
        appendLine("- Exactly $targetRounds rounds of discussion")
        appendLine("- Maximum $maxWordsPerResponse words per philosopher response")
        appendLine("- Include ALL original participants: ${participants.joinToString(", ")}")
        appendLine("- Each philosopher should speak once per round in the same order as the original")
        appendLine("- Focus on the most impactful ideas and memorable moments")
        appendLine("- Maintain each philosopher's authentic voice and key arguments")
        appendLine("- Make each exchange punchy and suitable for video")
        appendLine()
        appendLine("Format your response as a JSON object with this structure:")
        appendLine("""
{
  "summary": {
    "originalTopic": "$originalTopic",
    "condensedTopic": "A shorter, punchier version of the topic",
    "participants": [${participants.joinToString { "\"$it\"" }}],
    "rounds": [
      {
        "roundNumber": 1,
        "contributions": [
          {
            "philosopherName": "${participants.first()}",
            "response": "Condensed impactful response here",
            "wordCount": 45
          }
        ]
      }
    ],
    "videoNotes": "Brief notes on what makes this conversation compelling for video"
  }
}""".trimIndent())
        appendLine()
        appendLine("Ensure the JSON is valid and complete.")
    }
}

object DirectorPrompts {

    val systemPrompt = """You are a creative director specializing in philosophical YouTube Shorts. Your role is to transform philosophical discussions into compelling video scripts with visual scenes.

Your expertise:
- Creating engaging visual narratives for short-form video content
- Designing atmospheric scenes that enhance philosophical dialogue
- Crafting image prompts that create compelling backdrops without distracting from dialogue
- Understanding how visual storytelling supports philosophical concepts
- Optimizing content for social media engagement while preserving intellectual depth

Your approach:
- Each dialogue gets its own scene with a thoughtfully designed visual backdrop
- Images should be atmospheric and thematically relevant, not literal
- Visual metaphors that reinforce philosophical concepts
- Professional cinematic quality with warm, engaging lighting
- Scenes that work well with text overlays and spoken narration
- Focus on creating mood and atmosphere that enhances the philosophical content

Visual style guidelines:
- Cinematic composition with depth and visual interest
- Warm, inviting lighting that draws viewers in
- Rich colors and textures that feel premium
- Avoid busy backgrounds that compete with text overlays
- Create visual rhythm and flow between scenes
- Use environmental storytelling to reinforce themes"""

    fun buildScriptCreationPrompt(
        summary: String,
        estimatedDuration: String = "60 seconds"
    ): String = buildString {
        appendLine("PHILOSOPHICAL DISCUSSION SUMMARY:")
        appendLine(summary)
        appendLine()
        appendLine("SCRIPT CREATION TASK:")
        appendLine("Transform this philosophical discussion into a compelling YouTube Shorts video script.")
        appendLine("Target duration: $estimatedDuration")
        appendLine()
        appendLine("Requirements:")
        appendLine("- Create an opening scene that sets the philosophical tone")
        appendLine("- Design a unique visual scene for each philosopher's dialogue")
        appendLine("- Add brief transition scenes between major philosophical shifts")
        appendLine("- Include a closing scene that provides resolution or reflection")
        appendLine("- Each scene needs a detailed image prompt for AI image generation")
        appendLine("- Image prompts should create atmospheric backdrops that enhance but don't distract from the dialogue")
        appendLine("- Ensure visual continuity and thematic coherence throughout")
        appendLine("- Include production notes for video editing and pacing")
        appendLine()
        appendLine("Format your response as a JSON object with this structure:")
        appendLine("""
{
  "videoScript": {
    "title": "Engaging title for the video",
    "description": "Brief description of the philosophical discussion",
    "estimatedDuration": "$estimatedDuration",
    "scenes": [
      {
        "sceneNumber": 1,
        "type": "OPENING",
        "duration": "5 seconds",
        "imagePrompt": "Cinematic wide shot of ancient Greek columns bathed in golden hour light, creating a sense of timeless wisdom and philosophical depth. Soft shadows and warm lighting. Professional photography style.",
        "dialogue": null,
        "philosopherName": null,
        "directorNotes": "Sets the philosophical tone, allows for title overlay"
      },
      {
        "sceneNumber": 2,
        "type": "DIALOGUE",
        "duration": "15 seconds",
        "imagePrompt": "Atmospheric scene tailored to the philosopher's era and style",
        "dialogue": "The actual philosophical quote/dialogue",
        "philosopherName": "Philosopher Name",
        "directorNotes": "Notes about pacing, emphasis, visual elements"
      }
    ],
    "productionNotes": [
      "Key insights about editing rhythm and pacing",
      "Suggestions for text overlay timing",
      "Notes about music or sound design"
    ]
  }
}""".trimIndent())
        appendLine()
        appendLine("Key considerations for image prompts:")
        appendLine("- Use cinematic language (\"wide shot\", \"close-up\", \"golden hour lighting\")")
        appendLine("- Specify professional photography or film style")
        appendLine("- Include lighting details (warm, soft, dramatic, etc.)")
        appendLine("- Mention composition elements that create visual depth")
        appendLine("- Ensure each prompt creates a distinct but cohesive visual")
        appendLine("- Consider how text overlays will appear on the image")
        appendLine("- Match the visual mood to the philosophical content")
        appendLine()
        appendLine("Ensure the JSON is valid and complete.")
    }
}