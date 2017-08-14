package org.bvic23.intellij.plugin.storybook

import org.apache.commons.lang.StringUtils

val String.normalized
    get() = this.replace(" ", "").toLowerCase()

val String.firstLetters
    get() = this.split(" ").flatMap { it.toSnakeCase().split("_") }.filter{ it.isNotBlank() }.map { it.first() }.joinToString("")

fun String.similar(needle: String, threshold: Int) = this.normalized.contains(needle.normalized, false) || StringUtils.getLevenshteinDistance(this.normalized, needle.normalized) < threshold

fun String.toSnakeCase(): String {
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