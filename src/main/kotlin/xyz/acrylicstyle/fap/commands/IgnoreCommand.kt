package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.CollectionList
import xyz.acrylicstyle.fap.struct.doFilter
import xyz.acrylicstyle.fap.struct.toComponent

class IgnoreCommand: Command("ignore", null), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return sender.sendMessage("${ChatColor.RED}no u".toComponent())
        if (args.isEmpty()) return sender.sendMessage("${ChatColor.RED}/ignore <add/remove/list>".toComponent())
    }

    private fun doAdd(sender: ProxiedPlayer, targetName: String) {
    }

    private val commands = CollectionList.of("add", "remove", "list")

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return commands
        if (args.size == 1) return commands.doFilter(args[0])
        if (args.size == 2) {
            if (args[0] == "add" || args[0] == "remove") {
                return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.doFilter(args[1])
            }
        }
        return emptyList()
    }
}
