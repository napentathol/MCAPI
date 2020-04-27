package us.sodiumlabs.mcapi.client.utilities

import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.github.steveice10.packetlib.event.session.SessionAdapter

class HandlerChain: SessionAdapter() {
    private val chain: MutableList<HandlerLink> = mutableListOf()

    override fun packetReceived(event: PacketReceivedEvent) {
        chain.forEach {
            if(it.packetReceived(event)) return
        }
    }

    fun addLink(link: HandlerLink): HandlerChain {
        chain.add(link)
        return this
    }

    fun clearAllLinks() {
        chain.clear()
    }
}

interface HandlerLink {
    fun packetReceived(event: PacketReceivedEvent): Boolean
}
