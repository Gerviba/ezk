package hu.gerviba.ezk.backend.service

import hu.gerviba.ezk.backend.dto.RoomCreateRequest
import hu.gerviba.ezk.backend.dto.RoomCreateResponse
import hu.gerviba.ezk.backend.dto.RoomDto
import hu.gerviba.ezk.backend.dto.RoomListEntry
import hu.gerviba.ezk.backend.model.ingame.StartedRoom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom

const val AVAILABLE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
const val DEFAULT_OWNER_NAME = "Loading"

@Service
class RoomManager {

    @Autowired
    private lateinit var maps: MapManagerService

    @Value("\${ezk.max-rooms:200}")
    private var maxRooms: Int = 200

    @Value("\${ezk.max-players:6}")
    private var maxPlayers: Int = 6

    @Value("\${ezk.min-players:2}")
    private var minPlayers: Int = 2

    @Value("\${ezk.room-timeout:60000}")
    private var roomTimeout: Long = 60000

    @Value("\${ezk.room-code-length:4}")
    private var roomCodeLength: Int = 4

    @Value("\${ezk.show-empty-rooms:false}")
    private var showEmptyRooms: Boolean = false

    private val availableRooms = mutableMapOf<String, RoomListEntry>()

    private val startedRooms = mutableMapOf<String, StartedRoom>()

    private val roomCodesSoFar = mutableSetOf<String>()

    private var LAST_ID = 0

    private val DEFAULT_ROOM_ENTITY = RoomListEntry(-1, code = "", createdAt = 0, maxPlayers = 0, locked = false,
            password = "", difficulty = 0, map = "", ownerId = "", ownerName = "")

    private val DEFAULT_ROOMS = StartedRoom(-1, "****", ownerSessionId = "", roomEntity = DEFAULT_ROOM_ENTITY, mapResourcePrefix = "")

    fun startRoom(request: RoomCreateRequest): RoomCreateResponse {
        if (startedRooms.size >= maxRooms)
            return RoomCreateResponse(false, "Server is full", "")

        val maxPlayers = Math.max(Math.min(request.maxPlayers, maxPlayers), minPlayers)
        val difficoulty = Math.max(Math.min(request.difficulty, 3), 1)
        val roomCode = generateRoomCode()
        val map = maps.getMapNameOrDefault(request.map)
        val id = nextId()

        val roomEntity = RoomListEntry(id,
                code = roomCode,
                createdAt = System.currentTimeMillis(),
                started = false,
                startedAt = 0,
                maxPlayers = maxPlayers,
                locked = request.locked,
                password = request.password,
                difficulty = difficoulty,
                map = map,
                ownerName = DEFAULT_OWNER_NAME)
        availableRooms.put(roomCode, roomEntity)

        startedRooms.put(roomCode, StartedRoom(id, roomCode, roomEntity = roomEntity, mapResourcePrefix = ""))

        return RoomCreateResponse(true, "", roomCode)
    }

    fun getAllAvailableRooms(): List<RoomDto> {
        return availableRooms.values
                .map { Pair(it, startedRooms[it.code]) }
                .filter { it.second != null }
                .filter { showEmptyRooms || it.second?.players?.size ?: 0 > 0 }
                .map { RoomDto(
                    it.first.code,
                    it.first.ownerName,
                    it.first.map,
                    it.first.maxPlayers,
                    it.second?.players?.size ?: 0,
                    it.first.difficulty,
                    it.first.locked)
                }
    }

    private fun nextId() = ++LAST_ID

    private fun generateRoomCode(): String {
        synchronized(roomCodesSoFar) {
            var code: String
            do {
                code = ""
                for (i in 1..roomCodeLength)
                    code += AVAILABLE_CHARS[ThreadLocalRandom.current().nextInt(AVAILABLE_CHARS.length)]
            } while (roomCodesSoFar.contains(code))
            roomCodesSoFar.add(code)
            return code
        }
    }

    fun getByCode(roomCode: String) = startedRooms.getOrDefault(roomCode, DEFAULT_ROOMS)

    fun removeFromList(room: StartedRoom) {
        availableRooms.remove(room.code)
    }

}
