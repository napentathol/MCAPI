package us.sodiumlabs.mcapi.client.trackers.entity

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket
import com.github.steveice10.packetlib.Session

interface DeathListener {
    fun onDeath(session: Session, packet: ServerPlayerHealthPacket)
}
