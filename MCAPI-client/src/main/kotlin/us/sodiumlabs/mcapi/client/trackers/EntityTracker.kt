package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.data.game.entity.EntityStatus
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType
import com.github.steveice10.mc.protocol.data.game.entity.type.`object`.ObjectType
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
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.time.LocalDateTime
import java.util.function.Consumer
import java.util.function.Predicate

class EntityTracker: HandlerLink {
    private val entityMap = HashMap<Int, Entity>()
    private val playerMap = HashMap<String, Int>()

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerSpawnMobPacket::class.java, Consumer { packet ->
                    entityMap[packet.entityId] =
                            Entity(packet.entityId, MobTypeContainer(packet.type), packet.yaw, packet.pitch, packet.x, packet.y, packet.z)
                })
                .next(ServerSpawnPlayerPacket::class.java, Consumer { packet ->
                    playerMap[packet.uuid.toString()] = packet.entityId
                    entityMap[packet.entityId] =
                            Entity(packet.entityId, MobTypeContainer(MobType.PLAYER), packet.yaw, packet.pitch, packet.x, packet.y, packet.z)
                })
                .next(ServerSpawnObjectPacket::class.java, Consumer { packet ->
                    val entity = Entity(packet.entityId, ObjectTypeContainer(packet.type), packet.yaw, packet.pitch, packet.x, packet.y, packet.z)
                    entity.motionX = packet.motionX
                    entity.motionY = packet.motionY
                    entity.motionZ = packet.motionZ
                    packet.data
                    entityMap[packet.entityId] = entity
                })
                .next(ServerEntityTeleportPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.yaw = packet.yaw
                        entity.pitch = packet.pitch
                        entity.x = packet.x
                        entity.y = packet.y
                        entity.z = packet.z
                        entity.onGround = packet.isOnGround
                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityPositionRotationPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.onGround = packet.isOnGround
                        entity.yaw = packet.yaw
                        entity.pitch = packet.pitch
                        entity.x += packet.moveX
                        entity.y += packet.moveY
                        entity.z += packet.moveZ

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityPositionPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.onGround = packet.isOnGround
                        entity.x += packet.moveX
                        entity.y += packet.moveY
                        entity.z += packet.moveZ

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityVelocityPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.motionX = packet.motionX
                        entity.motionY = packet.motionY
                        entity.motionZ = packet.motionZ

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityRotationPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.onGround = packet.isOnGround
                        entity.yaw = packet.yaw
                        entity.pitch = packet.pitch

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityHeadLookPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.headYaw = packet.headYaw

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityMetadataPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.metadata = packet.metadata

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityPropertiesPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.attributes = packet.attributes

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityStatusPacket::class.java, Predicate { packet ->
                    entityMap.computeIfPresent(packet.entityId) { _, entity ->
                        entity.status = LocalDateTime.now() to packet.status

                        entity
                    }
                    entityMap.containsKey(packet.entityId)
                })
                .next(ServerEntityDestroyPacket::class.java, Consumer { packet ->
                    packet.entityIds.forEach {
                        val removed = entityMap.remove(it) != null
                        if(!removed) println("Could not remove entity id $it")
                    }
                })
                .finish()
    }
}

data class Entity(
        val entityId: Int,
        val type: EntityTypeContainer,

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

abstract class EntityTypeContainer(val entityType: EntityType)

class MobTypeContainer(val type: MobType): EntityTypeContainer(EntityType.Mob)
class ObjectTypeContainer(val type: ObjectType): EntityTypeContainer(EntityType.Object)

enum class EntityType {
    Mob, Object
}
