package hu.schbme.ezk.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

enum class NoteType {
    SIMPLE,
    PATH_INFO
}

data class NoteDto(
    val type: NoteType,
    val title: String,
    val text: String
)

enum class GateType {
    WIRES,
    KEYPAD,
    PATHS,
    DELAY
}

data class ButtonDto(
    val id: Int,
    val color: String,
    val name: String,
    val broken: Boolean
)

enum class FeatureType {
    MODEL,
    VERSION,
    CREATED,
    EDITION,
    MANUFACTURER,
    MADE_IN,
    ABORT
}

data class FeatureDto(
    val id: Int,
    val type: FeatureType,
    val value: String
)

const val DIRECTION_LEFT = "LEFT"
const val DIRECTION_FORWARD ="FORWARD"
const val DIRECTION_RIGHT = "RIGHT"

data class GateDetails(
    val type: GateType,
    val graphics: String,
    val buttons: List<ButtonDto> = listOf(),
    val features: List<FeatureDto> = listOf(),

    /**
     * A list (or set if type=WIRES) of ids to be pressed to complete
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val solution: List<Int> = listOf(),

    /**
     * The good direction. Only for
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val goodDirections: List<String> = listOf(DIRECTION_LEFT, DIRECTION_FORWARD, DIRECTION_RIGHT)
)

data class NoteDetailsDto(
        val gateDetails: GateDetails,
        val targetTime: Long,
        val notes: MutableList<NoteDto>,
        val code: String = "",
        var customTextHeader: String = "",
        var customTextContent: String = "",
        var leaveVisible: Boolean = false,
        var nextVisible: Boolean = false,
        @Volatile var innerState: Int? = -1,

        @JsonIgnore val times: IntArray = intArrayOf(60, 50, 40)
)