package org.bvic23.intellij.plugin.storybook.models

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

data class Tree(val kinds: List<Kind>) {
    fun toJTreeModel(): TreeModel {
        val root = DefaultMutableTreeNode("Root")
        kinds.map{ mapKind(it) }.forEach { root.add(it) }
        return DefaultTreeModel(root)
    }

    private fun mapKind(kind: Kind): DefaultMutableTreeNode {
        val result = DefaultMutableTreeNode(kind.kind)
        kind.stories.map { DefaultMutableTreeNode(it) }.forEach { result.add(it) }
        return result
    }

    fun filteredTree(filterString: String) = Tree(
        this.kinds.map {
            Kind(it.kind, filterStories(it.stories, it.kind, removeSpaces(filterString)))
        }.filter{
            it.stories.isNotEmpty()
        }
    )

    private fun removeSpaces(filterString: String) = filterString.replace(" ", "")

    private fun filterStories(stories: List<String>, kindName: String, filterString: String) = stories.filter { contains(removeSpaces(kindName + it), filterString) }

    private fun contains(haystack: String, filterString: String): Boolean {

        return haystack.contains(filterString, true)
    }

    companion object {
        val empty = Tree(emptyList())
    }

}