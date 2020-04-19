package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket
import com.github.steveice10.mc.protocol.packet.login.server.EncryptionRequestPacket
import com.github.steveice10.mc.protocol.packet.login.server.LoginSetCompressionPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.time.LocalDateTime
import java.util.function.Consumer

class ServerCommsTracker: HandlerLink {
    private var lastKeepAlive: LocalDateTime? = null

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerKeepAlivePacket::class.java, Consumer { lastKeepAlive = LocalDateTime.now() })
                .next(LoginSetCompressionPacket::class.java, Consumer {  })
                .next(EncryptionRequestPacket::class.java, Consumer {  })
                .next(ServerChatPacket::class.java, Consumer {  })
                .finish()
    }
}
