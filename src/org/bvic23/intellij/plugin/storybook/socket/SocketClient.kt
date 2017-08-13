package org.bvic23.intellij.plugin.storybook.socket

import org.bvic23.intellij.plugin.storybook.models.GeneralMessage
import org.bvic23.intellij.plugin.storybook.notifications.NotificationManager
import org.glassfish.tyrus.client.ClientManager
import java.net.URI
import javax.websocket.*

typealias ErrorHandler = (Throwable) -> Unit
typealias CloseHandler = (CloseReason) -> Unit
typealias OpenHandler = () -> Unit
typealias OnHandler<T> = (List<T>) -> Unit

class SocketClient(notificationManager: NotificationManager) {
    private val endpointConfig = ClientEndpointConfig.Builder.create().build()!!
    private val client = ClientManager.createClient()!!
    private var errorHandler: ErrorHandler = {}
    private var closeHandler: CloseHandler = {}
    private var openHandler: OpenHandler = {}
    private var session: Session? = null
    private val handlers = mutableMapOf<String, OnHandler<Any>>()
    private val messageParser = MessageParser(notificationManager)

    fun connect(host: String, port: String) {
        val uriString = "ws://$host:$port/"
        if (session?.isOpen ?: false) {
            session?.close()

        }
        client.connectToServer(object : Endpoint() {
            override fun onOpen(newSession: Session, config: EndpointConfig) {
                session = newSession

                session?.addMessageHandler(MessageHandler.Whole<String> { json ->
                    val message = messageParser.parseGeneralMessage(json)
                    message?.let {
                        val handler = handlers[message.type]
                        handler?.invoke(message.args)
                    }
                })
                openHandler()
            }

            override fun onClose(session: Session, closeReason: CloseReason) = closeHandler(closeReason)
            override fun onError(session: Session, error: Throwable) = errorHandler(error)
        }, endpointConfig, URI(uriString))
    }

    fun onError(errorHandler: ErrorHandler) {
        this.errorHandler = errorHandler
    }

    fun onClose(closeHandler: CloseHandler) {
        this.closeHandler = closeHandler
    }

    fun onOpen(openHandler: OpenHandler) {
        this.openHandler = openHandler
    }

    fun on(type: String, handler: OnHandler<Any>) = handlers.set(type, handler)

    fun send(msg: String) = session?.basicRemote?.sendText(msg)
    fun send(message: GeneralMessage) = session?.basicRemote?.sendText(message.toMessage())
}