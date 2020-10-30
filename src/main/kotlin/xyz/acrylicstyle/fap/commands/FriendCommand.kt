package xyz.acrylicstyle.fap.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.scheduler.ScheduledTask
import util.Collection
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.Player
import xyz.acrylicstyle.fap.struct.getPlayers
import xyz.acrylicstyle.fap.struct.toComponent
import java.util.UUID
import java.util.concurrent.TimeUnit

class FriendCommand: Command("friend", null, "f") {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) return sender.sendMessage("Bye bye :)".toComponent())
        if (args.isEmpty()) return sendHelp(sender)
        when (args[0]) {
            "add" -> {
                if (args.size == 1) return sendHelp(sender)
                doAdd(sender, args[1])
            }
            "accept" -> {
                if (args.size == 1) return sendHelp(sender)
                doAccept(sender, args[1])
            }
            "remove" -> {
                if (args.size == 1) return sendHelp(sender)
                doRemove(sender, args[1])
            }
            "list" -> doList(sender)
            else -> doAdd(sender, args[0])
        }
    }

    private fun sendHelp(sender: ProxiedPlayer) {
        sender.sendMessage(FAP.blueSeparator.toComponent())
        sender.sendMessage("${ChatColor.AQUA}/f <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().friendHelpAdd}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/f add <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().friendHelpAdd}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/f accept <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().friendHelpAccept}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/f remove <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().friendHelpRemove}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/f list ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().friendHelpList}".toComponent())
        sender.sendMessage(FAP.blueSeparator.toComponent())
    }

    private fun doAdd(sender: ProxiedPlayer, targetName: String) {
        val target = ProxyServer.getInstance().getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
            return
        }
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(Locale.getLocale().invalidArgs.toComponent(ChatColor.RED))
            return
        }
        if (tasks[target.uniqueId] == null) tasks[target.uniqueId] = Collection()
        if (tasks[target.uniqueId]!!.containsKey(sender.uniqueId)) {
            doAccept(sender, targetName)
            return
        }
        FAP.db.friends.isFriend(sender.uniqueId, target.uniqueId).then { isFriend ->
            if (isFriend) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().alreadyFriend.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            val targetPlayer = FAP.db.players.getPlayer(target.uniqueId).complete()
            val player = FAP.db.players.getPlayer(sender.uniqueId).complete()
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale().sentFR.format("${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            target.sendMessage(FAP.blueSeparator.toComponent())
            target.sendMessage(Locale.getLocale().receivedFR.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
            val text = TextComponent(ChatColor.AQUA.toString() + Locale.getLocale().receivedFR2)
            text.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f accept ${sender.name}")
            text.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("${ChatColor.YELLOW}${Locale.getLocale().clickToAcceptFR}"))
            target.sendMessage(text)
            target.sendMessage(FAP.blueSeparator.toComponent())
            val task = ProxyServer.getInstance().scheduler.schedule(FAP.instance, {
                tasks[sender.uniqueId]!!.remove(target.uniqueId)
                if (sender.isConnected) {
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                    sender.sendMessage(Locale.getLocale().friendInviteExpired.format("${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                }
                if (target.isConnected) {
                    target.sendMessage(FAP.blueSeparator.toComponent())
                    target.sendMessage(Locale.getLocale().friendInviteExpiredReceiver.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    target.sendMessage(FAP.blueSeparator.toComponent())
                }
            }, 3, TimeUnit.MINUTES)
            if (tasks[sender.uniqueId] == null) tasks[sender.uniqueId] = Collection()
            tasks[sender.uniqueId]!![target.uniqueId] = task
        }.queue()
    }

    private fun doAccept(sender: ProxiedPlayer, targetName: String) {
        val target = ProxyServer.getInstance().getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
            return
        }
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(Locale.getLocale().invalidArgs.toComponent(ChatColor.RED))
            return
        }
        if (tasks[target.uniqueId] == null) {
            tasks[target.uniqueId] = Collection()
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale().noFR.toComponent(ChatColor.RED))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            return
        }
        if (tasks[target.uniqueId]!![sender.uniqueId] == null) {
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale().noFR.toComponent(ChatColor.RED))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            return
        }
        val senderFullName = FAP.db.players.getPlayer(sender.uniqueId).complete().getFullName()
        val targetFullName = FAP.db.players.getPlayer(target.uniqueId).complete().getFullName()
        tasks[target.uniqueId]!!.remove(sender.uniqueId)?.cancel()
        FAP.db.friends.addFriend(sender.uniqueId, target.uniqueId).complete()
        target.sendMessage(FAP.blueSeparator.toComponent())
        target.sendMessage(Locale.getLocale().friendAccepted.format("${senderFullName}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
        target.sendMessage(FAP.blueSeparator.toComponent())
        sender.sendMessage(FAP.blueSeparator.toComponent())
        sender.sendMessage(Locale.getLocale().friendAccepted.format("${targetFullName}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
        sender.sendMessage(FAP.blueSeparator.toComponent())
    }

    private fun doRemove(sender: ProxiedPlayer, targetName: String) {
        @Suppress("DEPRECATION")
        FAP.db.players.getPlayer(targetName).then { target ->
            if (target == null) {
                sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
                return@then
            }
            if (sender.uniqueId == target.uuid) {
                sender.sendMessage(Locale.getLocale().invalidArgs.toComponent(ChatColor.RED))
                return@then
            }
            val isFriend = FAP.db.friends.isFriend(sender.uniqueId, target.uuid).complete()
            if (!isFriend) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().notFriend.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            val player = FAP.db.players.getPlayer(sender.uniqueId).complete()
            FAP.db.friends.removeFriend(sender.uniqueId, target.uuid).complete()
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale().removedFriend.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.GREEN))
            sender.sendMessage(FAP.blueSeparator.toComponent())
        }.queue()
    }

    companion object {
        val tasks = Collection<UUID, Collection<UUID, ScheduledTask>>()

        fun doList(sender: ProxiedPlayer) {
            FAP.db.players.getPlayer(sender.uniqueId).then { player ->
                player.fetchFriends().complete()
                val size = player.friends!!.size
                var friends = ""
                Promise.allTyped(*player.friends!!.getPlayers().toTypedArray()).complete().forEach { p ->
                    friends += name(p)
                }
                sender.sendMessage("${ChatColor.GREEN}Friends ($size):".toComponent())
                sender.sendMessage(friends.toComponent())
            }.queue()
        }

        private fun name(player: Player) = "  ${if (player.isOnline()) ChatColor.GREEN else ChatColor.RED}${FAP.CIRCLE} ${player.getFullName()}"
    }
}
