package hu.schbme.ezk.model

import hu.gerviba.ezk.backend.dto.RoomDto

const val HINT_ROOM_IS_FULL = "roomIsFull"
const val HINT_INVALID_PASSWORD = "invalidPassword"
const val HINT_ALREADY_STARTED = "alreadyStarted"
const val HINT_ROOM_NOT_FOUND = "roomNotFound"
const val NO_HINT = ""

enum class GameState {
    FAILED,
    LOBBY,
    INGAME_INTRO,
    INGAME_DELAY,
    INGAME_TASK,
    INGAME_SELECT,
    LOSE,
    WIN,
    CLOSED;
}

data class GameStateDto(
        var state: GameState = GameState.FAILED,
        var hint: String = NO_HINT,
        var room: RoomDto = RoomDto.empty(),
        var youHost: Boolean = false,
        var noteDetails: NoteDetailsDto? = null
)