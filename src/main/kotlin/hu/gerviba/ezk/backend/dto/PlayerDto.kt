package hu.schbme.ezk.model

import hu.gerviba.ezk.backend.model.ingame.Player

data class PlayerDto(
    val name: String,
    val ping: Int = 0,
    val host: Boolean = false,
    val ready: Boolean = false
) {
    constructor(player: Player) : this(player.name, player.ping, player.host, player.ready)
}