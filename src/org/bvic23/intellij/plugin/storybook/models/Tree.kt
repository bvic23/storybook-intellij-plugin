package org.bvic23.intellij.plugin.storybook.models

import org.apache.commons.lang.StringUtils
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

data class Story(val kind: String, val name: String) {
    private val normalizedSearchString = (kind + " " + name).normalized
    private val firstLetters = (kind + " " + name).firstLetters
    fun similarTo(target: String) = normalizedSearchString.similar(target, 10) || firstLetters.similar(target, 3)
}

data class Tree(val nodes: List<Story>) {

    fun toJTreeModel(): TreeModel {
        val root = DefaultMutableTreeNode("Root")
        val groups = nodes.groupBy { it.kind }
        groups.forEach { key, stories ->
            val node = DefaultMutableTreeNode(key)
            stories.forEach { node.add(DefaultMutableTreeNode(it.name)) }
            root.add(node)
        }
        return DefaultTreeModel(root)
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