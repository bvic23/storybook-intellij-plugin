package org.bvic23.intellij.plugin.storybook.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneralMessage(val type: String, val args: List<StoriesArg> = emptyList()) {
    companion object {
        val mapper = ObjectMapper().registerKotlinModule()
    }

    fun toMessage() = mapper.writeValueAsString(this)
}
