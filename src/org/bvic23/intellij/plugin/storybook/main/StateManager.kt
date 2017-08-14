package org.bvic23.intellij.plugin.storybook.main

import java.util.*
import javax.swing.JLabel
import kotlin.concurrent.fixedRateTimer
import org.bvic23.intellij.plugin.storybook.main.State.*

enum class State {
    UNDEFINED,
    WAITING_FOR_CONNECTION,
    WAITING_FOR_STORIES,
    READY
}

class StateManager(private val statusField: JLabel, private val subStatusField: JLabel, private val onChange: (State) -> Unit) {
    var state = UNDEFINED
        set(value) {
            if (value == state) return
            field = value
            updateLabels()
            if (state != READY) startTimer()
            else stopTimer()
            onChange(value)
        }
    private var dots = ""
    private var dotsTimer: Timer? = null

    init {
        state = WAITING_FOR_CONNECTION
    }

    private fun startTimer() {
        if (dotsTimer != null) return
        dotsTimer = fixedRateTimer(period = 1000) {
            dots = if (dots.length < 3) dots + "." else ""
            updateLabels()
        }
    }

    private fun stopTimer() {
        if (dotsTimer == null) return
        dotsTimer?.cancel()
        dotsTimer = null
        dots = ""
        updateLabels()
    }

    private fun updateLabels() {
        statusField.text = when (state) {
            WAITING_FOR_CONNECTION -> "Waiting for storybook$dots"
            WAITING_FOR_STORIES -> "Waiting for story list$dots"
            else -> ""
        }

        subStatusField.text = when (state) {
            WAITING_FOR_CONNECTION -> "Please start storybook from command line!"
            WAITING_FOR_STORIES -> "Please refresh your [simu|emu]lator!"
            else -> ""
        }
    }
}