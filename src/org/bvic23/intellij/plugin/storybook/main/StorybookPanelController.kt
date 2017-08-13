package org.bvic23.intellij.plugin.storybook.main

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import org.bvic23.intellij.plugin.storybook.main.Status.WAITING_FOR_CONNECTION
import org.bvic23.intellij.plugin.storybook.models.GeneralMessage
import org.bvic23.intellij.plugin.storybook.models.StoriesArg
import org.bvic23.intellij.plugin.storybook.models.StorySelection
import org.bvic23.intellij.plugin.storybook.models.Tree
import org.bvic23.intellij.plugin.storybook.notifications.NotificationManager
import org.bvic23.intellij.plugin.storybook.notifications.SettingsChangeNotifier
import org.bvic23.intellij.plugin.storybook.settings.SettingsController
import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import org.bvic23.intellij.plugin.storybook.socket.SocketClient
import java.util.*
import javax.websocket.CloseReason
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

enum class Status {
    WAITING_FOR_CONNECTION,
    WAITING_FOR_STORIES,
    READY
}

class StorybookPanelController(project: Project) : SettingsChangeNotifier {
    private val panel = StorybookPanel()
    private var settingsManager = SettingsManager()
    private val socketClient = SocketClient()
    private val notificationManager = NotificationManager()
    private var showFailedMessage = false
    private var status = WAITING_FOR_CONNECTION
    private var dots = ""
    private var dotsTimer: Timer? = null
    private var tree = Tree(emptyList())
    private var selectedStory = StorySelection("", "")
    private val treeController = TreeController(panel.storyTree, settingsManager) { storySelection ->
        setCurrentStory(storySelection)
    }
    private val filterController = FilterController(panel.filterField, settingsManager) {
        updateTree()
    }

    val content
        get() = ContentFactory.SERVICE.getInstance().createContent(panel.contentPane, "", false)

    init {
        setupMessageBus(project)
        setupListeners(project)
        setStatus(WAITING_FOR_CONNECTION)
        connect()
    }

    private fun setupMessageBus(project: Project) {
        SettingsController.messageBus = project.messageBus
        project.messageBus.connect().subscribe(SettingsChangeNotifier.SETTINGS_CHANGE_TOPIC, this)
    }

    override fun onSettingsChange() {
        notificationManager.info("settings has changed, try to reconnect...")
        connect()
    }

    private fun setCurrentStory(story: StorySelection) {
        selectedStory = story
        socketClient.sendText(story.toMessage())
    }

    private fun updateTree() {
        val filterString = filterController.filterString
        treeController.model = if (filterString.isEmpty()) tree
        else tree.filteredTree(filterString)
    }

    private fun setupListeners(project: Project) {
        panel.settingsButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Storybook")
        }

        socketClient.onClose { reason ->
            if (reason.closeCode == CloseReason.CloseCodes.CLOSED_ABNORMALLY) {
                notificationManager.info("lost connection, waiting for restart storybook")
                setStatus(Status.WAITING_FOR_CONNECTION)
                connect()
            } else {
                notificationManager.info("connection got close with reason: $reason")
            }
        }

        socketClient.onError { e ->
            notificationManager.error("error: $e")
        }

        socketClient.on("setStories") { stories ->
            setStatus(Status.READY)
            if (stories[0] is StoriesArg) {
                tree = (stories[0] as StoriesArg).toTree()
                updateTree()
            }
        }

        socketClient.on("getCurrentStory") { _ -> setCurrentStory(selectedStory) }

        socketClient.onOpen {
            notificationManager.info("connected")
            showFailedMessage = false
            setStatus(Status.WAITING_FOR_STORIES)
            dots = ""
            socketClient.sendText(GeneralMessage("getStories", emptyList()).toMessage())
        }
    }

    private fun setStatus(newStatus: Status) {
        status = newStatus
        panel.treePanel.isVisible = status == Status.READY
        panel.statusPanel.isVisible = status != Status.READY
        updateStatusLabels()
        if (status != Status.READY) startTimer()
        else stopTimer()
    }

    private fun startTimer() {
        if (dotsTimer != null) return
        dotsTimer = fixedRateTimer(period = 1000) {
            dots = if (dots.length < 3) dots + "." else ""
            updateStatusLabels()
        }
    }

    private fun stopTimer() {
        if (dotsTimer == null) return
        dotsTimer?.cancel()
        dotsTimer = null
        dots = ""
        updateStatusLabels()
    }

    private fun updateStatusLabels() {
        panel.statusField.text = when (status) {
            Status.WAITING_FOR_CONNECTION -> "Waiting for storybook$dots"
            Status.WAITING_FOR_STORIES -> "Waiting for story list$dots"
            Status.READY -> ""
        }

        panel.subStatusField.text = when (status) {
            Status.WAITING_FOR_CONNECTION -> "Please start storybook from command line!"
            Status.WAITING_FOR_STORIES -> "Please refresh your [simu|emu]lator!"
            Status.READY -> ""
        }
    }

    private fun connect() {
        try {
            socketClient.connect(settingsManager.host, settingsManager.port)
        } catch (e: Throwable) {
            if (!showFailedMessage) {
                showFailedMessage = true
                notificationManager.error("failed to connect, please check your settings or start storybook!")
            }

            Timer().schedule(timerTask {
                print("retry")
                connect()
            }, 1000)
        }
    }

}
