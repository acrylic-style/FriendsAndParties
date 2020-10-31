package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.CollectionList
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.doFilter
import xyz.acrylicstyle.fap.struct.toComponent

class FAPCommand: Command("fap"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            if (!player.admin) return@then sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
            if (args.isEmpty()) return@then sender.sendMessage("${ChatColor.RED}/fap <clearPlayerCache | resetParty | prefix | admin>".toComponent())
            when (args[0]) {
                "clearPlayerCache" -> doClearPlayerCache(sender)
                "resetParty" -> doResetParty(sender)
                "prefix" -> {
                    if (args.size < 3) return@then sender.sendMessage("${ChatColor.RED}/fap prefix <Player> <Prefix>".toComponent())
                    doSetPrefix(sender, args[1], args[2])
                }
                "admin" -> {
                    if (args.size < 3) return@then sender.sendMessage("${ChatColor.RED}/fap admin <Player> <true/false>".toComponent())
                    doSetAdmin(sender, args[1], args[2])
                }
                else -> sender.sendMessage("${ChatColor.RED}/fap <clearPlayerCache | resetParty | prefix | admin>".toComponent())
            }
        }.queue()
    }

    private fun doClearPlayerCache(sender: ProxiedPlayer) {
        FAP.db.players.clearCache()
        sender.sendMessage("${ChatColor.GREEN}Cleared players cache.".toComponent())
    }

    private fun doResetParty(sender: ProxiedPlayer) {
        FAP.db.players.removeAllParties().complete()
        sender.sendMessage("${ChatColor.GREEN}Removed all parties.".toComponent())
    }

    private fun doSetPrefix(sender: ProxiedPlayer, targetName: String, prefix: String) {
        @Suppress("DEPRECATION")
        FAP.db.players.getPlayer(targetName).then { player ->
            if (player == null) {
                sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
                return@then
            }
            player.prefix = if (prefix == "null") null else prefix
            player.update().complete()
            sender.sendMessage("${ChatColor.GREEN}Changed $targetName's prefix to: ${ChatColor.GRAY}${ChatColor.translateAlternateColorCodes('&', prefix)}".toComponent())
        }.queue()
    }

    private fun doSetAdmin(sender: ProxiedPlayer, targetName: String, newStatus: String) {
        val bool = newStatus.toBoolean()
        @Suppress("DEPRECATION")
        FAP.db.players.getPlayer(targetName).then { player ->
            if (player == null) {
                sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
                return@then
            }
            if (player.uuid == sender.uniqueId) {
                sender.sendMessage(Locale.getLocale().invalidArgs.toComponent(ChatColor.RED))
                return@then
            }
            player.admin = bool
            player.update().complete()
            sender.sendMessage("${ChatColor.GREEN}Changed $targetName's Admin status to $bool")
        }.queue()
    }

    private val commands = CollectionList.of("clearPlayerCache", "resetParty", "prefix", "admin")

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return commands
        if (args.size == 1) return commands.doFilter(args[0])
        if (args.size == 2) {
            if (args[0] == "prefix" || args[0] == "admin") {
                return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.doFilter(args[1])
            }
        }
        return emptyList()
    }
}
