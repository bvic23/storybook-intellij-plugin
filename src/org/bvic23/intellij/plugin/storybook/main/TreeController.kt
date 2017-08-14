package org.bvic23.intellij.plugin.storybook.main

import com.intellij.openapi.project.Project
import org.bvic23.intellij.plugin.storybook.locator.FileLocator
import org.bvic23.intellij.plugin.storybook.models.Story
import org.bvic23.intellij.plugin.storybook.models.Tree
import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import java.awt.event.*
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class TreeController(private val tree: JTree, private val settingsManager: SettingsManager, private val project: Project, private val onSelection: (Story) -> Unit) {
    private var collapsedPaths = settingsManager.collapsed
    private var treeModel = DefaultTreeModel(DefaultMutableTreeNode(), false)
    private val fileLocator = FileLocator(project)

    var model = Tree(emptyList())
        set(value) {
            val root = value.toJTreeModel()
            treeModel = DefaultTreeModel(root, false)
            tree.model = treeModel
            expandAll(tree, TreePath(root))
        }

    var selectedStory = settingsManager.story
        set(value) {
            field = value
            settingsManager.story = value
            val path = value.toPath()
            manualSelect(path)
        }

    init {
        tree.showsRootHandles = false
        tree.isRootVisible = false
        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent?) = updateCollapsedSettings(event) { set, path ->
                set.minus(path)
            }

            override fun treeCollapsed(event: TreeExpansionEvent?) = updateCollapsedSettings(event) { set, path ->
                set.plus(path)
            }

        })
        tree.selectionModel.addTreeSelectionListener { node ->
            val path = node.path
            if (path.pathCount < 3) return@addTreeSelectionListener
            val story = storyFromPath(path)
            onSelection(story)
        }
        tree.addKeyListener(object: KeyListener {
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyPressed(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent) {
                val code = e.keyCode
                when (code) {
                    KeyEvent.VK_ENTER -> {
                        openSelectedStoryFile()
                    }
                    KeyEvent.VK_LEFT -> {}
                    KeyEvent.VK_RIGHT -> {}
                }
            }
        })
        tree.addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.clickCount == 1) {
                    return super.mousePressed(e)
                }
                openSelectedStoryFile()
            }
        })
    }

    private fun openSelectedStoryFile() {
        val story = storyFromPath(tree.selectionPath)
        fileLocator.openFileForStory(story)
    }

    private fun storyFromPath(path: TreePath): Story {
        val kind = path.getPathComponent(1).toString()
        val storyName = path.getPathComponent(2).toString()
        val story = Story(kind, storyName)
        return story
    }

    private fun updateCollapsedSettings(event: TreeExpansionEvent?, update: (Set<String>, String) -> Set<String>) {
        if (event == null || event.path.pathCount != 2) return
        val pathName = event.path.lastPathComponent.toString()
        collapsedPaths = update(collapsedPaths, pathName)
        settingsManager.collapsed = collapsedPaths
    }

    private fun expandAll(tree: JTree, parent: TreePath) {
        val node = parent.lastPathComponent as TreeNode
        if (node.childCount >= 0) {
            val e = node.children()
            while (e.hasMoreElements()) {
                val n = e.nextElement() as TreeNode
                val path = parent.pathByAddingChild(n)
                expandAll(tree, path)
            }
        }
        if (!collapsedPaths.contains(parent.lastPathComponent.toString())) {
            tree.expandPath(parent)
        }
    }

    private fun manualSelect(path: Array<String>) {
        var target = tree.model.root as DefaultMutableTreeNode

        for (uo in path.drop(1)) {
            target = getNodeByUserObject(target, uo)?: return
        }

        navigateToNode(target)
    }

    private fun navigateToNode(node: DefaultMutableTreeNode) {
        val nodes = treeModel.getPathToRoot(node)
        val treePath = TreePath(nodes)
        tree.makeVisible(treePath)
        tree.scrollPathToVisible(treePath)
        tree.selectionPath = treePath
    }

    private fun getNodeByUserObject(node: DefaultMutableTreeNode, o: Any) = node.children()
        .asSequence()
        .map { it as DefaultMutableTreeNode }
        .firstOrNull { it.userObject == o }

}

private fun Story.toPath() = if (kind == "" || story == "") emptyArray<String>()
else arrayOf("Root", kind, story)
