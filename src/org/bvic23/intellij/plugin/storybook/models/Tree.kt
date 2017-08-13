package org.bvic23.intellij.plugin.storybook.models

import org.apache.commons.lang.StringUtils
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
            Kind(it.kind, filterStories(it.stories, it.kind, filterString))
        }.filter{
            it.stories.isNotEmpty()
        }
    )

    private fun filterStories(stories: List<String>, kindName: String, filterString: String) = stories.filter { contains(kindName + " " + it , filterString) }
    private fun contains(haystack: String, needle: String) = haystack.similar(needle, 10)|| haystack.firstLetters.similar(needle, 3)

    companion object {
        val empty = Tree(emptyList())
    }

}

private val String.normalized
    get() = this.replace(" ", "").toLowerCase()

private val String.firstLetters
        get() = this.split(" ").flatMap { it.toSnakeCase().split("_") }.filter{ it.isNotBlank() }.map { it.first() }.joinToString("")

private fun String.similar(needle: String, threshold: Int) = this.normalized.contains(needle.normalized, false) || StringUtils.getLevenshteinDistance(this.normalized, needle.normalized) < threshold

private fun String.toSnakeCase(): String {
    var text: String = ""
    var isFirst = true
    this.forEach {
        if (it.isUpperCase()) {
            if (isFirst) isFirst = false
            else text += "_"
            text += it.toLowerCase()
        } else {
            text += it
        }
    }
    return text
}