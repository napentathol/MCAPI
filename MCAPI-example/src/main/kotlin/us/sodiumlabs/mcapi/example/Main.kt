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
import us.sodiumlabs.mcapi.client.trackers.WorldTracker
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

    val entityTracker = client.queryLink<EntityTracker>("EntityTracker")
            .orElseThrow { RuntimeException("No entity tracker") }
    val worldTracker = client.queryLink<WorldTracker>("WorldTracker")
            .orElseThrow { RuntimeException("No world tracker") }

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

                            entityTracker.getPlayerEntityByUsername(username).ifPresent {
                                event.session.send(ClientChatPacket("/w $username following ${it.entityId}"))
                            }
                        } else if (message.translationParams[1].text.toLowerCase() == "stop") {
                            val username = message.translationParams[0].text
                            event.session.send(ClientChatPacket("/w $username OK"))
                            followMe.set(Optional.empty())
                        } else if (message.translationParams[1].text.toLowerCase() == "describe column") {
                            entityTracker.getOwnEntity().ifPresent { self ->
                                log.info(worldTracker.listColumn(self.x, self.z).contentToString())
                            }
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
            System.exit(-1)
        }
    })

    entityTracker.addDeathListener(object: DeathListener {
            override fun onDeath(session: Session, packet: ServerPlayerHealthPacket) {
                session.send(ClientRequestPacket(ClientRequest.RESPAWN))
                session.send(ClientChatPacket("I have returned."))
            }
        })

    client.connect()

    var ySpeed = 0.0
    while(true) {
        val selfEntity = entityTracker.getOwnEntity()

        selfEntity.ifPresent { self ->
            var movX = 0.0
            var movY = 0.0
            var movZ = 0.0

            followMe.get().flatMap { username ->
                entityTracker.getPlayerEntityByUsername(username)
            }.ifPresent { entity ->
                val diffX = entity.x - self.x
                val diffY = entity.y - self.y
                val diffZ = entity.z - self.z

                val total = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ)

                if (total > 1) {
                    movX = diffX * 0.5 / total
                    movY = diffY * 0.5 / total
                    movZ = diffZ * 0.5 / total
                }
            }

            val newX = self.x + movX
            var newY = self.y + movY
            val newZ = self.z + movZ

            var onGround = worldTracker.onGround(newX, newY, newZ)

            if(!onGround) {
                val groundY = worldTracker.findGroundY(newX, newY, newZ);
                newY += ySpeed

                if(newY < groundY) {
                    newY = groundY
                    onGround = true
                    ySpeed = 0.0
                } else {
                    ySpeed -= .98
                }
            } else {
                ySpeed = 0.0
            }

            entityTracker.setPlayerPosition(client, newX, newY, newZ, onGround)
        }


        Thread.sleep(500)
    }
}
