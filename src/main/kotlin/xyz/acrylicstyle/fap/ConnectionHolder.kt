package xyz.acrylicstyle.fap

import xyz.acrylicstyle.fap.struct.FriendsTable
import xyz.acrylicstyle.fap.struct.PartyTable
import xyz.acrylicstyle.fap.struct.PlayersTable
import xyz.acrylicstyle.sql.DataType
import xyz.acrylicstyle.sql.Sequelize
import xyz.acrylicstyle.sql.TableDefinition
import java.sql.Driver
import java.sql.SQLException
import java.util.*
import kotlin.NoSuchElementException

class ConnectionHolder(host: String, database: String, user: String, password: String): Sequelize(
    host,
    database,
    user,
    password
) {
    lateinit var friends: FriendsTable
    lateinit var players: PlayersTable
    lateinit var party: PartyTable

    fun connect() {
        FAP.log.info("Connecting to database")
        var driver: Driver? = null
        try {
            driver = Class.forName("com.mysql.cj.jdbc.Driver").newInstance() as Driver
        } catch (ignore: ReflectiveOperationException) {
        }
        if (driver == null) {
            try {
                driver = Class.forName("com.mysql.jdbc.Driver").newInstance() as Driver
            } catch (ignore: ReflectiveOperationException) {
            }
        }
        if (driver == null) throw NoSuchElementException("Could not find any MySQL driver")
        val prop = Properties()
        prop.setProperty("maxReconnects", "2")
        prop.setProperty("autoReconnect", "true")
        try {
            authenticate(driver, prop)
        } catch (e: SQLException) {
            e.printStackTrace()
            return
        }
        friends = FriendsTable(define("friends", arrayOf(
            TableDefinition.Builder("uuid", DataType.STRING).setAllowNull(false).build(),
            TableDefinition.Builder("uuid2", DataType.STRING).setAllowNull(false).build(),
        )))
        players = PlayersTable(define("players", arrayOf(
            TableDefinition.Builder("uuid", DataType.STRING).setAllowNull(false).build(),
            TableDefinition.Builder("name", DataType.STRING).setAllowNull(true).build(),
            TableDefinition.Builder("party", DataType.INT).setAllowNull(true).build(),
            TableDefinition.Builder("invitedParty", DataType.INT).setAllowNull(true).build(),
            TableDefinition.Builder("prefix", DataType.STRING).setAllowNull(true).build(),
            TableDefinition.Builder("admin", DataType.BOOLEAN).setAllowNull(false).setDefaultValue(false).build(),
            TableDefinition.Builder("lastMessageFrom", DataType.STRING).setAllowNull(true).build(),
            TableDefinition.Builder("acceptingParty", DataType.BOOLEAN).setAllowNull(false).setDefaultValue(true).build(),
            TableDefinition.Builder("acceptingFriend", DataType.BOOLEAN).setAllowNull(false).setDefaultValue(true).build(),
            TableDefinition.Builder("acceptingMessage", DataType.BOOLEAN).setAllowNull(false).setDefaultValue(true).build(),
        )))
        party = PartyTable(define("party", arrayOf(
            TableDefinition.Builder("id", DataType.INT).setAllowNull(false).setPrimaryKey(true).setAutoIncrement(true).build(),
            TableDefinition.Builder("leader", DataType.STRING).setAllowNull(false).build(),
        )))
        sync()
        FAP.log.info("Successfully connected to database.")
    }
}
