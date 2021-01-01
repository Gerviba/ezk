package hu.gerviba.ezk.backend.dto

val EMPTY_ROOM_DTO = RoomDto("", "", "", 0, 0, 0, true)

data class RoomDto(
        val code: String,
        val owner: String,
        val map: String,
        val maxPlayers: Int,
        val connectedPlayers: Int,
        val difficulty: Int,
        val locked: Boolean
) {
    constructor(code: RoomListEntry, connectedPlayers: Int) : this(
            code.code,
            code.ownerName,
            code.map,
            code.maxPlayers,
            connectedPlayers,
            code.difficulty,
            code.locked)

    companion object {
        fun empty() = EMPTY_ROOM_DTO
    }
}