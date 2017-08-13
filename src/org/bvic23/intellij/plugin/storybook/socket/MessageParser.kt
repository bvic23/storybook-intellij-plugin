package org.bvic23.intellij.plugin.storybook.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import org.bvic23.intellij.plugin.storybook.models.GeneralMessage
import org.bvic23.intellij.plugin.storybook.notifications.NotificationManager

typealias MessageArgs = Map<String, Any>

class MessageParser(private val notificationManager: NotificationManager) {
    val mapper = ObjectMapper().registerKotlinModule()

    fun parseGeneralMessage(json: String) = try {
        mapper.readValue<GeneralMessage>(json)
    } catch (e: Throwable) {
        notificationManager.error(e.toString())
        null
    }
}