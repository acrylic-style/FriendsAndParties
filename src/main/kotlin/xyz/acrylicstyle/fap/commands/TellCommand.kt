package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.CollectionList
import util.ICollectionList
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.doFilter
import xyz.acrylicstyle.fap.struct.playSound
import xyz.acrylicstyle.fap.struct.toComponent

class TellCommand: Command("tell", null, "w", "msg", "message"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return
        if (args.size < 2) {
            return sender.sendMessage("${ChatColor.RED}/tell <Player> <Message...>".toComponent())
        }
        doSend(sender, args[0], ICollectionList.asList(args).shiftChain().join(" "))
    }

    private fun doSend(sender: ProxiedPlayer, targetName: String, message: String) {
        val targetPlayer = ProxyServer.getInstance().getPlayer(targetName)
        if (targetPlayer == null) {
            sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
            return
        }
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            val target = FAP.db.players.getPlayer(targetPlayer.uniqueId).complete()
            if (!target.acceptingMessage && !player.admin)
                return@then sender.sendMessage(Locale.getLocale().cantSendMessage.toComponent(ChatColor.RED))
            sender.sendMessage("${ChatColor.LIGHT_PURPLE}To ${target.getFullName()}${ChatColor.GREEN}: ${ChatColor.WHITE}${message}".toComponent())
            targetPlayer.sendMessage("${ChatColor.LIGHT_PURPLE}From ${player.getFullName()}${ChatColor.GREEN}: ${ChatColor.WHITE}${message}".toComponent())
            sender.playSound("ORB_PICKUP", 1F, 1F)
            sender.playSound("ENTITY_EXPERIENCE_ORB_PICKUP", 1F, 1F)
            targetPlayer.playSound("ORB_PICKUP", 1F, 1F)
            targetPlayer.playSound("ENTITY_EXPERIENCE_ORB_PICKUP", 1F, 1F)
            target.lastMessageFrom = sender.uniqueId
            target.update().queue()
        }.queue()
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }
        if (args.size == 1) return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.doFilter(args[0])
        return emptyList()
    }
}
