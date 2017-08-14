package org.bvic23.intellij.plugin.storybook.main

import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FilterController(private val filterField: JTextField, private val settingsManager: SettingsManager, private val onChange: () -> Unit) {
    val filterString
        get() = filterField.text.trim()

    init {
        filterField.text = settingsManager.filter
        filterField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = updateTree()
            override fun removeUpdate(e: DocumentEvent) = updateTree()
            override fun insertUpdate(e: DocumentEvent) = updateTree()
        })
    }

    private fun updateTree() {
        settingsManager.filter = filterString
        onChange()
    }
}