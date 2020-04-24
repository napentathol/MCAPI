package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import org.apache.logging.log4j.LogManager
import us.sodiumlabs.mcapi.client.utilities.HandlerLink

class ErrorTracker: HandlerLink {
    private val log = LogManager.getFormatterLogger()
    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        log.error(String.format("Unable to handle packet: %s", event.getPacket()))
        return false
    }
}
