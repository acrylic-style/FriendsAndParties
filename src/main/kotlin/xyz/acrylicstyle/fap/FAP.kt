package xyz.acrylicstyle.fap

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ProxyReloadEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import net.md_5.bungee.event.EventHandler
import util.Collection
import util.CollectionList
import util.ICollectionList
import xyz.acrylicstyle.fap.commands.FAPCommand
import xyz.acrylicstyle.fap.commands.FriendCommand
import xyz.acrylicstyle.fap.commands.FriendListCommand
import xyz.acrylicstyle.fap.commands.PartyChatCommand
import xyz.acrylicstyle.fap.commands.PartyCommand
import xyz.acrylicstyle.fap.commands.PartyListCommand
import xyz.acrylicstyle.fap.commands.PrivacyCommand
import xyz.acrylicstyle.fap.commands.ReplyCommand
import xyz.acrylicstyle.fap.commands.TellCommand
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.locale.Locale_en_US
import xyz.acrylicstyle.fap.locale.Locale_ja_JP
import java.io.File
import java.util.logging.Logger

class FAP: Plugin(), Listener {
    init {
        instance = this
    }

    override fun onEnable() {
        log = this.logger
        Locale_ja_JP
        Locale_en_US
        proxy.pluginManager.registerListener(this, this)
        config = ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File("./plugins/FAP/config.yml"))
        Locale.setLocale(config.getString("locale", "ja_JP"))
        val host = config.getString("database.host") ?: throw IllegalArgumentException("database.host, name, username, password must be defined")
        val name = config.getString("database.name") ?: throw IllegalArgumentException("database.name, username, password must be defined")
        val username = config.getString("database.username") ?: throw IllegalArgumentException("database.username, password must be defined")
        val password = config.getString("database.password") ?: throw IllegalArgumentException("database.password must be defined")
        db = ConnectionHolder(host, name, username, password)
        db.connect()
        log.info("Removing all parties")
        db.players.removeAllParties().complete()
        log.info("Removed all parties")
        ICollectionList.asList(config.getStringList("noWarpServers")).map { s -> s.toLowerCase() }.unique().forEach { noWarpServers.add(it) }
        proxy.registerChannel("fap:prefix")
        proxy.pluginManager.registerListener(this, FAPChannelListener)
        proxy.pluginManager.registerCommand(this, PartyCommand())
        proxy.pluginManager.registerCommand(this, PartyChatCommand())
        proxy.pluginManager.registerCommand(this, PartyListCommand())
        proxy.pluginManager.registerCommand(this, FriendCommand())
        proxy.pluginManager.registerCommand(this, FriendListCommand())
        proxy.pluginManager.registerCommand(this, FAPCommand())
        proxy.pluginManager.registerCommand(this, TellCommand())
        proxy.pluginManager.registerCommand(this, ReplyCommand())
        proxy.pluginManager.registerCommand(this, PrivacyCommand())
    }

    override fun onDisable() {
        log.info("Removing all parties")
        db.players.removeAllParties().complete()
        log.info("Removed all parties")
    }

    @EventHandler
    fun onProxyReload(e: ProxyReloadEvent) {
        noWarpServers.clear()
        Locale.setLocale(config.getString("locale", "ja_JP"))
        ICollectionList.asList(config.getStringList("noWarpServers")).map { s -> s.toLowerCase() }.unique().forEach { noWarpServers.add(it) }
    }

    @EventHandler
    fun onPostLogin(e: PostLoginEvent) {
        if (!FriendCommand.tasks.containsKey(e.player.uniqueId)) FriendCommand.tasks.add(e.player.uniqueId, Collection())
        db.players.getPlayer(e.player.uniqueId).then { player ->
            player.updateName(e.player.name).complete()
        }.queue()
        db.friends.getOnlineFriends(e.player.uniqueId).then {
            it.forEach { player ->
                player.sendMessage(TextComponent("${ChatColor.AQUA}Friend > ${ChatColor.GRAY}[${ChatColor.GREEN}+${ChatColor.GRAY}] ${ChatColor.YELLOW}${e.player.name} joined."))
            }
        }.queue()
    }

    @EventHandler
    fun onPlayerDisconnect(e: PlayerDisconnectEvent) {
        PartyCommand.tasks.remove(e.player.uniqueId)?.let {
            it.task.run()
            it.cancel()
        }
        db.friends.getOnlineFriends(e.player.uniqueId).then {
            it.forEach { player ->
                player.sendMessage(TextComponent("${ChatColor.AQUA}Friend > ${ChatColor.GRAY}[${ChatColor.RED}-${ChatColor.GRAY}] ${ChatColor.YELLOW}${e.player.name} left."))
            }
        }.queue()
    }

    companion object {
        val noWarpServers = CollectionList<String>()
        lateinit var instance: FAP
        lateinit var log: Logger
        lateinit var config: Configuration
        lateinit var db: ConnectionHolder

        const val HEAVY_CHECK_MARK = '\u2714'
        const val HEAVY_X = '\u2716'
        const val CIRCLED_STAR = '\u272A'
        const val CIRCLE = '\u25CF'

        /* separators */
        private const val separator = "--------------------------------------------------"
        val blueSeparator = "${ChatColor.BLUE}${separator}"
        val goldSeparator = "${ChatColor.GOLD}${separator}"
    }
}
