package org.bvic23.intellij.plugin.storybook.settings

import com.intellij.ide.util.PropertiesComponent
fun String.removePrefix(prefix: String) = this.replace(prefix, "")

class SettingsManager {
    private val DEFAULT_HOST = "localhost"
    private val DEFAULT_PORT = "7007"
    private val HOST_KEY = "host_key"
    private val PORT_KEY = "port_key"

    var host
        get() = getValue(HOST_KEY) ?: DEFAULT_HOST
        set(value) = setValue(HOST_KEY, trimProtocol(value))

    var port
        get() = getValue(PORT_KEY) ?: DEFAULT_PORT
        set(value) = setValue(PORT_KEY, value)

    private fun trimProtocol(host: String) = host.removePrefix("http://").removePrefix("https://")
    private fun getValue(key: String) = PropertiesComponent.getInstance().getValue(key)
    private fun setValue(key: String, value: String) = PropertiesComponent.getInstance().setValue(key, value)
}