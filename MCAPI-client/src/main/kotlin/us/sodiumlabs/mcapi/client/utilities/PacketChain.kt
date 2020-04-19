package us.sodiumlabs.mcapi.client.utilities

import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.github.steveice10.packetlib.packet.Packet
import java.util.function.Consumer
import java.util.function.Predicate

interface PacketChain {
    fun <T: Packet> next(klass: Class<T>, packetConsumer: Consumer<T>): PacketChain
    fun <T: Packet> next(klass: Class<T>, packetPredicate: Predicate<T>): PacketChain
    fun finish(): Boolean
}

private class FinishedPacketChain: PacketChain {
    override fun <T : Packet> next(klass: Class<T>, packetConsumer: Consumer<T>) = this
    override fun <T : Packet> next(klass: Class<T>, packetPredicate: Predicate<T>) = this
    override fun finish() = true
}

class ConcretePacketChain(private val event: PacketReceivedEvent): PacketChain {
    override fun <T: Packet> next(klass: Class<T>, packetConsumer: Consumer<T>): PacketChain {
        val packet = event.getPacket<Packet>()
        if(klass.isInstance(packet)) {
            packetConsumer.accept(event.getPacket())
            return FinishedPacketChain()
        }
        return this
    }

    override fun <T: Packet> next(klass: Class<T>, packetPredicate: Predicate<T>): PacketChain {
        val packet = event.getPacket<Packet>()
        if(klass.isInstance(packet) && packetPredicate.test(event.getPacket())) {
            return FinishedPacketChain()
        }
        return this
    }

    override fun finish() = false
}
