package org.bvic23.intellij.plugin.storybook.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kind(val kind: String, val stories: List<String>)
