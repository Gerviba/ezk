package hu.gerviba.ezk.backend.controller

import hu.gerviba.ezk.backend.dto.packet.JoinServerPacket
import hu.gerviba.ezk.backend.service.GameLogic
import hu.gerviba.ezk.backend.service.UserManagerService
import hu.schbme.ezk.model.packet.LocalStatePacket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller

@Controller
class IngameWebsocketController {

    @Autowired
    private lateinit var users: UserManagerService

    @Autowired
    private lateinit var control: GameLogic

    @MessageMapping("/join")
    @SendToUser("/topic/game-state")
    fun joinLobby(@Payload payload: JoinServerPacket, @Header("simpSessionId") sessionId: String) {
        val user = users.getPlayer(sessionId)
        println("[JOIN] ${user.sessionId} $payload")
        control.joinRoom(user, payload.roomCode, payload.password)
    }

    @MessageMapping("/spectate")
    @SendToUser("/topic/game-state")
    fun joinScreen(@Payload payload: JoinServerPacket, @Header("simpSessionId") sessionId: String) {
        val user = users.getPlayer(sessionId)
        println("[SPECTATE] ${user.sessionId} $payload")
        control.spectate(user, payload.roomCode)
    }

    @MessageMapping("/ready")
    fun lobbyReady(@Header("simpSessionId") sessionId: String) {
        val user = users.getPlayer(sessionId)
        if (user.roomCode.isNotEmpty()) {
            user.ready = true
            control.notifyRoomUserDetails(user.roomCode)
        }
    }

    @MessageMapping("/start")
    fun startGame(@Header("simpSessionId") sessionId: String) {
        val user = users.getPlayer(sessionId)
        control.startGame(user, user.roomCode)
    }

    @MessageMapping("/next")
    fun nextPhase(@Header("simpSessionId") sessionId: String, @Payload localState: LocalStatePacket) {
        val user = users.getPlayer(sessionId)
        control.nextPhase(user, user.roomCode)
    }

    @MessageMapping("/path/{direction}")
    fun goDirection(@Header("simpSessionId") sessionId: String, @DestinationVariable direction: String, @Payload localState: LocalStatePacket) {
        val user = users.getPlayer(sessionId)
        control.selectPath(user.roomCode, direction)
    }

    @MessageMapping("/click/{i}")
    fun buttonClick(@Header("simpSessionId") sessionId: String, @DestinationVariable i: Int, @Payload localState: LocalStatePacket) {
        val user = users.getPlayer(sessionId)
        control.clickButton(user, user.roomCode, i)
    }

    @MessageMapping("/abort")
    fun abort(@Header("simpSessionId") sessionId: String, @Payload localState: LocalStatePacket) {
        val user = users.getPlayer(sessionId)
        control.abortGame(user.roomCode)
    }

    @MessageMapping("/submit")
    fun submit(@Header("simpSessionId") sessionId: String, @Payload localState: LocalStatePacket) {
        val user = users.getPlayer(sessionId)
        control.executeSubmit(user.roomCode)
    }


}