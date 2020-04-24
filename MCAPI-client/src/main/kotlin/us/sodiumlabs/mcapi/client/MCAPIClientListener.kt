package us.sodiumlabs.mcapi.client

import com.github.steveice10.mc.auth.data.GameProfile
import com.github.steveice10.mc.protocol.ClientListener
import com.github.steveice10.mc.protocol.MinecraftConstants
import com.github.steveice10.mc.protocol.data.SubProtocol
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent
import com.github.steveice10.mc.protocol.packet.handshake.client.HandshakePacket
import com.github.steveice10.mc.protocol.packet.login.client.EncryptionResponsePacket
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket
import com.github.steveice10.mc.protocol.packet.login.server.EncryptionRequestPacket
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.github.steveice10.packetlib.packet.Packet
import us.sodiumlabs.mcapi.common.Signing
import java.nio.ByteBuffer
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

const val PROTOCOL_VERSION = 575

class MCAPIClientListener(val subProtocol: SubProtocol, val signing: Signing, val secretSupplier: String):
        ClientListener(subProtocol) {

    override fun connected(event: ConnectedEvent) {
        if(SubProtocol.LOGIN == subProtocol) {
            val protocol = event.session.packetProtocol as MCAPIProtocol
            event.session.send(HandshakePacket(PROTOCOL_VERSION, event.session.host, event.session.port, HandshakeIntent.LOGIN))

            val profile: GameProfile = event.session.getFlag(MinecraftConstants.PROFILE_KEY);
            protocol.updateSubProtocol(SubProtocol.LOGIN, event.session)
            event.session.send(LoginStartPacket(profile.name))
        } else {
            super.connected(event)
        }
    }

    override fun packetReceived(event: PacketReceivedEvent) {
        val protocol = event.session.packetProtocol as MCAPIProtocol
        if(protocol.subProtocol == SubProtocol.LOGIN && event.getPacket<Packet>() is EncryptionRequestPacket) {
            val packet = event.getPacket<EncryptionRequestPacket>()
            val payload = signing.constructSignaturePayload(ByteBuffer.wrap(packet.verifyToken), secretSupplier)
            val key = generateKey()

            event.session.send(EncryptionResponsePacket(packet.publicKey, key, payload.array()))
            protocol.updateEncryption(key)
        } else {
            super.packetReceived(event)
        }
    }

    fun generateKey(): SecretKey {
        return try {
            val gen = KeyGenerator.getInstance("AES")
            gen.init(128)
            gen.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("Failed to generate shared key.", e)
        }
    }
}
