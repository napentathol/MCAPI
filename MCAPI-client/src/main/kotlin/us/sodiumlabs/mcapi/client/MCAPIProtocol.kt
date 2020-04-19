package us.sodiumlabs.mcapi.client

import com.github.steveice10.mc.protocol.MinecraftProtocol
import com.github.steveice10.mc.protocol.data.SubProtocol
import com.github.steveice10.packetlib.Session
import javax.crypto.SecretKey

class MCAPIProtocol(name: String): MinecraftProtocol(name) {
    fun updateSubProtocol(subProtocol: SubProtocol, session: Session) {
        setSubProtocol(subProtocol, true, session)
    }

    fun updateEncryption(key: SecretKey) {
        enableEncryption(key)
    }
}
