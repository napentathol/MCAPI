package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.data.game.entity.EntityStatus
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.time.LocalDateTime
import java.util.function.Consumer

class EntityTracker: HandlerLink {
    private val entityMap = HashMap<Int, Entity>()
    private val playerMap = HashMap<String, Int>()

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerSpawnMobPacket::class.java, Consumer { packet ->
                    entityMap[packet.entityId] =
                            Entity(packet.entityId, packet.type, packet.yaw, packet.pitch, packet.x, packet.y, packet.z)
                })
                .next(ServerSpawnPlayerPacket::class.java, Consumer { packet ->
                    println("NEW PLAYER:::: ")
                    println(packet)
                    playerMap[packet.uuid.toString()] = packet.entityId
                    entityMap[packet.entityId] =
                            Entity(packet.entityId, MobType.PLAYER, packet.yaw, packet.pitch, packet.x, packet.y, packet.z)
                })
                .next(ServerEntityTeleportPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.yaw = packet.yaw
                        entity.pitch = packet.pitch
                        entity.x = packet.x
                        entity.y = packet.y
                        entity.z = packet.z
                        entity.onGround = packet.isOnGround
                        entity
                    }
                })
                .next(ServerEntityPositionRotationPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.onGround = packet.isOnGround
                        entity.yaw = packet.yaw
                        entity.pitch = packet.pitch
                        entity.x += packet.moveX
                        entity.y += packet.moveY
                        entity.z += packet.moveZ

                        entity
                    }
                })
                .next(ServerEntityPositionPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.onGround = packet.isOnGround
                        entity.x += packet.moveX
                        entity.y += packet.moveY
                        entity.z += packet.moveZ

                        entity
                    }
                })
                .next(ServerEntityVelocityPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.motionX = packet.motionX
                        entity.motionY = packet.motionY
                        entity.motionZ = packet.motionZ

                        entity
                    }
                })
                .next(ServerEntityRotationPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.onGround = packet.isOnGround
                        entity.yaw = packet.yaw
                        entity.pitch = packet.pitch

                        entity
                    }
                })
                .next(ServerEntityHeadLookPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.headYaw = packet.headYaw

                        entity
                    }
                })
                .next(ServerEntityMetadataPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.metadata = packet.metadata

                        entity
                    }
                })
                .next(ServerEntityPropertiesPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.attributes = packet.attributes

                        entity
                    }
                })
                .next(ServerEntityStatusPacket::class.java, Consumer { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.status = LocalDateTime.now() to packet.status

                        entity
                    }
                })
                .next(ServerEntityDestroyPacket::class.java, Consumer { packet ->
                    packet.entityIds.forEach {
                        entityMap.remove(it)
                    }
                })
                .finish()
    }
}

data class Entity(
        val entityId: Int,
        val type: MobType,

        var yaw: Float,
        var pitch: Float,

        var x: Double,
        var y: Double,
        var z: Double) {

    var attributes: List<Attribute> = listOf()
    var headYaw: Float? = null
    var onGround: Boolean? = null

    var motionX: Double? = null
    var motionY: Double? = null
    var motionZ: Double? = null

    var metadata: Array<EntityMetadata>? = null
    var status: Pair<LocalDateTime, EntityStatus>? = null
}
