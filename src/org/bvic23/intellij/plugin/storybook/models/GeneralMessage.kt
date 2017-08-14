package org.bvic23.intellij.plugin.storybook.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.bvic23.intellij.plugin.storybook.JacksonMapper

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneralMessage<out T>(val type: String, val args: List<T> = emptyList()) {
    fun toMessage() = JacksonMapper.mapper.writeValueAsString(this)!!
}
