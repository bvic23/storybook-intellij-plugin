package org.bvic23.intellij.plugin.storybook

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import org.bvic23.intellij.plugin.storybook.models.GeneralMessage
import org.bvic23.intellij.plugin.storybook.models.StoriesArg
import org.bvic23.intellij.plugin.storybook.models.StorySelection
import org.bvic23.intellij.plugin.storybook.models.Tree
import org.bvic23.intellij.plugin.storybook.settings.SettingsChangeNotifier
import org.bvic23.intellij.plugin.storybook.settings.SettingsController
import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import org.bvic23.intellij.plugin.storybook.socket.SocketClient
import java.util.*
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.websocket.CloseReason
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask
import org.bvic23.intellij.plugin.storybook.Status.*

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
    private var tree: Tree? = null
    private var selectedStory: StorySelection? = null
    private val collapsedPaths = mutableSetOf<String>()
    private val contentFactory = ContentFactory.SERVICE.getInstance()

    val content = contentFactory.createContent(panel.contentPane, "", false)

    init {
        setupMessageBus(project)
        setupFilter()
        setupTree()
        setupListeners(project)
        setupFilter()
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

    private fun setupFilter() {
        panel.filterField.text = settingsManager.filter
        panel.filterField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun insertUpdate(e: DocumentEvent) = update()
            fun update() = updateTree()
        })
    }

    private fun updateTree() {
        val filterString = panel.filterField.text.trim()

        settingsManager.filter = filterString

        if (filterString?.isEmpty()) updateTree(tree!!)
        else updateTree(tree!!.filteredTree(filterString))
    }


    private fun updateTree(tree: Tree) {
        panel.storyTree.model = tree.toJTreeModel()
        expandAll(panel.storyTree)
    }

    private fun expandAll(tree: JTree) {
        val root = tree.model.root as TreeNode
        expandAll(tree, TreePath(root))
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

    private fun setupTree() {
        collapsedPaths.addAll(settingsManager.collapsed)
        panel.storyTree.showsRootHandles = false
        panel.storyTree.isRootVisible = false
        panel.storyTree.addTreeExpansionListener(object: TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent?) {
                if (event == null) return
                if (event.path.pathCount != 2) return
                val pathName = event.path.lastPathComponent.toString()
                collapsedPaths.remove(pathName)
                updateCollapsedSettings()
            }

            override fun treeCollapsed(event: TreeExpansionEvent?) {
                if (event == null) return
                if (event.path.pathCount != 2) return
                val pathName = event.path.lastPathComponent.toString()
                collapsedPaths.add(pathName)
                updateCollapsedSettings()
            }

        })
        panel.storyTree.selectionModel.addTreeSelectionListener { node ->
            val path = node.path
            if (path.pathCount < 3) return@addTreeSelectionListener
            val selectedStory = StorySelection(path.getPathComponent(1).toString(), path.getPathComponent(2).toString())
            setCurrentStory(selectedStory)
        }
    }

    private fun updateCollapsedSettings() {
        settingsManager.collapsed = collapsedPaths
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
            if (stories[0] is StoriesArg){
                tree = (stories[0] as StoriesArg).toTree()
                updateTree()
            }
        }

        socketClient.on("getCurrentStory") { _ ->
            if (selectedStory != null) setCurrentStory(selectedStory!!)
        }

        socketClient.onOpen {
            notificationManager.info("connected")
            showFailedMessage = false
            setStatus(Status.WAITING_FOR_STORIES)
            dots = ""
            socketClient.sendText(GeneralMessage("getStories", emptyList()).toMessage())
        }
    }


    private fun setCurrentStory(selectedStory: StorySelection) = socketClient.sendText(selectedStory.toMessage())


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
            Status.WAITING_FOR_CONNECTION -> "Waiting for storybook${dots}"
            Status.WAITING_FOR_STORIES -> "Waiting for story list${dots}"
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