package hu.gerviba.ezk.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import hu.gerviba.ezk.backend.dto.EMPTY_ROOM_DTO
import hu.gerviba.ezk.backend.dto.GameMap
import hu.gerviba.ezk.backend.model.ingame.Player
import hu.gerviba.ezk.backend.model.ingame.StartedRoom
import hu.schbme.ezk.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Service
class GameLogic {

    val GAME_STATE_TOPIC = "/topic/game-state"
    val PLAYERS_TOPIC = "/topic/players"

    @Autowired
    lateinit var users: UserManagerService

    @Autowired
    lateinit var rooms: RoomManager

    @Autowired
    lateinit var outgoing: SimpMessagingTemplate

    @Autowired
    lateinit var mapper: ObjectMapper

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val maps = mutableMapOf<String, GameMap>()

    @Value("\${ezk.supported-maps:spaceship}")
    private lateinit var supportedMaps: String

    @PostConstruct
    fun init() {
        for (map in supportedMaps.split(Regex(",[ ]*")))
            loadMap(map)
    }

    private fun loadMap(map: String) {
        println("[MAP] New map '${map}' stored as: '${map.toLowerCase()}' from 'maps/${map.toLowerCase()}.json'")
        maps.put(map.toLowerCase(), mapper.readValue(ClassPathResource("maps/${map.toLowerCase()}.json").file, GameMap::class.java))
    }


    private fun sendMessageTo(player: Player, channel: String, payload: Any) {
        val headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE)
        with (headerAccessor) {
            sessionId = player.sessionId
            setLeaveMutable(true)
        }
        outgoing.convertAndSendToUser(player.sessionId, channel, payload, headerAccessor.messageHeaders)
    }

    fun notifyRoomUserDetails(roomCode: String) {
        println("[NOTIF] Room players of ${roomCode}")
        val room = rooms.getByCode(roomCode)
        for (user in room.players) {
            sendMessageTo(user, PLAYERS_TOPIC, room.players.map { PlayerDto(it) })
        }
    }

    fun notifyRoomGameState(roomCode: String, injectGame: Map<String, NoteDetailsDto?>? = null) {
        println("[NOTIF] Game state of ${roomCode}")
        val room = rooms.getByCode(roomCode)
        val gameState = room.generateGameState()
        for (user in room.players) {
            if (user.host) {
                sendMessageTo(user, GAME_STATE_TOPIC, gameState.copy(youHost = true, noteDetails = detailsForUser(user.sessionId, injectGame)))
            } else {
                sendMessageTo(user, GAME_STATE_TOPIC, gameState.copy(youHost = false, noteDetails = detailsForUser(user.sessionId, injectGame)))
            }
        }
    }

    fun notifyRoomGameState(roomCode: String, injectGame: NoteDetailsDto?) {
        println("[NOTIF] Game state of ${roomCode}")
        val room = rooms.getByCode(roomCode)
        val gameState = room.generateGameState()
        for (user in room.players) {
            if (user.host) {
                sendMessageTo(user, GAME_STATE_TOPIC, gameState.copy(youHost = true, noteDetails = injectGame))
            } else {
                sendMessageTo(user, GAME_STATE_TOPIC, gameState.copy(youHost = false, noteDetails = injectGame))
            }
        }
    }

    private fun detailsForUser(sessionId: String, injectGame: Map<String, NoteDetailsDto?>?): NoteDetailsDto? {
        if (injectGame == null)
            return null

        return injectGame[sessionId]
    }

    fun joinRoom(user: Player, roomCode: String, password: String) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode) {
            sendMessageTo(user, GAME_STATE_TOPIC,
                    GameStateDto(GameState.FAILED, HINT_ROOM_NOT_FOUND, EMPTY_ROOM_DTO, false))

        } else if (room.roomEntity.locked && room.roomEntity.password != password) {
            sendMessageTo(user, GAME_STATE_TOPIC,
                    GameStateDto(GameState.FAILED, HINT_INVALID_PASSWORD, EMPTY_ROOM_DTO, false))

        } else if (room.started && !room.players.any { it.rejoinToken == user.rejoinToken }) {
            sendMessageTo(user, GAME_STATE_TOPIC,
                    GameStateDto(GameState.FAILED, HINT_ALREADY_STARTED, EMPTY_ROOM_DTO, false))

        } else if (room.players.size >= room.roomEntity.maxPlayers) {
            sendMessageTo(user, GAME_STATE_TOPIC,
                    GameStateDto(GameState.FAILED, HINT_ROOM_IS_FULL, EMPTY_ROOM_DTO, false))

        } else {
            room.players.add(user)
            user.host = room.players.none { it.host }
            if (user.host)
                room.roomEntity.ownerName = user.name
            user.roomCode = roomCode
            notifyRoomGameState(roomCode)
            notifyRoomUserDetails(roomCode)
        }
    }

    fun startGame(user: Player, roomCode: String) {
        val room = rooms.getByCode(roomCode)
        if (!user.host)
            return

        rooms.removeFromList(room)
        room.ownerSessionId = user.sessionId
        room.started = true
        room.innerState.state = GameState.INGAME_INTRO
        room.mapResourcePrefix = room.roomEntity.map.toLowerCase()
        room.innerState.pathNotePlayer = ThreadLocalRandom.current().nextInt(100) % room.players.size
        room.innerState.loadMap(maps[room.roomEntity.map.toLowerCase()] ?: maps["spaceship"]!!)
        notifyRoomGameState(roomCode)
    }

    fun leaveRoom(user: Player) {
        val room = rooms.getByCode(user.roomCode)
        if (room.id >= 0) {
            room.players.removeIf { it.sessionId == user.sessionId }
            if (room.players.none { it.host }) {
                room.players.firstOrNull()?.let {
                    it.host = true
                    room.roomEntity.ownerName = it.name
                }
                if (!room.innerState.finished)
                    notifyRoomGameState(room.code)
            }
            notifyRoomUserDetails(room.code)
        }
    }

    fun nextPhase(user: Player, roomCode: String) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode)
            return

        println("[NEXT] Next phase: ${room.code}")

        if (!room.innerState.started) {
            room.innerState.started = true
            nextGate(room)
        } else if (room.innerState.finished) {

            println("Room finished: ${room.code}")
        } else {

            nextGate(room)
        }
    }

    private fun nextGate(room: StartedRoom) {
        if (room.innerState.availableQuest.isEmpty()) {
            victory(room)
            return
        }

        println("[NEXT] Next gate: ${room.code}")

        room.innerState.activeQuest = room.innerState.availableQuest.poll()
        room.innerState.reset()
        if (room.innerState.activeQuest == null || room.innerState.activeQuest?.gateDetails == null)
            return
        val activeQuest = room.innerState.activeQuest!!
        when (activeQuest.gateDetails.type) {
            GateType.WIRES, GateType.KEYPAD -> {
                for (index in activeQuest.gateDetails.buttons.indices) {
                    if (activeQuest.gateDetails.buttons[index].broken)
                        room.innerState.brokenButtons.add(index)
                }
                room.innerState.targetTime = System.currentTimeMillis() + (activeQuest.times[room.roomEntity.difficulty - 1] * 1000)
                room.innerState.state = GameState.INGAME_TASK

                room.innerState.pathNotePlayer = ThreadLocalRandom.current().nextInt(100) % room.players.size

                sendGameUpdate(room)

                scheduleGameEnd(activeQuest.innerState ?: 0, activeQuest.times[room.roomEntity.difficulty - 1], room)
            }
            GateType.PATHS -> {
                room.innerState.state = GameState.INGAME_SELECT
                sendGameUpdate(room)
            }
            GateType.DELAY -> {
            }
        }
    }

    private fun sendGameUpdate(room: StartedRoom) {
        println("[UPDATE] Game state: ${room.code}")
        val notes = mutableMapOf<String, NoteDetailsDto?>()
        for (userIndex in room.players.indices) {
            notes.put(room.players[userIndex].sessionId, room.generateNotesForUser(room.players[userIndex], userIndex))
        }
        notifyRoomGameState(room.code, notes)
        notifyRoomUserDetails(room.code)
    }

    private fun scheduleGameEnd(stateToCheck: Int, timeout: Int, room: StartedRoom) {
        scheduler.schedule({
            println("[TIMEOUT] ${room.innerState.activeQuest?.innerState ?: 0} == $stateToCheck ???")
            if (room.innerState.activeQuest?.innerState ?: 0 == stateToCheck) {
                executeSubmit(room.code)
            }
        }, timeout.toLong(), TimeUnit.SECONDS)
    }

    fun executeSubmit(roomCode: String) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode)
            return

        println("[SUBMIT] Submit: ${room.code}")

        if (room.innerState.activeQuest == null || room.innerState.activeQuest?.gateDetails == null)
            return
        val activeQuest = room.innerState.activeQuest!!

        if (!room.innerState.pathNotes.isEmpty())
            room.innerState.pathNotes.poll()


        waitForReboot(room, activeQuest.gateDetails.type)

        scheduler.schedule({
            when (activeQuest.gateDetails.type) {
                GateType.WIRES -> {
                    if (activeQuest.gateDetails.solution.toSet().equals(room.innerState.pressedButtons.toSet())) {
                        if (room.innerState.availableQuest.isEmpty()) {
                            victory(room)
                            return@schedule
                        } else {
                            nextDelay(room)
                        }
                    } else {
                        lose(room, "Wrong wires! The alarm was activated...", GateType.WIRES)
                    }
                }
                GateType.KEYPAD -> {
                    if (room.innerState.pressedButtons.equals(activeQuest.gateDetails.solution)) {
                        if (room.innerState.availableQuest.isEmpty()) {
                            victory(room)
                            return@schedule
                        } else {
                            nextDelay(room)
                        }
                    } else {
                        lose(room, "Wrong code! The alarm was activated...", GateType.KEYPAD)
                    }
                }
                else -> {
                }
            }
        }, 3, TimeUnit.SECONDS)
    }

    private fun waitForReboot(room: StartedRoom, type: GateType) {
        println("[REBOOT] Waiting for applied: ${room.code}")

        room.innerState.state = GameState.INGAME_DELAY
        val notes = NoteDetailsDto(
                gateDetails = GateDetails(type,
                        (room.innerState.activeQuest?.gateDetails?.graphics ?: "default")),
                notes = mutableListOf(),
                targetTime = 0,
                code = "",
                customTextHeader = "REBOOTING",
                customTextContent = "5, 4, 3, 2, 1, ...",
                leaveVisible = false,
                nextVisible = false
        )

        notifyRoomGameState(room.code, notes)
    }

    private fun nextDelay(room: StartedRoom) {
        room.innerState.state = GameState.INGAME_DELAY

        println("[DELAY] Delay applied: ${room.code}")

        val notes = NoteDetailsDto(
                gateDetails = GateDetails(GateType.PATHS, "spaceship_gate_authorized"),
                notes = mutableListOf(),
                targetTime = 0,
                code = "",
                customTextHeader = "AUTHORIZED",
                customTextContent = "Alarm system disarmed!",
                leaveVisible = false,
                nextVisible = true
        )

        notifyRoomGameState(room.code, notes)
    }

    private fun victory(room: StartedRoom) {
        room.innerState.state = GameState.WIN
        room.innerState.finished = true

        println("[END] Win: ${room.code}")

        val notes = NoteDetailsDto(
                gateDetails = GateDetails(GateType.PATHS, room.mapResourcePrefix + "_win"),
                notes = mutableListOf(),
                targetTime = 0,
                code = "",
                customTextHeader = "VICTORY",
                customTextContent = "You guys did it!",
                leaveVisible = true,
                nextVisible = false
        )

        notifyRoomGameState(room.code, notes)
    }

    private fun lose(room: StartedRoom, reason: String, type: GateType) {
        room.innerState.state = GameState.LOSE
        room.innerState.finished = true

        println("[END] Lose: ${room.code} reason: $reason")

        val notes = NoteDetailsDto(
                gateDetails = GateDetails(type, room.mapResourcePrefix + "_lose"),
                notes = mutableListOf(),
                targetTime = 0,
                code = "",
                customTextHeader = "YOU LOST",
                customTextContent = reason,
                leaveVisible = true,
                nextVisible = false
        )

        notifyRoomGameState(room.code, notes)
    }

    fun abortGame(roomCode: String) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode)
            return

        println("[END] Manual abort: ${room.code}")

        lose(room, "Alarm was triggered manually!", room.innerState.activeQuest?.gateDetails?.type ?: GateType.WIRES)
    }

    fun clickButton(user: Player, roomCode: String, buttonIndex: Int) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode)
            return

        println("[CLICK] In ${room.code} and button is $buttonIndex")

        room.innerState.pressedButtons.add(buttonIndex)
        if (room.innerState.activeQuest?.gateDetails?.type == GateType.WIRES)
            room.innerState.brokenButtons.add(buttonIndex)
        else if (room.innerState.activeQuest?.gateDetails?.type == GateType.KEYPAD)
            room.innerState.code += "*"

        sendGameUpdate(room)
    }

    fun selectPath(roomCode: String, direction: String) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode)
            return

        println("[PATH] Selected path of ${room.code} is $direction")

        if (room.innerState.activePath == null)
            return

        val path = room.innerState.activePath!!
        if (path.gateDetails.goodDirections.contains(direction.toUpperCase())) {
            if (!room.innerState.availablePaths.isEmpty())
                room.innerState.activePath = room.innerState.availablePaths.poll()
            room.innerState.pathNotes = LinkedList()
            room.innerState.activePath?.notes?.forEach { room.innerState.pathNotes.add(it.copy()) }
            nextGate(room)
        } else {
            lose(room, "This is the wrong way!", GateType.PATHS)
        }
    }

    fun spectate(user: Player, roomCode: String) {
        val room = rooms.getByCode(roomCode)
        if (room.code != roomCode)
            return

        room.spectators.add(user)
    }

}