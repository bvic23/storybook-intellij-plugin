package org.bvic23.intellij.plugin.storybook.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.bvic23.intellij.plugin.storybook.models.GeneralMessage
import org.bvic23.intellij.plugin.storybook.models.StoriesArg
import org.bvic23.intellij.plugin.storybook.models.StorySelection
import org.bvic23.intellij.plugin.storybook.notifications.NotificationManager

typealias MessageArgs = Map<String, Any>

class MessageParser(private val notificationManager: NotificationManager) {
    val mapper = ObjectMapper().registerKotlinModule()

    fun parse(json: String) = parseStoriesArg(json) ?: parseStorySelection(json)

    fun parseStoriesArg(json: String) = try {
        mapper.readValue<GeneralMessage<StoriesArg>>(json)
    } catch (e: Throwable) {
        //notificationManager.error(e.toString())
        null
    }

    fun parseStorySelection(json: String) = try {
        mapper.readValue<GeneralMessage<StorySelection>>(json)
    } catch (e: Throwable) {
        //notificationManager.error(e.toString())
        null
    }
}