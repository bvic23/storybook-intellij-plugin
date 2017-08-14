package org.bvic23.intellij.plugin.storybook.socket

import com.fasterxml.jackson.module.kotlin.readValue
import org.bvic23.intellij.plugin.storybook.JacksonMapper
import org.bvic23.intellij.plugin.storybook.models.GeneralMessage
import org.bvic23.intellij.plugin.storybook.models.StoriesArg
import org.bvic23.intellij.plugin.storybook.models.Story
import org.bvic23.intellij.plugin.storybook.notifications.NotificationManager

typealias MessageArgs = Map<String, Any>

class MessageParser(private val notificationManager: NotificationManager) {
    fun parse(json: String) = parseStoriesArg(json) ?: parseStorySelection(json)

    fun parseStoriesArg(json: String) = try {
        JacksonMapper.mapper.readValue<GeneralMessage<StoriesArg>>(json)
    } catch (e: Throwable) {
        //notificationManager.error(e.toString())
        null
    }

    fun parseStorySelection(json: String) = try {
        JacksonMapper.mapper.readValue<GeneralMessage<Story>>(json)
    } catch (e: Throwable) {
        //notificationManager.error(e.toString())
        null
    }
}