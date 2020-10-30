package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command

class PartyListCommand: Command("partylist", null, "pl") { // replacing /pl in bukkit is intentional
    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (sender is ProxiedPlayer) PartyCommand.doList(sender)
    }
}
