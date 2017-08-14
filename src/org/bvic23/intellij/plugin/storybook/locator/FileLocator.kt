package org.bvic23.intellij.plugin.storybook.locator

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.bvic23.intellij.plugin.storybook.models.Story
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.vfs.VirtualFile

class FileLocator(private val project: Project) {
    private val cache = mutableMapOf<Story, VirtualFile>()

    fun openFileForStory(story: Story) {
        val cachedEntry = cache[story]
        if (cachedEntry != null) {
            val stillValid = openAndVerify(cachedEntry, story)
            if (stillValid) return
        }
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
            if (!file.path.contains("node_modules") && file.extension == "js" && file.isValid) {
                openAndVerify(file, story)
            }
            true
        }
    }

    private fun openAndVerify(file: VirtualFile, story: Story): Boolean {
        val content = LoadTextUtil.loadText(file).toString()

        val kindOffset = content.indexOf("story('${story.kind}")
        if (kindOffset == -1) return false

        val storyOffset = content.substring(kindOffset).indexOf("add('${story.story}")
        if (storyOffset == -1)  return false

        cache[story] = file
        OpenFileDescriptor(project, file, kindOffset + storyOffset).navigate(true)

        return true
    }
}