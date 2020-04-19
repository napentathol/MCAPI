package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.util.function.Consumer
import java.util.function.Predicate

class PlayerTracker: HandlerLink {
    private val playerMetadata = HashMap<String, PlayerMetadata>()
    private val selfMetadata = SelfMetadata()

    private var uuid: String? = null

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
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
                .finish()
    }

    private fun convertFromPlayerListEntry(playerListEntry: PlayerListEntry): PlayerMetadata {
        val metadata = PlayerMetadata(
                id = playerListEntry.profile.idAsString,
                name = playerListEntry.profile.name,
                ping = playerListEntry.ping)
        metadata.gameMode = playerListEntry.gameMode
        return metadata
    }
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
