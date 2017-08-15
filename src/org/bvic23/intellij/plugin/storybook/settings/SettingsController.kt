package org.bvic23.intellij.plugin.storybook.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.util.messages.MessageBus
import org.bvic23.intellij.plugin.storybook.notifications.SettingsChangeNotifier
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class SettingsController : Configurable {
    private val panel = SettingsPanel()

    @Nls
    override fun getDisplayName(): String {
        return "Storybook"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        panel.hostField.text = settingsManager.host
        panel.portField.text = settingsManager.port
        return panel.contentPanel
    }

    override fun isModified(): Boolean {
        return getHost() != settingsManager.host ||
        getPort() != settingsManager.port
    }

    @Throws(ConfigurationException::class)
    override fun apply() {

        val newHost = getHost()
        val newPort = getPort()

        if (newHost != "") settingsManager.host = newHost
        if (newPort != "") settingsManager.port = newPort

        reset()

        notifySettingsChanged()
    }

    override fun reset() {
        panel.hostField.text = settingsManager.host
        panel.portField.text = settingsManager.port
    }

    override fun disposeUIResources() {
        panel.contentPanel.removeAll()
        panel.contentPanel = null
    }

    private fun notifySettingsChanged() {
        val notifier = messageBus.syncPublisher(SettingsChangeNotifier.SETTINGS_CHANGE_TOPIC)
        notifier.onSettingsChange()
    }

    private fun getHost() = panel.hostField.text.trim()
    private fun getPort() = panel.portField.text.trim()

    companion object {
        lateinit var messageBus: MessageBus
        lateinit var settingsManager: SettingsManager
    }
}
