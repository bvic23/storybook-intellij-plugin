package org.bvic23.intellij.plugin.storybook.models

import com.fasterxml.jackson.module.kotlin.readValue
import org.bvic23.intellij.plugin.storybook.JacksonMapper
import org.bvic23.intellij.plugin.storybook.firstLetters
import org.bvic23.intellij.plugin.storybook.normalized
import org.bvic23.intellij.plugin.storybook.similar

data class Story(val kind: String, val story: String) {
    private val normalizedSearchString = (kind + " " + story).normalized
    private val firstLetters = (kind + " " + story).firstLetters
    fun toJSON() = JacksonMapper.mapper.writeValueAsString(this)
    fun toMessage() = StorySelectionMessage("setCurrentStory", listOf(this)).toJSON()
    fun similarTo(target: String) = normalizedSearchString.similar(target, 10) || firstLetters.similar(target, 3)

    companion object {
        fun fromJSON(string: String?) = if (string != null) JacksonMapper.mapper.readValue<Story>(string) else null
    }
}