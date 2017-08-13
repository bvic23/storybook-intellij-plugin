package org.bvic23.intellij.plugin.storybook

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.bvic23.intellij.plugin.storybook.main.StorybookPanelController

class WindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val controller = StorybookPanelController(project)
        toolWindow.contentManager.addContent(controller.content)
    }
}