package hu.gerviba.ezk.backend.model.ingame

data class Player(
        var sessionId: String,
        val name: String,
        var roomCode: String = "",
        var rejoinToken: String,
        var ping: Int = 0,
        var ready: Boolean = false,
        var host: Boolean = false
)