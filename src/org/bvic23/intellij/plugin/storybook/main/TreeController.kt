package org.bvic23.intellij.plugin.storybook.main

import org.bvic23.intellij.plugin.storybook.models.StorySelection
import org.bvic23.intellij.plugin.storybook.models.Tree
import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class TreeController(val tree: JTree, val settingsManager: SettingsManager, val onSelection: (StorySelection) -> Unit) {

    private var collapsedPaths = settingsManager.collapsed

    var model = Tree(emptyList())
        set(value) {
            tree.model = value.toJTreeModel()
            val root = tree.model.root as TreeNode
            expandAll(tree, TreePath(root))
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
            val kind = path.getPathComponent(1).toString()
            val story = path.getPathComponent(2).toString()
            onSelection(StorySelection(kind, story))
        }
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
}