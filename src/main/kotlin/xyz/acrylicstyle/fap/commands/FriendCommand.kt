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
import net.md_5.bungee.api.plugin.TabExecutor
import net.md_5.bungee.api.scheduler.ScheduledTask
import util.Collection
import util.CollectionList
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.Player
import xyz.acrylicstyle.fap.struct.doFilter
import xyz.acrylicstyle.fap.struct.getPlayers
import xyz.acrylicstyle.fap.struct.toComponent
import java.util.UUID
import java.util.concurrent.TimeUnit

class FriendCommand: Command("friend", null, "fr"/*, "f"*/), TabExecutor {
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
        sender.sendMessage("${ChatColor.AQUA}/fr <${Locale.getLocale(sender).player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale(sender).friendHelpAdd}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/fr add <${Locale.getLocale(sender).player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale(sender).friendHelpAdd}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/fr accept <${Locale.getLocale(sender).player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale(sender).friendHelpAccept}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/fr remove <${Locale.getLocale(sender).player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale(sender).friendHelpRemove}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/fr list ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale(sender).friendHelpList}".toComponent())
        sender.sendMessage(FAP.blueSeparator.toComponent())
    }

    private fun doAdd(sender: ProxiedPlayer, targetName: String) {
        val target = ProxyServer.getInstance().getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(Locale.getLocale(sender).noPlayer.toComponent(ChatColor.RED))
            return
        }
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(Locale.getLocale(sender).invalidArgs.toComponent(ChatColor.RED))
            return
        }
        if (tasks[target.uniqueId] == null) tasks[target.uniqueId] = Collection()
        if (tasks[sender.uniqueId] == null) tasks[sender.uniqueId] = Collection()
        if (tasks[sender.uniqueId]!!.containsKey(target.uniqueId)) {
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale(sender).alreadySentFR.toComponent(ChatColor.RED))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            return
        }
        if (tasks[target.uniqueId]!!.containsKey(sender.uniqueId)) {
            doAccept(sender, targetName)
            return
        }
        FAP.db.friends.isFriend(sender.uniqueId, target.uniqueId).then { isFriend ->
            if (isFriend) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale(sender).alreadyFriend.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            val targetPlayer = FAP.db.players.getPlayer(target.uniqueId).complete()
            val player = FAP.db.players.getPlayer(sender.uniqueId).complete()
            if (!targetPlayer.acceptingFriend && !player.admin)
                return@then sender.sendMessage(Locale.getLocale(sender).cantSendFriendRequest.toComponent(ChatColor.RED))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale(sender).sentFR.format("${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            target.sendMessage(FAP.blueSeparator.toComponent())
            target.sendMessage(Locale.getLocale(sender).receivedFR.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
            val text = TextComponent(ChatColor.AQUA.toString() + Locale.getLocale(sender).receivedFR2)
            text.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fr accept ${sender.name}")
            text.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("${ChatColor.YELLOW}${Locale.getLocale(sender).clickToAcceptFR}"))
            target.sendMessage(text)
            target.sendMessage(FAP.blueSeparator.toComponent())
            val task = ProxyServer.getInstance().scheduler.schedule(FAP.instance, {
                tasks[sender.uniqueId]!!.remove(target.uniqueId)
                if (sender.isConnected) {
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                    sender.sendMessage(Locale.getLocale(sender).friendInviteExpired.format("${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                }
                if (target.isConnected) {
                    target.sendMessage(FAP.blueSeparator.toComponent())
                    target.sendMessage(Locale.getLocale(sender).friendInviteExpiredReceiver.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    target.sendMessage(FAP.blueSeparator.toComponent())
                }
            }, 3, TimeUnit.MINUTES)
            tasks[sender.uniqueId]!![target.uniqueId] = task
        }.queue()
    }

    private fun doAccept(sender: ProxiedPlayer, targetName: String) {
        val target = ProxyServer.getInstance().getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(Locale.getLocale(sender).noPlayer.toComponent(ChatColor.RED))
            return
        }
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(Locale.getLocale(sender).invalidArgs.toComponent(ChatColor.RED))
            return
        }
        if (tasks[target.uniqueId] == null) {
            tasks[target.uniqueId] = Collection()
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale(sender).noFR.toComponent(ChatColor.RED))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            return
        }
        if (tasks[target.uniqueId]!![sender.uniqueId] == null) {
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale(sender).noFR.toComponent(ChatColor.RED))
            sender.sendMessage(FAP.blueSeparator.toComponent())
            return
        }
        val senderFullName = FAP.db.players.getPlayer(sender.uniqueId).complete().getFullName()
        val targetFullName = FAP.db.players.getPlayer(target.uniqueId).complete().getFullName()
        tasks[target.uniqueId]!!.remove(sender.uniqueId)?.cancel()
        FAP.db.friends.addFriend(sender.uniqueId, target.uniqueId).complete()
        target.sendMessage(FAP.blueSeparator.toComponent())
        target.sendMessage(Locale.getLocale(target).friendAccepted.format("${senderFullName}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
        target.sendMessage(FAP.blueSeparator.toComponent())
        sender.sendMessage(FAP.blueSeparator.toComponent())
        sender.sendMessage(Locale.getLocale(sender).friendAccepted.format("${targetFullName}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
        sender.sendMessage(FAP.blueSeparator.toComponent())
    }

    private fun doRemove(sender: ProxiedPlayer, targetName: String) {
        @Suppress("DEPRECATION")
        FAP.db.players.getPlayer(targetName).then { target ->
            if (target == null) {
                sender.sendMessage(Locale.getLocale(sender).noPlayer.toComponent(ChatColor.RED))
                return@then
            }
            if (sender.uniqueId == target.uuid) {
                sender.sendMessage(Locale.getLocale(sender).invalidArgs.toComponent(ChatColor.RED))
                return@then
            }
            if (sender.name == target.name) {
                sender.sendMessage(Locale.getLocale(sender).invalidArgs.toComponent(ChatColor.RED))
                return@then
            }
            val isFriend = FAP.db.friends.isFriend(sender.uniqueId, target.uuid).complete()
            if (!isFriend) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale(sender).notFriend.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            FAP.db.friends.removeFriend(sender.uniqueId, target.uuid).complete()
            sender.sendMessage(FAP.blueSeparator.toComponent())
            sender.sendMessage(Locale.getLocale(sender).removedFriend.format("${target.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
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

    private val commands = CollectionList.of("add", "remove", "list", "accept")

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return commands
        if (args.size == 1) return commands.doFilter(args[0])
        if (args.size == 2) {
            if (args[0] == "add" || args[0] == "remove" || args[0] == "accept") {
                return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.doFilter(args[1])
            }
        }
        return emptyList()
    }
}
