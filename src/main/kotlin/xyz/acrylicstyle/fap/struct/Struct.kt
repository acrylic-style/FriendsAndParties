package xyz.acrylicstyle.fap.struct

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.connection.Server
import org.jetbrains.annotations.Contract
import util.CollectionList
import util.DataSerializer
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.sql.Table
import xyz.acrylicstyle.sql.TableData
import xyz.acrylicstyle.sql.options.FindOptions
import xyz.acrylicstyle.sql.options.InsertOptions
import xyz.acrylicstyle.sql.options.Sort
import xyz.acrylicstyle.sql.options.UpsertOptions
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.*
import kotlin.collections.HashMap

class FriendsTable(private val table: Table) {
    fun getFriends(player: UUID): Promise<CollectionList<UUID>> =
        table.findAll(FindOptions.Builder().addWhere("uuid", player.toString()).build()).then { it.map { td -> UUID.fromString(td.getString("uuid2")) } }

    fun getOnlineFriends(player: UUID): Promise<CollectionList<ProxiedPlayer>> = getFriends(player).then { l -> l.getOnlinePlayers() }

    fun isFriend(player1: UUID, player2: UUID): Promise<Boolean> =
        table.findOne(FindOptions.Builder().addWhere("uuid", player1.toString()).addWhere("uuid2", player2.toString()).build()).then { td -> td != null }

    fun addFriend(player1: UUID, player2: UUID): Promise<Void> = Promise.all(
        table.upsert(UpsertOptions.Builder().addWhere("uuid", player1.toString()).addWhere("uuid2", player2.toString()).addValue("uuid", player1.toString()).addValue("uuid2", player2.toString()).build()),
        table.upsert(UpsertOptions.Builder().addWhere("uuid", player2.toString()).addWhere("uuid2", player1.toString()).addValue("uuid", player2.toString()).addValue("uuid2", player1.toString()).build()),
    ).then { null }

    fun removeFriend(player1: UUID, player2: UUID): Promise<Void> = Promise.all(
        table.delete(FindOptions.Builder().addWhere("uuid", player1.toString()).addWhere("uuid2", player2.toString()).build()),
        table.delete(FindOptions.Builder().addWhere("uuid", player2.toString()).addWhere("uuid2", player1.toString()).build()),
    ).then { null }
}

class PlayersTable(private val table: Table) {
    private val playersCache = HashMap<UUID, Player>()

    fun clearCache() = playersCache.clear()

    fun removeAllParties(): Promise<Void> {
        return Promise.async {
            val statement = FAP.db.connection.createStatement()
            statement.executeUpdate("update players set party=null, invitedParty=null;")
            statement.close()
            return@async null
        }
    }

    fun getPlayer(uuid: UUID): Promise<Player> = Promise.async {
        playersCache[uuid]?.let { return@async it }
        val player = table.findOne(FindOptions.Builder().addWhere("uuid", uuid.toString()).build())
            .then { td ->
                if (td == null) return@then Player.parse(
                    table.insert(
                        InsertOptions.Builder().addValue("uuid", uuid.toString()).build()
                    ).complete()
                )
                return@then Player.parse(td)
            }.complete()
        playersCache[uuid] = player
        return@async player
    }

    @Deprecated("Heavy operation")
    fun getPlayer(name: String): Promise<Player?> =
        table.findOne(FindOptions.Builder().addWhere("name", name).build())
            .then { td -> if (td != null) Player.parse(td) else null }

    fun setName(uuid: UUID, name: String): Promise<Player> {
        playersCache[uuid]?.let { p -> p.name = name }
        return table.upsert(UpsertOptions.Builder().addWhere("uuid", uuid.toString()).addValue("uuid", uuid.toString()).addValue("name", name).build()).then { Player.parse(it[0]) }
    }

    fun getPartyPlayers(id: Int): Promise<CollectionList<Player>> =
        table.findAll(FindOptions.Builder().addWhere("party", id).build()).then { l -> l.map { td -> Player.parse(td) } }

    fun getPendingPartyPlayers(id: Int): Promise<CollectionList<Player>> =
        table.findAll(FindOptions.Builder().addWhere("invitedParty", id).build()).then { l -> l.map { td -> Player.parse(td) } }

    fun setPartyId(uuid: UUID, id: Int?): Promise<Player> {
        playersCache[uuid]?.let { p -> p.party = id }
        return table.upsert(UpsertOptions.Builder().addWhere("uuid", uuid.toString()).addValue("uuid", uuid.toString()).addValue("party", id).build()).then { Player.parse(it[0]) }
    }

    fun updatePlayer(player: Player): Promise<Void> {
        playersCache[player.uuid] = player
        return table.upsert(UpsertOptions.Builder()
            .addWhere("uuid", player.uuid.toString())
            .addValue("uuid", player.uuid.toString())
            .addValue("name", player.name)
            .addValue("party", player.party)
            .addValue("invitedParty", player.invitedParty)
            .addValue("prefix", player.prefix)
            .addValue("admin", player.admin)
            .addValue("lastMessageFrom", player.lastMessageFrom?.toString())
            .addValue("acceptingParty", player.acceptingParty)
            .addValue("acceptingFriend", player.acceptingFriend)
            .addValue("acceptingMessage", player.acceptingMessage)
            .build()).then { null }
    }
}

class PartyTable(private val table: Table) {
    fun getParty(uuid: UUID): Promise<Party?> = FAP.db.players.getPlayer(uuid).then { p -> p.party?.let { getParty(it).complete() } }

    fun getParty(id: Int): Promise<Party?> = table.findOne(FindOptions.Builder().addWhere("id", id).build()).then { td -> if (td == null) null else Party.parse(td) }

    fun disbandParty(id: Int): Promise<Void> = FAP.db.players.getPartyPlayers(id).then { list ->
        list.forEach { player ->
            if (player.invitedParty == id) player.invitedParty = null
            if (player.party == id) player.party = null
            player.update().complete()
        }
    }.then { null }

    fun createParty(leader: Player): Promise<Party> = table.findAll(FindOptions.Builder().setOrderBy("id").setOrder(Sort.DESC).setLimit(1).build())
        .then { list -> list[0] }
        .then { td -> td.getInteger("id") }
        .then { Party(it + 1, leader, CollectionList.of(leader)).apply { update().complete() } }

    fun updateParty(party: Party): Promise<Void> = table.upsert(UpsertOptions.Builder().addWhere("id", party.id).addValue("id", party.id).addValue("leader", party.leader.uuid.toString()).build()).then { null }
}

data class Party(
    val id: Int,
    var leader: Player,
    var members: CollectionList<Player>?,
) {
    companion object {
        fun parse(td: TableData) = Party(
            td.getInteger("id"),
            FAP.db.players.getPlayer(UUID.fromString(td.getString("leader"))).complete(),
            null,
        )
    }

    @Contract(pure = true)
    fun getMembers(): Promise<CollectionList<Player>> = FAP.db.players.getPartyPlayers(id).then { l -> members = l; members }

    @Contract(pure = true)
    fun getOnlineMembers(): Promise<CollectionList<ProxiedPlayer>> = getMembers().then { l -> l.map { p -> ProxyServer.getInstance().getPlayer(p.uuid) }.nonNull() }

    @Contract(pure = true)
    fun getOfflineMembers(): Promise<CollectionList<Player>> = getMembers().then { l -> l.filter { p -> ProxyServer.getInstance().getPlayer(p.uuid) == null } }

    fun update() = FAP.db.party.updateParty(this)
}

/**
 * A player that can represent both online and offline players.
 */
data class Player(
    val uuid: UUID,
    var name: String?,
    var party: Int?,
    var invitedParty: Int?,
    var friends: CollectionList<UUID>?,
    var prefix: String?,
    var admin: Boolean,
    var lastMessageFrom: UUID?,
    var acceptingParty: Boolean,
    var acceptingFriend: Boolean,
    var acceptingMessage: Boolean,
) {
    companion object {
        fun parse(td: TableData) = Player(
            UUID.fromString(td.getString("uuid")),
            td.getString("name"),
            td.getInteger("party"),
            td.getInteger("invitedParty"),
            null,
            td.getString("prefix"),
            td.getBoolean("admin") ?: false,
            td.getString("lastMessageFrom")?.let { UUID.fromString(it) },
            td.getBoolean("acceptingParty") ?: true,
            td.getBoolean("acceptingFriend") ?: true,
            td.getBoolean("acceptingMessage") ?: true,
        )
    }

    fun fetchFriends(): Promise<Player> =
        FAP.db.friends.getFriends(uuid).then { players -> friends = players }.then { this }

    fun updateName(name: String): Promise<Player> =
        FAP.db.players.setName(uuid, name).then {
            this.name = name
        }.then { this }

    fun setParty(id: Int?): Promise<Player> = FAP.db.players.setPartyId(uuid, id)

    fun update(): Promise<Player> = FAP.db.players.updatePlayer(this).then { this }

    private fun getInternalPrefix() = ChatColor.GRAY.toString() + if (prefix == null) "" else (ChatColor.translateAlternateColorCodes('&', prefix) + " ")

    fun getFullName() = "${getInternalPrefix()}$name"

    fun isOnline(): Boolean = ProxyServer.getInstance().getPlayer(uuid) != null
}

fun CollectionList<UUID>.getOnlinePlayers() = this.map { uuid -> ProxyServer.getInstance().getPlayer(uuid) }.nonNull()
fun CollectionList<UUID>.getPlayers() = this.map { uuid -> FAP.db.players.getPlayer(uuid) }
fun CollectionList<String>.doFilter(s: String) = this.filter { s2 -> s2.toLowerCase().startsWith(s.toLowerCase()) }
fun CollectionList<ProxiedPlayer>.broadcastMessage(text: TextComponent) = this.forEach { p -> p.sendMessage(text) }

fun String.toComponent() = TextComponent(this)
fun String.toComponent(color: ChatColor) = TextComponent("${color}$this")

fun Server.sendData(tag: String, subchannel: String, message: String) {
    val baos = ByteArrayOutputStream()
    val output = DataOutputStream(baos)
    output.writeUTF(subchannel)
    output.writeUTF(message)
    this.sendData(tag, baos.toByteArray())
}

fun Server.sendData(tag: String, subchannel: String, serializer: DataSerializer) = sendData(tag, subchannel, serializer.serialize())

fun ProxiedPlayer.playSound(sound: String, volume: Float, pitch: Float) {
    val serializer = DataSerializer()
    serializer.set("sound", sound)
    serializer.set("volume", volume.toString())
    serializer.set("pitch", pitch.toString())
    this.server.sendData("tomeito_lib:sound", this.uniqueId.toString(), serializer)
}
