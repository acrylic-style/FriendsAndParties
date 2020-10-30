package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command

class FriendListCommand: Command("friendlist", null, "fl") {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender is ProxiedPlayer) FriendCommand.doList(sender)
    }
}
