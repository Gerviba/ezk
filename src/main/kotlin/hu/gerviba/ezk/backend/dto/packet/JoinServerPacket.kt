package hu.gerviba.ezk.backend.dto.packet

data class JoinServerPacket(
        val roomCode: String,
        val password: String
)