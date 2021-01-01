package hu.gerviba.ezk.backend.dto

data class RoomCreateRequest(
        val map: String = "",
        val difficulty: Int = 0,
        val maxPlayers: Int = 0,
        val locked: Boolean = false,
        val password: String = ""
)