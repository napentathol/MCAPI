package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.data.game.setting.Difficulty
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDifficultyPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockValuePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateLightPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateViewPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerWorldBorderPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.util.function.Consumer

class WorldTracker: HandlerLink {
    private var difficulty: Difficulty = Difficulty.EASY
    private var age: Long = 0
    private var time: Long = 0

    private var chunkX = 0
    private var chunkZ = 0

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerChunkDataPacket::class.java, Consumer {  })
                .next(ServerUpdateLightPacket::class.java, Consumer {  })
                .next(ServerBlockChangePacket::class.java, Consumer {  })
                .next(ServerBlockValuePacket::class.java, Consumer {  })
                .next(ServerMultiBlockChangePacket::class.java, Consumer {  })
                .next(ServerWorldBorderPacket::class.java, Consumer {  })
                .next(ServerSpawnPositionPacket::class.java, Consumer {  })
                .next(ServerUpdateViewPositionPacket::class.java, Consumer { packet ->
                    chunkX = packet.chunkX
                    chunkZ = packet.chunkZ
                })
                .next(ServerDifficultyPacket::class.java, Consumer { packet ->
                    difficulty = packet.difficulty
                })
                .next(ServerUpdateTimePacket::class.java, Consumer { packet ->
                    age = packet.worldAge
                    time = packet.time
                })
                .finish()
    }
}
