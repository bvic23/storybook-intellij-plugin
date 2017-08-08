package org.bvic23.intellij.plugin.storybook.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

data class StorySelectionMessage(val type: String, val args: List<StorySelection>) {
    companion object {
        val mapper = ObjectMapper().registerKotlinModule()
    }

    fun toJSON() = mapper.writeValueAsString(this)
}