package org.bvic23.intellij.plugin.storybook.models

import org.bvic23.intellij.plugin.storybook.normalized
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

data class Tree(val nodes: List<Story>) {
    fun toJTreeModel(): TreeNode {
        val root = DefaultMutableTreeNode("Root")
        val groups = nodes.groupBy { it.kind }
        groups.forEach { key, stories ->
            val node = DefaultMutableTreeNode(key)
            stories.forEach { node.add(DefaultMutableTreeNode(it.story)) }
            root.add(node)
        }
        return root
    }

    fun filteredTree(filterString: String): Tree {
        val fs = filterString.normalized
        val matchingNodes = nodes.filter { it.similarTo(fs) }
        return Tree(matchingNodes)
    }

    companion object {
        val empty = Tree(emptyList())

        fun fromKinds(kinds: List<Kind>): Tree {
            val nodes = kinds.flatMap { (kind, stories) -> stories.map {story -> Story(kind, story) }}
            return Tree(nodes)
        }
    }
}
