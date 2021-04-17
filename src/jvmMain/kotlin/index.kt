import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlinx.css.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun HTMLTag.css(builder: CSSBuilder.() -> Unit) = CSSBuilder().apply(builder).toString()

fun Route.registration() {
    get("/registration") {
        call.respondHtml {
            head {
                title("Регистрация")
            }
            body {
                val sd1 = css {
                    backgroundColor = Color.aliceBlue
                    display = Display.grid
                    gridTemplateColumns = GridTemplateColumns(1.fr)

                }
                form {
                    method = FormMethod.post
                    action = "/on_registration"
                    style = sd1
                    val child = css {
                        margin = "0 auto"
                        padding = "10px"
                    }
                    if (call.request.queryParameters["error"] == "1") {
                        p {
                            style = css {
                                color = Color.red
                                textAlign = TextAlign.center
                            }
                            +"Пользователь с данным логином уже существует"
                        }
                    }
                    label {
                        style = child
                        +"Логин   "; input { name = "login" }
                    }
                    label {
                        style = child
                        +"Пароль   "; passwordInput { name = "pass" }
                    }

                    button {
                        style = child
                        +"Зарегестрироваться";

                    }
                }
                val sd2 = css {
                    backgroundColor = Color.aliceBlue
                    display = Display.grid
                    gridTemplateColumns = GridTemplateColumns(2.fr)

                }
                form {
                    style = sd2
                    val child = css {
                        margin = "0 auto"
                        padding = "10px"
                    }
                    label {
                        style = child
                        +"Если вы уже зарегистрированы: "
                    }
                    a {
                        style = child
                        href = "/"
                        +"Войти";
                    }
                }
            }
        }
    }
    post("/on_registration") {

        val params = call.receiveParameters()
        val log = params["login"].orEmpty()
        val pass = params["pass"].orEmpty()

        if (transaction { User.select { User.login eq log }.count() > 0 }) {
            call.respondRedirect("/registration?error=1")
            return@post
        }

        transaction {
            User.insertAndGetId {
                it[login] = log
                it[password] = pass
            }
        }.let {
            call.sessions.set("cook", Cookies(it.value))
        }

        call.respondRedirect("/feeds")
    }
}

fun Routing.login() {
    get("/") {
        call.respondHtml {
            head {
                title("Авторизация")
            }
            body {
                val sd1 = css {
                    backgroundColor = Color.aliceBlue
                    display = Display.grid
                    gridTemplateColumns = GridTemplateColumns(1.fr)

                }
                form {
                    method = FormMethod.post
                    action = "/on_login"
                    style = sd1
                    val child = css {
                        margin = "0 auto"
                        padding = "10px"
                    }
                    if (call.request.queryParameters["error"] == "1") {
                        p {
                            style = css {
                                color = Color.red
                            }
                            +"Неправильный логин или пароль!"
                        }
                    }
                    label {
                        style = child
                        +"Логин   "; input { name = "login" }
                    }
                    label {
                        style = child
                        +"Пароль   "; passwordInput { name = "pass" }
                    }

                    button {
                        style = child
                        +"Войти";

                    }
                }
                val sd2 = css {
                    backgroundColor = Color.aliceBlue
                    display = Display.grid
                    gridTemplateColumns = GridTemplateColumns(2.fr)

                }
                div {
                    style = sd2
                    val child = css {
                        margin = "0 auto"
                        padding = "10px"
                    }
                    label {
                        style = child
                        +"Если вы не зарегистрированны: "
                    }
                    a {
                        style = child
                        href = "/registration"
                        +"Регистрация";

                    }
                }
            }
        }
    }
    post("/on_login") {
        val params = call.receiveParameters()
        val log = params["login"].orEmpty()
        val pass = params["pass"].orEmpty()

        transaction { User.select { User.login eq log and (User.password eq pass) }.firstOrNull() }.let {
            if (it == null) {
                call.respondRedirect("/?error=1")
                return@post
            }
            call.sessions.set("cook", Cookies(it[User.id].value))
        }

        call.respondRedirect("/feeds")
    }
}

fun Routing.feeds() {
    get("/feeds") {
        if (call.sessions.get("cook") as? Cookies == null) {
            call.respondRedirect("/")
            return@get
        }
        val messages = transaction {
            (Message innerJoin User).selectAll().map {
                "Имя пользователя: ${it[User.login]}\nСообщение: ${it[Message.text]}"
            }
        }
        call.respondHtml {
            head {
                title("Сообщения")
            }
            body {
                val sd1 = css {
                    backgroundColor = Color.aliceBlue
                    display = Display.grid
                    gridTemplateColumns = GridTemplateColumns(1.fr)
                }
                form {
                    method = FormMethod.post
                    action = "/on_exit_click"
                    style = sd1
                    button {
                        style = css {
                            width = 5.pct
                        }
                        +"Exit";
                    }
                }
                form {
                    method = FormMethod.post
                    action = "/on_send_click"
                    style = sd1
                    val child = css {
                        width = 100.pct
                        padding = "10px"
                    }
                    label {
                        style = child
                        +"Введите сообщение: "
                        textArea {
                            style = css {
                                width = 95.pct
                                height = 100.px
                                resize = Resize.none
                            }
                            name = "message"
                        }
                    }
                    button {
                        style = css {
                            width = 5.pct
                        }
                        +"Send";
                    }
                }
                form {
                    val child = css {
                        padding = "10px"
                    }
                    messages.forEach {
                        p {
                            +it
                        }
                    }
                }
            }
        }
    }
    post("/on_send_click") {
        val params = call.receiveParameters()
        val mess = params["message"]

        if (mess == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        call.sessions.get("cook").let { cook ->
            if (cook as? Cookies == null) {
                call.respondRedirect("/")
                return@post
            }

            transaction {
                Message.insert {
                    it[userId] = cook.userId
                    it[text] = mess
                }
            }

            call.respondRedirect("/feeds")
        }
    }
    post("/on_exit_click") {
        call.sessions.clear("cook")
        call.respondRedirect("/")
    }
}
