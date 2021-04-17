import org.jetbrains.exposed.dao.id.IntIdTable

object User : IntIdTable(columnName = "user_id") {
    val login = varchar("login", 16).uniqueIndex()
    val password = varchar("password", 20)
}

object Message : IntIdTable(columnName = "mess_id") {
    val text = text("text")
    val userId = reference("user_id", User)
}
