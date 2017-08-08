package org.bvic23.intellij.plugin.storybook

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.bvic23.intellij.plugin.storybook.Status.*
import org.bvic23.intellij.plugin.storybook.models.*
import org.bvic23.intellij.plugin.storybook.settings.SettingsChangeNotifier
import org.bvic23.intellij.plugin.storybook.settings.SettingsController
import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import org.bvic23.intellij.plugin.storybook.socket.SocketClient
import java.util.*
import javax.websocket.CloseReason
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.JTree




enum class Status {
    WAITING_FOR_CONNECTION,
    WAITING_FOR_STORIES,
    READY
}

class WindowFactory : ToolWindowFactory, SettingsChangeNotifier {
    private var panel = StorybookPanel()
    private var settingsManager = SettingsManager()
    private val socketClient = SocketClient()
    private val notificationManager = NotificationManager()
    private var showFailedMessage = false
    private var status = WAITING_FOR_CONNECTION
    private var dots = ""
    private var dotsTimer: Timer? = null
    private var tree: Tree? = null
    private var selectedStory: StorySelection? = null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createPanel(toolWindow)
        setupMessageBus(project)
        setupTree()
        setupListeners(project)
        panel.filterField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) =  update()
            override fun insertUpdate(e: DocumentEvent) = update()
            fun update() {
                val filterString = panel.filterField.text.trim()
                if (filterString.isEmpty()) return
                updateTree(tree!!.filteredTree(filterString))
            }
        })
        setStatus(WAITING_FOR_CONNECTION)
        connect()
    }

    private fun setupTree() {
        panel.storyTree.showsRootHandles = false
        panel.storyTree.isRootVisible = false
        panel.storyTree.selectionModel.addTreeSelectionListener { node ->
            val path = node.path
            if (path.pathCount < 3) return@addTreeSelectionListener
            val selectedStory = StorySelection(path.getPathComponent(1).toString(), path.getPathComponent(2).toString())
            setCurrentStory(selectedStory)
        }
    }

    private fun setupListeners(project: Project) {
        panel.settingsButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Storybook")
        }

        socketClient.onClose { reason ->
            if (reason.closeCode == CloseReason.CloseCodes.CLOSED_ABNORMALLY) {
                notificationManager.info("lost connection, waiting for restart storybook")
                setStatus(WAITING_FOR_CONNECTION)
                connect()
            } else {
                notificationManager.info("connection got close with reason: $reason")
            }
        }

        socketClient.onError { e ->
            notificationManager.error("error: $e")
        }

        socketClient.on("setStories") { stories ->
            setStatus(READY)
            if (stories[0] is StoriesArg){
                tree = (stories[0] as StoriesArg).toTree()
                updateTree(tree!!)
            }
        }

        socketClient.on("getCurrentStory") { _ ->
            if (selectedStory != null) setCurrentStory(selectedStory!!)
        }

        socketClient.onOpen {
            notificationManager.info("connected")
            showFailedMessage = false
            setStatus(WAITING_FOR_STORIES)
            dots = ""
            socketClient.sendText(GeneralMessage("getStories", emptyList()).toMessage())
        }
    }

    private fun updateTree(tree: Tree) {
        panel.storyTree.model = tree.toJTreeModel()
        expandAllNodes(panel.storyTree)
    }

    private fun expandAllNodes(tree: JTree) {
        val rowCount = tree.rowCount
        for (i in 0..rowCount - 1) {
            tree.expandRow(i)
        }
    }

    private fun setCurrentStory(selectedStory: StorySelection) = socketClient.sendText(selectedStory.toMessage())

    private fun setupMessageBus(project: Project) {
        SettingsController.messageBus = project.messageBus
        project.messageBus.connect().subscribe(SettingsChangeNotifier.SETTINGS_CHANGE_TOPIC, this)
    }

    private fun setStatus(newStatus: Status) {
        status = newStatus
        panel.treePanel.isVisible = status == READY
        panel.statusPanel.isVisible = status != READY
        updateStatusLabels()
        if (status != READY) startTimer()
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
            WAITING_FOR_CONNECTION -> "Waiting for storybook${dots}"
            WAITING_FOR_STORIES -> "Waiting for story list${dots}"
            READY -> ""
        }

        panel.subStatusField.text = when (status) {
            WAITING_FOR_CONNECTION -> "Please start storybook from command line!"
            WAITING_FOR_STORIES -> "Please refresh your [simu|emu]lator!"
            READY -> ""
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

    private fun createPanel(toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel.contentPane, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun onSettingsChange() {
        notificationManager.info("settings has changed, try to reconnect...")
        connect()
    }

}