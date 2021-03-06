package org.bvic23.intellij.plugin.storybook.main

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import org.bvic23.intellij.plugin.storybook.main.State.WAITING_FOR_CONNECTION
import org.bvic23.intellij.plugin.storybook.notifications.NotificationManager
import org.bvic23.intellij.plugin.storybook.notifications.SettingsChangeNotifier
import org.bvic23.intellij.plugin.storybook.settings.SettingsController
import org.bvic23.intellij.plugin.storybook.settings.SettingsManager
import org.bvic23.intellij.plugin.storybook.socket.SocketClient
import java.util.*
import javax.websocket.CloseReason
import kotlin.concurrent.timerTask
import org.bvic23.intellij.plugin.storybook.main.State.*
import org.bvic23.intellij.plugin.storybook.models.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import sun.jvm.hotspot.HelloWorld.e
import javafx.scene.input.KeyCode.getKeyCode



class StorybookPanelController(project: Project) : SettingsChangeNotifier {
    private val panel = StorybookPanel()
    private val notificationManager = NotificationManager()
    private val getStoriesMessage = GeneralMessage<StoriesArg>("getStories")
    private val settingsManager = SettingsManager(project.name)
    private val socketClient = SocketClient(notificationManager)

    private var showFailedMessage = false
    private var tree = Tree.empty

    private val treeController = TreeController(panel.storyTree, settingsManager, project) { storySelection ->
        setCurrentStory(storySelection)
    }

    private val filterController = FilterController(panel.filterField, settingsManager) {
        updateTree()
    }

    private val stateManager = StateManager(panel.statusField, panel.subStatusField) { state ->
        panel.treePanel.isVisible = state == READY
        panel.statusPanel.isVisible = state != READY
    }

    val content
        get() = ContentFactory.SERVICE.getInstance().createContent(panel.contentPane, "", false)

    init {
        setupSettingsAndMessageBus(project)
        setupListeners(project)
        connect()
    }

    private fun setupListeners(project: Project) {
        panel.settingsButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Storybook")
        }

        panel.filterField.addKeyListener(object: KeyListener{
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyPressed(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent) {
                val code = e.keyCode
                when (code) {
                    KeyEvent.VK_DOWN -> {
                        panel.storyTree.requestFocus()
                    }
                    KeyEvent.VK_LEFT -> {}
                    KeyEvent.VK_RIGHT -> {}
                }
            }
        })

        socketClient.onClose { reason ->
            if (reason.closeCode == CloseReason.CloseCodes.CLOSED_ABNORMALLY) {
                notificationManager.info("lost connection, waiting for restart storybook")
                stateManager.state = WAITING_FOR_CONNECTION
                connect()
            } else {
                notificationManager.info("connection got close with reason: $reason")
            }
        }

        socketClient.onError { e ->
            notificationManager.error("error: $e")
        }

        socketClient.on("setStories") { args ->
            if (args[0] is StoriesArg) {
                tree = (args[0] as StoriesArg).toTree()
                updateTree()
            }
            stateManager.state = READY

            Timer().schedule(timerTask {
                setCurrentStory(treeController.selectedStory, true)
            }, 500)
        }

        socketClient.on("setCurrentStory") { args ->
            if (args[0] is Story) {
                setCurrentStory((args[0] as Story))
            }
            stateManager.state = READY
        }

        socketClient.on("getCurrentStory") { _ -> setCurrentStory(treeController.selectedStory) }

        socketClient.onOpen {
            notificationManager.info("connected")
            showFailedMessage = false
            stateManager.state = WAITING_FOR_STORIES
            socketClient.send(getStoriesMessage)
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
                connect()
            }, 1000)
        }
    }

    private fun setupSettingsAndMessageBus(project: Project) {
        SettingsController.messageBus = project.messageBus
        SettingsController.settingsManager = settingsManager
        project.messageBus.connect().subscribe(SettingsChangeNotifier.SETTINGS_CHANGE_TOPIC, this)
    }

    override fun onSettingsChange() {
        notificationManager.info("settings has changed, try to reconnect...")
        connect()
    }

    private fun setCurrentStory(story: Story, force: Boolean = false) {
        if (treeController.selectedStory == story && !force) return
        treeController.selectedStory = story
        socketClient.send(story.toMessage())
    }

    private fun updateTree() {
        val filterString = filterController.filterString
        treeController.model = if (filterString.isEmpty()) tree
        else tree.filteredTree(filterString)
    }

}
