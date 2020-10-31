@file:Suppress("DuplicatedCode")

package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import util.CollectionList
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.doFilter
import xyz.acrylicstyle.fap.struct.toComponent

class PrivacyCommand: Command("privacy"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return
        if (args.isEmpty()) return sender.sendMessage("${ChatColor.RED}/privacy <party | friend | message>".toComponent())
        when (args[0]) {
            "party" -> doToggleParty(sender)
            "friend" -> doToggleFriend(sender)
            "message" -> doToggleMessage(sender)
            else -> return sender.sendMessage("${ChatColor.RED}/privacy <party | friend | message>".toComponent())
        }
    }

    private fun doToggleParty(sender: ProxiedPlayer) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            player.acceptingParty = !player.acceptingParty
            player.update().queue()
            if (player.acceptingParty) {
                sender.sendMessage(Locale.getLocale().privacyPartyTurnedOn.toComponent(ChatColor.GREEN))
            } else {
                sender.sendMessage(Locale.getLocale().privacyPartyTurnedOff.toComponent(ChatColor.GREEN))
            }
        }.queue()
    }

    private fun doToggleFriend(sender: ProxiedPlayer) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            player.acceptingFriend = !player.acceptingFriend
            player.update().queue()
            if (player.acceptingFriend) {
                sender.sendMessage(Locale.getLocale().privacyFriendTurnedOn.toComponent(ChatColor.GREEN))
            } else {
                sender.sendMessage(Locale.getLocale().privacyFriendTurnedOff.toComponent(ChatColor.GREEN))
            }
        }.queue()
    }

    private fun doToggleMessage(sender: ProxiedPlayer) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            player.acceptingMessage = !player.acceptingMessage
            player.update().queue()
            if (player.acceptingMessage) {
                sender.sendMessage(Locale.getLocale().privacyMessageTurnedOn.toComponent(ChatColor.GREEN))
            } else {
                sender.sendMessage(Locale.getLocale().privacyMessageTurnedOff.toComponent(ChatColor.GREEN))
            }
        }.queue()
    }

    private val commands = CollectionList.of("party", "friend", "message")

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return commands
        if (args.size == 1) return commands.doFilter(args[0])
        return emptyList()
    }
}
