package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareCommandsPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareRecipesPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDeclareTagsPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerUnlockRecipesPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.util.function.Consumer

class MiscTracker: HandlerLink {
    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerPlayEffectPacket::class.java, Consumer {  })
                .next(ServerPluginMessagePacket::class.java, Consumer {  })
                .next(ServerDeclareRecipesPacket::class.java, Consumer {  })
                .next(ServerDeclareTagsPacket::class.java, Consumer {  })
                .next(ServerDeclareCommandsPacket::class.java, Consumer {  })
                .next(ServerUnlockRecipesPacket::class.java, Consumer {  })
                .next(ServerAdvancementsPacket::class.java, Consumer {  })
                .finish()
    }
}
