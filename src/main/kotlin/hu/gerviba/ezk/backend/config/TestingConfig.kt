package hu.gerviba.ezk.backend.config

import hu.gerviba.ezk.backend.dto.RoomCreateRequest
import hu.gerviba.ezk.backend.service.RoomManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ThreadLocalRandom
import javax.annotation.PostConstruct

@Configuration
class TestingConfig {

    @Autowired
    private lateinit var rooms: RoomManager

    @PostConstruct
    fun init() {
        for (i in 0..8) {
            val locked = ThreadLocalRandom.current().nextBoolean()
            rooms.startRoom(RoomCreateRequest("spaceship",
                    ThreadLocalRandom.current().nextInt(1, 4),
                    ThreadLocalRandom.current().nextInt(2, 7),
                    locked,
                    if (locked) "1234" else ""))
        }
    }

}