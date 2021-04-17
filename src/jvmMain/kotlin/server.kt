import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level

fun main() {
    Database.connect("jdbc:sqlite:database.db")
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(User, Message)
    }
    embeddedServer(Netty, port = 8081) {
        install(CallLogging) {
            level = Level.DEBUG
        }
        install(Sessions) {
            cookie<Cookies>("cook")
        }
        routing {
            login()
            registration()
            feeds()
        }
    }.start(wait = true)
}

