package us.sodiumlabs.mcapi.client.trackers

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk
import com.github.steveice10.mc.protocol.data.game.chunk.Column
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDifficultyPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockValuePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateLightPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateViewPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerWorldBorderPacket
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import us.sodiumlabs.mcapi.client.utilities.ConcretePacketChain
import us.sodiumlabs.mcapi.client.utilities.HandlerLink
import java.util.Objects
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.floor

private const val AIR = 0;

class WorldTracker: HandlerLink {
    private var difficulty: Difficulty = Difficulty.EASY
    private var age: Long = 0
    private var time: Long = 0

    private var spawnX = 0
    private var spawnY = 0
    private var spawnZ = 0

    private var chunkX = 0
    private var chunkZ = 0

    private val chunkTable: Table<Int, Int, ChunkColumn> = HashBasedTable.create()

    override fun packetReceived(event: PacketReceivedEvent): Boolean {
        return ConcretePacketChain(event)
                .next(ServerChunkDataPacket::class.java, Consumer { packet ->
                    chunkTable.put(packet.column.x, packet.column.z, convertChunkCube(packet.column))
                })
                .next(ServerUnloadChunkPacket::class.java, Consumer { packet ->
                    chunkTable.remove(packet.x, packet.z)
                })
                .next(ServerBlockChangePacket::class.java, Predicate { packet ->
                    if(chunkTable.contains(chunkPos(packet.record.position.x), chunkPos(packet.record.position.z))) {
                        updateBlock(
                                packet.record.position.x,
                                packet.record.position.y,
                                packet.record.position.z,
                                packet.record.block.id)

                        true
                    } else {
                        false
                    }
                })
                .next(ServerMultiBlockChangePacket::class.java, Predicate { packet ->
                    var handled = true
                    packet.records.forEach { record ->
                        val mhm = if(chunkTable.contains(chunkPos(record.position.x), chunkPos(record.position.z))) {
                            updateBlock(record.position.x, record.position.y, record.position.z, record.block.id)
                            true
                        } else {
                            false
                        }
                        handled = mhm && handled
                    }
                    handled
                })
                .next(ServerSpawnPositionPacket::class.java, Consumer { packet ->
                    spawnX = packet.position.x
                    spawnY = packet.position.y
                    spawnZ = packet.position.z
                })
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
                .next(ServerWorldBorderPacket::class.java, Consumer {  })
                .next(ServerUpdateLightPacket::class.java, Consumer {  })
                .next(ServerBlockValuePacket::class.java, Consumer {  })
                .finish()
    }

    fun getBlockAt(x: Int, y: Int, z: Int): Int {
        return Optional.ofNullable(chunkTable.get(chunkPos(x), chunkPos(z)))
                .map { p -> p.chunkCubes[chunkPos(y)] }
                .map { c -> c.blockstates[index(x, y, z)] }
                .orElse(AIR)
    }

    fun updateBlock(x: Int, y: Int, z: Int, id: Int) {
        if (chunkTable.contains(chunkPos(x), chunkPos(z))) {
            chunkTable.get(chunkPos(x), chunkPos(z))!!
                    .chunkCubes[chunkPos(y)]
                    .blockstates[index(x, y, z)] = id
        }
    }

    fun onGround(x: Double, y: Double, z: Double): Boolean {
        if(y < 0) return false

        return findGroundY(x, y, z) == y
    }

    fun findGroundY(x: Double, y: Double, z: Double): Double {
        val intX = floor(x).toInt()
        var intY = floor(y).toInt()
        val intZ = floor(z).toInt()

        while (intY > -1) {
            if(!Objects.equals(getBlockAt(intX, intY, intZ), AIR)) {
                return intY + 1.0
            }
            intY--
        }
        return -1.0
    }

    fun listColumn(x: Double, z: Double): Array<Int> {
        return listColumn(floor(x).toInt(), floor(z).toInt())
    }

    private fun listColumn(x: Int, z: Int): Array<Int> {
        val array = Array(256) { 0 }
        for(i in 0 until 256) {
            array[i] = getBlockAt(x, i, z)
        }
        return array
    }
}

private const val CHUNK_SIZE = 16 * 16 * 16

private fun convertChunkCube(column: Column): ChunkColumn {
    return ChunkColumn(column.chunks.map(::convertChunkCube).toTypedArray())
}

private fun convertChunkCube(chunk: Chunk?): ChunkCube {
    val states = Array(CHUNK_SIZE) { 0 }
    for (k in 0 until 16) {
        for (j in 0 until 16) {
            for (i in 0 until 16) {
                states[index(i, j, k)] = chunk?.get(i, j, k)?.id ?: -1
            }
        }
    }
    return ChunkCube(states)
}

private fun index(x: Int, y: Int, z: Int) = (y and 0xf shl 8) or (z and 0xf shl 4) or (x and 0xf)

private fun chunkPos(n: Int) = n shr 4

class ChunkColumn(val chunkCubes: Array<ChunkCube>) {
    override fun toString(): String {
        return chunkCubes.contentToString()
    }
}

class ChunkCube(val blockstates: Array<Int>) {
    override fun toString(): String {
        return blockstates.contentToString()
    }
}
