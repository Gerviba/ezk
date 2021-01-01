package hu.gerviba.ezk.backend.model.ingame

import hu.gerviba.ezk.backend.dto.GameMap
import hu.gerviba.ezk.backend.dto.RoomDto
import hu.gerviba.ezk.backend.dto.RoomListEntry
import hu.schbme.ezk.model.*
import java.util.*
import java.util.concurrent.ThreadLocalRandom

data class InnerState(
        var state: GameState = GameState.LOBBY,
        var availableQuest: Queue<NoteDetailsDto> = LinkedList(),
        @Volatile var activeQuest: NoteDetailsDto? = null,
        var availablePaths: Queue<NoteDetailsDto> = LinkedList(),
        @Volatile var activePath: NoteDetailsDto? = null,
        var started: Boolean = false,
        var finished: Boolean = false,
        var code: String = "",
        var brokenButtons: MutableList<Int> = mutableListOf(),
        var pressedButtons: MutableList<Int> = mutableListOf(),
        var targetTime: Long = 0,
        var pathNotePlayer: Int = 0,
        var pathNotes: Queue<NoteDto> = LinkedList()
) {
    fun reset() {
        code = ""
        brokenButtons = mutableListOf()
        pressedButtons = mutableListOf()
        targetTime = 0
    }

    fun loadMap(gameMap: GameMap) {
        availableQuest = LinkedList()
        availablePaths = LinkedList()

        // TODO: Used for testing
        availableQuest.addAll(gameMap.sectors["1"]!!)
        availableQuest.add(gameMap.paths["1"]!![0])
        availablePaths.add(gameMap.paths["1"]!![0])
        availableQuest.addAll(gameMap.sectors["2"]!!)
        availableQuest.add(gameMap.paths["2"]!![0])
        availablePaths.add(gameMap.paths["2"]!![0])
        activePath = availablePaths.poll()

        pathNotes = LinkedList()
        activePath?.notes?.forEach { pathNotes.add(it.copy()) }
    }
}

data class StartedRoom(
        var id: Int,
        var code: String,
        var players: MutableList<Player> = mutableListOf(),
        var ownerSessionId: String = "",
        var started: Boolean = false,
        var roomEntity: RoomListEntry,
        val innerState: InnerState = InnerState(),
        var mapResourcePrefix: String,
        val spectators: MutableList<Player> = mutableListOf()
) {

    fun generateGameState(): GameStateDto {
        return GameStateDto(innerState.state, NO_HINT, RoomDto(roomEntity, players.size),false)
    }

    fun generateNotesForUser(user: Player, userIndex: Int): NoteDetailsDto? {
        return innerState.activeQuest?.copy(
                code = innerState.code,
                targetTime = innerState.targetTime,
                notes = filterNotes(innerState.activeQuest?.notes ?: mutableListOf(), userIndex),
                gateDetails = innerState.activeQuest?.gateDetails?.copy(
                        buttons = filterButtons(innerState.activeQuest?.gateDetails?.buttons ?: listOf()))
                        ?: GateDetails(GateType.DELAY, mapResourcePrefix + "_default"))
    }

    private fun filterNotes(notes: MutableList<NoteDto>, userIndex: Int): MutableList<NoteDto> {
        val result = mutableListOf<NoteDto>()
        for (i in notes.indices)
            if (i % players.size == userIndex)
                result.add(notes[i].copy())
        if (innerState.pathNotePlayer == userIndex && !innerState.pathNotes.isEmpty())
            result.add(innerState.pathNotes.element())
        return result
    }

    private fun filterButtons(buttons: List<ButtonDto>): List<ButtonDto> {
        val result = mutableListOf<ButtonDto>()
        for (i in buttons.indices)
            result.add(buttons[i].copy(broken = innerState.brokenButtons.contains(i)))
        return result
    }
}