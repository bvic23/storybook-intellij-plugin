package org.bvic23.intellij.plugin.storybook.models

import org.bvic23.intellij.plugin.storybook.JacksonMapper

data class StorySelectionMessage(val type: String, val args: List<Story>) {
    fun toJSON() = JacksonMapper.mapper.writeValueAsString(this)
}