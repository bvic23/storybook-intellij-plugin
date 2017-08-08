package org.bvic23.intellij.plugin.storybook

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

class NotificationManager {
    fun info(msg: String) = Notifications.Bus.notify(buildNotification(msg, NotificationType.INFORMATION))
    fun error(msg: String) = Notifications.Bus.notify(buildNotification(msg, NotificationType.ERROR))
    private fun buildNotification(msg: String, type: NotificationType) = Notification("Storybook", "Storybook", msg, type)
}