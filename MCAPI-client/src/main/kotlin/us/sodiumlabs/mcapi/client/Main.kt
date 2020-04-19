package us.sodiumlabs.mcapi.client

import com.github.steveice10.mc.auth.exception.request.RequestException
import com.github.steveice10.mc.protocol.MinecraftProtocol
import com.github.steveice10.mc.protocol.data.SubProtocol
import com.github.steveice10.mc.protocol.data.message.Message
import com.github.steveice10.mc.protocol.data.message.TranslationMessage
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket
import com.github.steveice10.mc.protocol.packet.status.server.StatusResponsePacket
import com.github.steveice10.packetlib.Client
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.github.steveice10.packetlib.event.session.SessionAdapter
import com.github.steveice10.packetlib.packet.Packet
import com.github.steveice10.packetlib.tcp.TcpSessionFactory
import com.google.gson.Gson
import us.sodiumlabs.mcapi.common.Signing
import java.nio.charset.Charset
import java.time.Clock
import java.util.Arrays
import java.util.function.Supplier

const val HOST = "localhost"
const val PORT = 25566

fun main(args: Array<String>) {
    val credFile = ClassLoader.getSystemClassLoader().getResource("keys.json")
    val creds = Gson().fromJson(credFile!!.readText(Charset.defaultCharset()), Creds::class.java)
    status()

    var protocol: MCAPIProtocol? = null
    try {
        protocol = MCAPIProtocol(creds.key)
        protocol.isUseDefaultListeners = false
    } catch (e: RequestException) {
        e.printStackTrace()
        return
    }

    val signing = Signing(Clock.systemUTC())
    val client = Client(HOST, PORT, protocol, TcpSessionFactory())
    client.session.addListener(MCAPIClientListener(SubProtocol.LOGIN, signing, Supplier { creds.secret }))
    client.session.addListener(object : SessionAdapter() {
        override fun packetReceived(event: PacketReceivedEvent) {
            if (event.getPacket<Packet>() is ServerJoinGamePacket) {
                event.session.send(ClientChatPacket("Hello, this is a test of MCProtocolLib."))
            } else if (event.getPacket<Packet>() is ServerChatPacket) {
                val message = event.getPacket<ServerChatPacket>().message
                println("Received Message: " + message.fullText)
                if (message is TranslationMessage) {
                    println("Received Translation Components: " + Arrays.toString(message.translationParams))
                }
                //weevent.session.disconnect("Finished")
            }
        }

        override fun disconnected(event: DisconnectedEvent) {
            println("Disconnected: " + Message.fromString(event.reason).fullText)
            if (event.cause != null) {
                event.cause.printStackTrace()
            }
        }
    })

    client.session.connect()
}

fun status() {
    val protocol = MinecraftProtocol(SubProtocol.STATUS)
    val client = Client(HOST, PORT, protocol, TcpSessionFactory())
    client.session.addListener(object: SessionAdapter() {
        override fun packetReceived(event: PacketReceivedEvent) {
            if (event.getPacket<Packet>() is StatusResponsePacket) {
                val packet = event.getPacket<StatusResponsePacket>()
                println(packet)
            }
        }
    })
    client.session.connect()
}

data class Creds(val key: String, val secret: String)
