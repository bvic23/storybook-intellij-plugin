package org.bvic23.intellij.plugin.storybook.settings

import com.intellij.ide.util.PropertiesComponent
fun String.removePrefix(prefix: String) = this.replace(prefix, "")

class SettingsManager {
    private val COLLAPSED_SEPARATOR = "#"

    private val DEFAULT_HOST = "localhost"
    private val DEFAULT_PORT = "7007"

    private val FILTER_KEY = "FILTER_KEY"
    private val COLLAPSED_KEY = "COLLAPSED_KEY"
    private val HOST_KEY = "HOST_KEY"
    private val PORT_KEY = "PORT_KEY"

    var filter
        get() = getValue(FILTER_KEY) ?: ""
        set(value) = setValue(FILTER_KEY, value)

    var collapsed: Set<String>
        get() {
            val collapsedString = getValue(COLLAPSED_KEY) ?: return emptySet<String>()
            val collapsedList = collapsedString.split(COLLAPSED_SEPARATOR)
            val set = mutableSetOf<String>()
            set.addAll(collapsedList)
            return set.toSet()
        }
        set(value) {
            val collapsedString = value.toList().joinToString(COLLAPSED_SEPARATOR)
            setValue(COLLAPSED_KEY, collapsedString)
        }

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