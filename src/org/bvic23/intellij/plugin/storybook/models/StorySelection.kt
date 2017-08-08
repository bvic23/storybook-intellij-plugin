package org.bvic23.intellij.plugin.storybook.models

data class StorySelection(val kind: String, val story: String) {
    fun toMessage() = StorySelectionMessage("setCurrentStory", listOf(this)).toJSON()
}