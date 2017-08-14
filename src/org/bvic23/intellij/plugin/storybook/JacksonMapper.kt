package org.bvic23.intellij.plugin.storybook

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JacksonMapper {
    val mapper = ObjectMapper().registerKotlinModule()
}