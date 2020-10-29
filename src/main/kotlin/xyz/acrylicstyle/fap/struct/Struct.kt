package xyz.acrylicstyle.fap.struct

import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import util.CollectionList
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.sql.Table
import xyz.acrylicstyle.sql.TableData
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.util.*

class FriendsTable(private val table: Table) {
    fun getFriends(player: UUID): Promise<CollectionList<UUID>> =
        table.findAll(FindOptions.Builder().addWhere("uuid", player.toString()).build()).then { it.map { td -> UUID.fromString(td.getString("uuid2")) } }

    fun getOnlineFriends(player: UUID): Promise<CollectionList<ProxiedPlayer>> = getFriends(player).then { l -> l.getOnlinePlayers() }
}

class PlayersTable(private val table: Table) {
    fun getPlayer(uuid: UUID): Promise<Player?> =
        table.findOne(FindOptions.Builder().addWhere("uuid", uuid.toString()).build())
            .then { td ->
                if (td == null) return@then Player.parse(
                    table.insert(
                        InsertOptions.Builder().addValue("uuid", uuid.toString()).addValue("name", null).build()
                    ).complete()
                )
                return@then Player.parse(td)
            }

    fun getPlayer(name: String): Promise<Player?> =
        table.findOne(FindOptions.Builder().addWhere("name", name).build())
            .then { td -> if (td != null) Player.parse(td) else null }

    fun setName(uuid: UUID, name: String): Promise<Player> =
        table.upsert(UpsertOptions.Builder().addWhere("uuid", uuid.toString()).addValue("uuid", uuid.toString()).addValue("name", name).build()).then { Player.parse(it[0]) }
}

data class Player(
    val uuid: UUID,
    var name: String,
    var friends: CollectionList<UUID>?,
) {
    companion object {
        fun parse(td: TableData) = Player(
            UUID.fromString(td.getString("uuid")),
            td.getString("name"),
            null,
        )
    }

    fun fetchFriends(): Promise<Player> =
        FAP.db.friends.getFriends(uuid).then { players -> friends = players }.then { this }

    fun updateName(name: String): Promise<Player> =
        FAP.db.players.setName(uuid, name).then {
            this.name = name
        }.then { this }
}

fun CollectionList<UUID>.getOnlinePlayers() = this.map { uuid -> ProxyServer.getInstance().getPlayer(uuid) }.nonNull()
fun CollectionList<UUID>.getPlayers() = this.map { uuid -> FAP.db.players.getPlayer(uuid) }.nonNull() // it should not be removed though
