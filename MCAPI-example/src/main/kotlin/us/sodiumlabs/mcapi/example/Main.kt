package us.sodiumlabs.mcapi.example

import com.github.steveice10.mc.protocol.data.game.ClientRequest
import com.github.steveice10.mc.protocol.data.message.Message
import com.github.steveice10.mc.protocol.data.message.TranslationMessage
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket
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
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference

fun main() {
    val log = LogManager.getFormatterLogger()

    val credFile = ClassLoader.getSystemClassLoader().getResource("keys.json")
    val creds = Gson().fromJson(credFile!!.readText(Charset.defaultCharset()), Creds::class.java)
    val client = Client(creds)

    val followMe = AtomicReference<Optional<String>>(Optional.empty())

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
                    if (message.fullText == "commands.message.display.incoming"
                            && message.translationParams.size >= 2) {
                        if (message.translationParams[1].text.toLowerCase() == "follow me") {
                            val username = message.translationParams[0].text
                            event.session.send(ClientChatPacket("/w $username OK"))
                            followMe.set(Optional.of(username))

                            client.queryLink<EntityTracker>("EntityTracker").flatMap { tracker ->
                                tracker.getPlayerEntityByUsername(username)
                            }.ifPresent {
                                event.session.send(ClientChatPacket("/w $username following ${it.entityId}"))
                            }
                        } else if (message.translationParams[1].text.toLowerCase() == "stop") {
                            val username = message.translationParams[0].text
                            event.session.send(ClientChatPacket("/w $username OK"))
                            followMe.set(Optional.empty())
                        }
                    }
                }
            } else if (event.getPacket<Packet>() is ServerPlayerPositionRotationPacket) {
                val message = event.getPacket<ServerPlayerPositionRotationPacket>()
                event.session.send(ClientTeleportConfirmPacket(message.teleportId))
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
                session.send(ClientRequestPacket(ClientRequest.RESPAWN))
                session.send(ClientChatPacket("I have returned."))
            }
        })
    }

    client.connect()
    val entityTracker = client.queryLink<EntityTracker>("EntityTracker")
            .orElseThrow { RuntimeException("No entity tracker") }

    while(true) {
        val selfEntity = entityTracker.getOwnEntity()
        selfEntity.ifPresent { self ->
            followMe.get().flatMap { username ->
                entityTracker.getPlayerEntityByUsername(username)
            }.ifPresent { entity ->
                val diffX = entity.x - self.x
                val diffY = entity.y - self.y
                val diffZ = entity.z - self.z

                val total = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ)

                if (total > 1) {
                    val movX = diffX * 0.5 / total
                    val movY = diffY * 0.5 / total
                    val movZ = diffZ * 0.5 / total

                    entityTracker.setPlayerPosition(client, self.x + movX, self.y + movY, self.z + movZ)
                }
            }
        }
        Thread.sleep(500)
    }
}
