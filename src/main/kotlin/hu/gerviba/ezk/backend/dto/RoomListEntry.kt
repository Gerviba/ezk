package hu.gerviba.ezk.backend.dto

data class RoomListEntry(
        var id: Int,
        var code: String,
        var createdAt: Long,
        var started: Boolean = false,
        var startedAt: Long = 0,
        var maxPlayers: Int,
        var locked: Boolean,
        var password: String,
        var difficulty: Int,
        var map: String,
        var ownerId: String = "",
        var ownerName: String
)