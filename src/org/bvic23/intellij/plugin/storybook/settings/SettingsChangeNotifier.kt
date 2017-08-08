package org.bvic23.intellij.plugin.storybook.settings

import com.intellij.util.messages.Topic

interface SettingsChangeNotifier {
    fun onSettingsChange()

    companion object {
        val SETTINGS_CHANGE_TOPIC = Topic.create("Storybook", SettingsChangeNotifier::class.java)!!
    }
}