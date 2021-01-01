package hu.gerviba.ezk.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class MapManagerService {

    @Value("\${ezk.supported-maps:spaceship}")
    private lateinit var supportedMaps: String

    private lateinit var maps: List<String>

    private lateinit var mapsWithNames: List<Pair<String, String>>

    @PostConstruct
    fun init() {
        maps = supportedMaps.split(Regex(",[ ]*"))
                .map { it.toLowerCase() }

        mapsWithNames = supportedMaps.split(Regex(",[ ]*"))
                .map { Pair(it.replace('_', ' '), it.toLowerCase()) }
    }

    fun getMapNameOrDefault(map: String): String =
            if (maps.contains(map.toLowerCase())) map else "spaceship"

    fun getMaps(): List<Pair<String, String>> = mapsWithNames

}