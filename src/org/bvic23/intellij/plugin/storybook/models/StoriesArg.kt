package org.bvic23.intellij.plugin.storybook.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StoriesArg(val stories: List<Kind>) {
    fun toTree() = Tree(stories)
}