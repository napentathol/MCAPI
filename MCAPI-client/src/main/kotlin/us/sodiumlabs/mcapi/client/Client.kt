package us.sodiumlabs.mcapi.client

import com.github.steveice10.mc.auth.exception.request.RequestException
import com.github.steveice10.mc.protocol.MinecraftProtocol
import com.github.steveice10.mc.protocol.data.SubProtocol
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket
import com.github.steveice10.mc.protocol.packet.status.server.StatusResponsePacket
import com.github.steveice10.packetlib.Client
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.github.steveice10.packetlib.event.session.SessionAdapter
import com.github.steveice10.packetlib.packet.Packet
import com.github.steveice10.packetlib.tcp.TcpSessionFactory
import org.apache.logging.log4j.LogManager
import us.sodiumlabs.mcapi.client.trackers.ErrorTracker
import us.sodiumlabs.mcapi.client.trackers.MiscTracker
import us.sodiumlabs.mcapi.client.trackers.ServerCommsTracker
import us.sodiumlabs.mcapi.client.trackers.WorldTracker
import us.sodiumlabs.mcapi.client.trackers.entity.EntityTracker
import us.sodiumlabs.mcapi.client.utilities.HandlerChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import us.sodiumlabs.mcapi.common.Signing
import java.time.Clock
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class Client(private val creds: Creds) {
    private val log = LogManager.getFormatterLogger()

    private lateinit var client: Client

    private lateinit var future: Future<Any>

    private val handler = HandlerChain()

    private val trackers = linkedMapOf<String, HandlerLink>()

    fun status(): Future<ServerStatusInfo> {
        val protocol = MinecraftProtocol(SubProtocol.STATUS)
        val client = Client(creds.host, creds.port, protocol, TcpSessionFactory())
        val future = CompletableFuture<ServerStatusInfo>()
        client.session.addListener(object: SessionAdapter() {
            override fun packetReceived(event: PacketReceivedEvent) {
                if (event.getPacket<Packet>() is StatusResponsePacket) {
                    val packet = event.getPacket<StatusResponsePacket>()
                    future.complete(packet.info)
                }
            }
        })
        client.session.connect()
        return future
    }

    fun init() {
        val protocol: MCAPIProtocol?
        try {
            protocol = MCAPIProtocol(creds.key)
            protocol.isUseDefaultListeners = false
        } catch (e: RequestException) {
            log.error("Caught request exception", e)
            e.printStackTrace()
            return
        }

        future = CompletableFuture()
        val signing = Signing(Clock.systemUTC())
        client = Client(creds.host, creds.port, protocol, TcpSessionFactory())
        client.session.addListener(MCAPIClientListener(SubProtocol.LOGIN, signing, creds.secret))
        client.session.addListener(object : SessionAdapter() {
            override fun packetReceived(event: PacketReceivedEvent?) {
                if(event!!.getPacket<Packet>() is LoginSuccessPacket) {
                    (future as CompletableFuture).complete(Any())
                }
            }
        })

        client.session.addListener(handler)
    }

    fun registerLink(key: String, tracker: HandlerLink) {
        trackers[key] = tracker
        handler.addLink(tracker)
    }

    fun registerDefaultTrackers() {
        registerLink("EntityTracker", EntityTracker())
        registerLink("WorldTracker", WorldTracker())
        registerLink("ServerCommsTracker", ServerCommsTracker())
        registerLink("MiscTracker", MiscTracker())
        registerLink("ErrorTracker", ErrorTracker())
    }

    fun removeLink(key: String) {
        trackers.remove(key)
        handler.clearAllLinks()
        trackers.values.forEach {
            handler.addLink(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: HandlerLink> queryLink(key: String): Optional<T> {
        return Optional.ofNullable(trackers[key]) as Optional<T>
    }

    fun addListener(sessionAdapter: SessionAdapter) {
        client.session.addListener(sessionAdapter)
    }

    fun removeListener(sessionAdapter: SessionAdapter) {
        client.session.removeListener(sessionAdapter)
    }

    fun connect() {
        client.session.connect()
        while (!future.isDone) {
            Thread.sleep(100)
        }
    }

    fun disconnect() {
        client.session.disconnect("Shutting down.")
    }

    fun send(packet: Packet) {
        client.session.send(packet)
    }
}

data class Creds(val host: String, val port: Int, val key: String, val secret: String)
