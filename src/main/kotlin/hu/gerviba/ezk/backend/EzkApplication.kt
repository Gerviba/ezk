package hu.gerviba.ezk.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EzkApplication

fun main(args: Array<String>) {
    runApplication<EzkApplication>(*args)
}

const val PROFILE_WEBUI = "webui"