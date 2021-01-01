package hu.gerviba.ezk.backend.controller

import hu.gerviba.ezk.backend.PROFILE_WEBUI
import hu.gerviba.ezk.backend.service.MapManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpSession

const val SESSION_USERNAME = "username"

@Profile(PROFILE_WEBUI)
@Controller
class WebUiController {

    @Autowired
    private lateinit var maps: MapManagerService

    @GetMapping("/")
    fun index(): String {
        return "index"
    }

    @PostMapping("/game")
    fun openGame(@RequestParam(defaultValue = "player") username: String, httpSession: HttpSession): String {
        httpSession.setAttribute(SESSION_USERNAME, username)
        return "redirect:/game"
    }

    @GetMapping("/game")
    fun roomList(model: Model): String {
        return "list-rooms"
    }

    @GetMapping("/create-room")
    fun createRoom(model: Model): String {
        model.addAttribute("maps", maps.getMaps())
        return "create-room"
    }

    @PostMapping("/room")
    fun room(@RequestParam(defaultValue = "") roomCode: String,
             @RequestParam(defaultValue = "") password: String,
             model: Model,
             httpSession: HttpSession
    ): String {

        model.addAttribute("screen", false)
        model.addAttribute("username", httpSession.getAttribute(SESSION_USERNAME) ?: "Player")
        model.addAttribute("code", roomCode)
        model.addAttribute("password", password)
        return "ingame"
    }

    @GetMapping("/screen/{roomCode}")
    fun screen(@PathVariable roomCode: String, model: Model): String {
        model.addAttribute("screen", true)
        model.addAttribute("username", "screen")
        model.addAttribute("code", roomCode)
        return "ingame"
    }

}