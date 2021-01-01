package hu.gerviba.ezk.backend.dto

data class RoomCreateResponse(
        val done: Boolean,
        val hint: String = "",
        val roomCode: String
)