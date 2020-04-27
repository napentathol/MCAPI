package us.sodiumlabs.mcapi.example

import com.github.steveice10.mc.protocol.data.game.ClientRequest
import com.github.steveice10.mc.protocol.data.message.Message
import com.github.steveice10.mc.protocol.data.message.TranslationMessage
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket
import com.github.steveice10.packetlib.Session
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.github.steveice10.packetlib.event.session.SessionAdapter
import com.github.steveice10.packetlib.packet.Packet
import com.google.gson.Gson
import org.apache.logging.log4j.LogManager
import us.sodiumlabs.mcapi.client.Client
import us.sodiumlabs.mcapi.client.Creds
import us.sodiumlabs.mcapi.client.trackers.entity.DeathListener
import us.sodiumlabs.mcapi.client.trackers.entity.EntityTracker
import java.nio.charset.Charset
import java.util.Arrays

fun main() {
    val log = LogManager.getFormatterLogger()

    val credFile = ClassLoader.getSystemClassLoader().getResource("keys.json")
    val creds = Gson().fromJson(credFile!!.readText(Charset.defaultCharset()), Creds::class.java)
    val client = Client(creds)

    val statusFuture = client.status()
    while(!statusFuture.isDone) {
        Thread.sleep(100)
    }
    log.info(String.format("Status: %s", statusFuture.get()))
    client.init()
    client.registerDefaultTrackers()

    client.addListener(object : SessionAdapter() {
        override fun packetReceived(event: PacketReceivedEvent) {
            if (event.getPacket<Packet>() is ServerJoinGamePacket) {
                event.session.send(ClientChatPacket("Hello, this is a test of MCProtocolLib."))
            } else if (event.getPacket<Packet>() is ServerChatPacket) {
                val message = event.getPacket<ServerChatPacket>().message
                println("Received Message: " + message.fullText)
                if (message is TranslationMessage) {
                    println("Received Translation Components: " + Arrays.toString(message.translationParams))
                }
            }
        }

        override fun disconnected(event: DisconnectedEvent) {
            println("Disconnected: " + Message.fromString(event.reason).fullText)
            if (event.cause != null) {
                event.cause.printStackTrace()
            }
        }
    })

    client.queryLink<EntityTracker>("EntityTracker").ifPresent { tracker ->
        tracker.addDeathListener(object: DeathListener {
            override fun onDeath(session: Session, packet: ServerPlayerHealthPacket) {
                log.info("I got here.")
                session.send(ClientRequestPacket(ClientRequest.RESPAWN))
                session.send(ClientChatPacket("I have returned."))
            }
        })
    }
    client.connect()
}
