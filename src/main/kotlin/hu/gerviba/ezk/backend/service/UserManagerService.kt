package hu.gerviba.ezk.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import hu.gerviba.ezk.backend.model.ingame.Player
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Service
class UserManagerService {

    val userStorage = ConcurrentHashMap<String, Player>()

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var control: GameLogic

    @PostConstruct
    fun init() {

    }

    fun getOrCreatePlayer(sessionId: String, username: String, rejoinToken: String): Player {
        return userStorage.computeIfAbsent(sessionId) {
            session -> Player(session, username, rejoinToken = rejoinToken)
        }
    }

    fun getPlayer(sessionId: String): Player {
        return if (userStorage.containsKey(sessionId))
            userStorage[sessionId]!!
        else
            throw RuntimeException("Player `$sessionId` was not found!")
    }

    fun disconnectUser(sessionId: String) {
        userStorage[sessionId]?.let { user -> control.leaveRoom(user) }
    }

//    fun getMapOrDefault(name: String): MapEntity {
//        return maps.getOrDefault(name, maps["index"]!!)
//    }
//
//    fun getAll() = userStorage.values

}