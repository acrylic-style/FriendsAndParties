package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import util.ICollectionList
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.playSound
import xyz.acrylicstyle.fap.struct.toComponent

class ReplyCommand: Command("reply", null, "r") {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return
        if (args.isEmpty()) return sender.sendMessage("${ChatColor.RED}/r <Message...>".toComponent())
        val message = ICollectionList.asList(args).join(" ")
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            val lastUUID = player.lastMessageFrom ?: return@then sender.sendMessage(Locale.getLocale().invalidArgs.toComponent(ChatColor.RED))
            val targetPlayer = ProxyServer.getInstance().getPlayer(lastUUID) ?: return@then sender.sendMessage(Locale.getLocale().offlinePlayer.toComponent(ChatColor.RED))
            val target = FAP.db.players.getPlayer(lastUUID).complete()
            sender.sendMessage("${ChatColor.LIGHT_PURPLE}To ${target.getFullName()}: ${ChatColor.WHITE}${message}".toComponent())
            targetPlayer.sendMessage("${ChatColor.LIGHT_PURPLE}From ${player.getFullName()}: ${ChatColor.WHITE}${message}".toComponent())
            sender.playSound("ORB_PICKUP", 1F, 1F)
            sender.playSound("ENTITY_EXPERIENCE_ORB_PICKUP", 1F, 1F)
            targetPlayer.playSound("ORB_PICKUP", 1F, 1F)
            targetPlayer.playSound("ENTITY_EXPERIENCE_ORB_PICKUP", 1F, 1F)
            target.lastMessageFrom = sender.uniqueId
            target.update().queue()
        }.queue()
    }
}
