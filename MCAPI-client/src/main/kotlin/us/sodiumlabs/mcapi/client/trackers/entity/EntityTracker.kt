package us.sodiumlabs.mcapi.client.trackers.entity

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction
import com.github.steveice10.mc.protocol.data.game.entity.EntityStatus
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType
import com.github.steveice10.mc.protocol.data.game.entity.type.`object`.ObjectType
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAnimationPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import org.apache.logging.log4j.LogManager
import us.sodiumlabs.mcapi.client.Client
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.time.LocalDateTime
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Predicate

class EntityTracker: HandlerLink {
    private val log = LogManager.getFormatterLogger()

    private val playerMetadata = HashMap<String, PlayerMetadata>()
    private val selfMetadata = SelfMetadata()
    private var uuid: String? = null
    private val selfId = 0

    private val entityMap = HashMap<Int, Entity>()
    private val playerMap = HashMap<String, Int>()

    private val deathListeners = mutableListOf<DeathListener>()

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerPlayerPositionRotationPacket::class.java, Consumer { packet ->
                    event.session.send(ClientTeleportConfirmPacket(packet.teleportId))
                    entityMap.compute(selfId) { key, e ->
                        if(e == null) {
                            Entity(key, MobTypeContainer(MobType.PLAYER), packet.yaw, packet.pitch, packet.x, packet.y, packet.z)
                        } else {
                            e.x = packet.x
                            e.y = packet.y
                            e.z = packet.z
                            e.yaw = packet.yaw
                            e.pitch = packet.pitch

                            e
                        }
                    }
                })
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
                        if(!removed) log.warn("Could not remove entity id $it")
                    }
                })
                .next(LoginSuccessPacket::class.java, Consumer { packet ->
                    uuid = packet.profile.idAsString
                    playerMetadata[packet.profile.idAsString] = PlayerMetadata(
                            id = packet.profile.idAsString,
                            name = packet.profile.name,
                            ping = 0)
                })
                .next(ServerPlayerAbilitiesPacket::class.java, Consumer { packet ->
                    selfMetadata.invincible = packet.isInvincible
                    selfMetadata.canFly = packet.isCanFly
                    selfMetadata.flying = packet.isFlying
                    selfMetadata.creative = packet.isCreative
                    selfMetadata.flySpeed = packet.flySpeed
                    selfMetadata.walkSpeed = packet.walkSpeed
                })
                .next(ServerPlayerChangeHeldItemPacket::class.java, Consumer { packet ->
                    selfMetadata.heldItemSlot = packet.slot
                })
                .next(ServerPlayerHealthPacket::class.java, Consumer { packet ->
                    if(packet.health == 0.0f) {
                        deathListeners.forEach {
                            it.onDeath(event.session, packet)
                        }
                    }

                    selfMetadata.health = packet.health
                    selfMetadata.food = packet.food
                    selfMetadata.saturation = packet.saturation
                })
                .next(ServerPlayerSetExperiencePacket::class.java, Consumer { packet ->
                    selfMetadata.experience = packet.experience
                    selfMetadata.level = packet.level
                    selfMetadata.totalExperience = packet.totalExperience
                })
                .next(ServerJoinGamePacket::class.java, Predicate { packet ->
                    selfMetadata.entityId = packet.entityId
                    selfMetadata.hardcore = packet.isHardcore

                    false
                })
                .next(ServerPlayerListEntryPacket::class.java, Predicate { packet ->
                    return@Predicate when(packet.action) {
                        PlayerListEntryAction.ADD_PLAYER -> {
                            packet.entries
                                    .map(this::convertFromPlayerListEntry)
                                    .forEach { playerMetadata[it.id] = it }
                            true
                        }
                        PlayerListEntryAction.UPDATE_LATENCY -> {
                            packet.entries.forEach {
                                playerMetadata[it.profile.idAsString]?.ping = it.ping
                            }
                            true
                        }
                        PlayerListEntryAction.UPDATE_GAMEMODE -> {
                            packet.entries.forEach {
                                playerMetadata[it.profile.idAsString]?.gameMode = it.gameMode
                            }
                            true
                        }
                        PlayerListEntryAction.REMOVE_PLAYER -> {
                            packet.entries.forEach {
                                playerMetadata.remove(it.profile.idAsString)
                            }
                            true
                        }
                        else -> false
                    }
                })
                .next(ServerEntityAnimationPacket::class.java, Consumer {  })
                .next(ServerEntityEquipmentPacket::class.java, Consumer {  })
                .finish()
    }

    fun addDeathListener(deathListener: DeathListener) {
        deathListeners.add(deathListener)
    }

    private fun convertFromPlayerListEntry(playerListEntry: PlayerListEntry): PlayerMetadata {
        val metadata = PlayerMetadata(
                id = playerListEntry.profile.idAsString,
                name = playerListEntry.profile.name,
                ping = playerListEntry.ping)
        metadata.gameMode = playerListEntry.gameMode
        return metadata
    }

    fun setPlayerPosition(client: Client, x: Double, y: Double, z: Double) {
        val packet = ClientPlayerPositionPacket(true, x, y, z)
        client.send(packet)
        entityMap[selfId]?.x = x
        entityMap[selfId]?.y = y
        entityMap[selfId]?.z = z
    }

    fun getPlayerEntityByUsername(username: String): Optional<Entity> {
        return playerMetadata.values.stream().filter { playerMetadata ->
            playerMetadata.name == username
        }.findFirst().flatMap { playerMetadata ->
            Optional.ofNullable(playerMap[playerMetadata.id])
        }.flatMap { id ->
            Optional.ofNullable(entityMap[id])
        }
    }

    fun getOwnEntity(): Optional<Entity> {
        return Optional.ofNullable(entityMap[selfId])
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

data class PlayerMetadata(
        val id: String,
        val name: String,
        var ping: Int) {

    var gameMode: GameMode? = null
}

class SelfMetadata {
    var hardcore = false
    var heldItemSlot = 0
    var entityId: Int? = null
    var invincible = false
    var canFly = false
    var flying = false
    var creative = false
    var flySpeed = 0.05f
    var walkSpeed = 0.1f
    var health = 20.0f
    var food = 20
    var saturation = 3.0f
    var experience = 0.0f
    var level = 0
    var totalExperience = 0
}
