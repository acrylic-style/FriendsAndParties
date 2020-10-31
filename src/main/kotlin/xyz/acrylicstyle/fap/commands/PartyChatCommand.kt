package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import util.ICollectionList
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.Party
import xyz.acrylicstyle.fap.struct.Player
import xyz.acrylicstyle.fap.struct.broadcastMessage
import xyz.acrylicstyle.fap.struct.playSound
import xyz.acrylicstyle.fap.struct.toComponent

class PartyChatCommand: Command("partychat", null, "pc") {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return sender.sendMessage("no u".toComponent())
        Promise.all(FAP.db.players.getPlayer(sender.uniqueId), FAP.db.party.getParty(sender.uniqueId)).then { promises ->
            val player = promises[0] as Player
            val party = promises[1] as Party?
            if (party == null) {
                sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                return@then
            }
            val message = ICollectionList.asList(args).join(" ")
            party.getOnlineMembers().then {
                it.broadcastMessage("${ChatColor.BLUE}Party > ${player.getFullName()}${ChatColor.GRAY}: ${ChatColor.WHITE}${message}".toComponent())
                it.forEach { p ->
                    p.playSound("ORB_PICKUP", 1F, 1F)
                    p.playSound("ENTITY_EXPERIENCE_ORB_PICKUP", 1F, 1F)
                }
            }.queue()
        }.queue()
    }
}
