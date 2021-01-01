package hu.gerviba.ezk.backend.controller

import hu.gerviba.ezk.backend.dto.RoomCreateRequest
import hu.gerviba.ezk.backend.dto.RoomCreateResponse
import hu.gerviba.ezk.backend.dto.RoomDto
import hu.gerviba.ezk.backend.service.RoomManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import kotlin.random.Random
import kotlin.random.nextInt

@RestController
@RequestMapping("/api")
class RoomsController {

    @Autowired
    private lateinit var rooms: RoomManager

    @GetMapping("/rooms")
    fun listRooms() : List<RoomDto> {
        println("Listing rooms...")
        return rooms.getAllAvailableRooms()
    }

    @PostMapping("/create")
    fun createRoom(@RequestBody roomDetails: RoomCreateRequest): RoomCreateResponse {
        println(roomDetails)
        val result = rooms.startRoom(roomDetails)
        println("A new room was started by code '${result.roomCode}'")
        return result
    }

}