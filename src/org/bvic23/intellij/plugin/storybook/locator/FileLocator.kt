package org.bvic23.intellij.plugin.storybook.locator

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.bvic23.intellij.plugin.storybook.models.Story
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.vfs.VirtualFile

class FileLocator(private val project: Project) {
    private val storyCache = mutableMapOf<Story, VirtualFile>()
    private val componentCache = mutableMapOf<String, VirtualFile>()

    fun openFileForStory(story: Story) {
        val storyFile = storyCache[story]
        if (storyFile != null) {
            val stillValid = openAndVerify(storyFile, story)
            if (stillValid) return
        }
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
            if (filterFiles(file)) {
                openAndVerify(file, story)
            }
            true
        }
    }

    fun openFileForComponent(componentName: String) {
        val componentFile = componentCache[componentName]
        if (componentFile != null) {
            val stillValid = openAndVerify(componentFile, componentName)
            if (stillValid) return
        }
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
            if (filterFiles(file)) {
                openAndVerify(file, componentName)
            }
            true
        }
    }

    private fun filterFiles(file: VirtualFile) = !file.path.contains("node_modules") && file.extension == "js" && file.isValid

    private fun openAndVerify(file: VirtualFile, story: Story): Boolean {
        val content = LoadTextUtil.loadText(file).toString()

        val kindOffset = content.indexOf("story('${story.kind}")
        if (kindOffset == -1) return false

        val storyOffset = content.substring(kindOffset).indexOf("add('${story.story}")
        if (storyOffset == -1) return false

        storyCache[story] = file
        OpenFileDescriptor(project, file, kindOffset + storyOffset).navigate(true)

        return true
    }

    private fun openAndVerify(file: VirtualFile, componentName: String): Boolean {
        val content = LoadTextUtil.loadText(file).toString()

        val kindOffset = content.indexOf("export default $componentName;")
        if (kindOffset == -1) return false

        componentCache[componentName] = file
        OpenFileDescriptor(project, file, 0).navigate(true)

        return true
    }

}