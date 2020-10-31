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
import util.ICollectionList
import util.promise.Promise
import xyz.acrylicstyle.fap.FAP
import xyz.acrylicstyle.fap.locale.Locale
import xyz.acrylicstyle.fap.struct.Party
import xyz.acrylicstyle.fap.struct.Player
import xyz.acrylicstyle.fap.struct.broadcastMessage
import xyz.acrylicstyle.fap.struct.doFilter
import xyz.acrylicstyle.fap.struct.toComponent
import java.util.UUID
import java.util.concurrent.TimeUnit

class PartyCommand: Command("party", null, "p"), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (sender !is ProxiedPlayer) {
            sender.sendMessage(TextComponent("${ChatColor.RED}This command cannot be executed from console."))
            return
        }
        if (args.isEmpty()) {
            sendHelp(sender)
            return
        }
        when (args[0]) {
            "leave" -> doLeave(sender)
            "disband" -> doDisband(sender)
            "invite" -> {
                if (args.size == 1) {
                    sendHelp(sender)
                    return
                }
                doInvite(sender, args[1])
            }
            "list" -> doList(sender)
            "kick" -> {
                if (args.size == 1) return sendHelp(sender)
                doKick(sender, args[1], ICollectionList.asList(args).shiftChain().shiftChain().join(" "))
            }
            "accept" -> {
                if (args.size == 1) return
                doAccept(sender, args[1])
            }
            "promote" -> {
                if (args.size == 1) return sendHelp(sender)
                doPromote(sender, args[1])
            }
            "warp" -> doWarp(sender)
            "hijack" -> doHijack(sender)
            "join" -> {
                if (args.size == 1) return doInvite(sender, args[0])
                doJoin(sender, args[1])
            }
            else -> doInvite(sender, args[0])
        }
    }

    private fun sendHelp(sender: ProxiedPlayer) {
        sender.sendMessage(FAP.blueSeparator.toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpInvite}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p invite <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpInvite}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p leave ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpLeave}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p list ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpList}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p disband ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpDisband}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p kick <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpKick}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p promote <${Locale.getLocale().player}> ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpPromote}".toComponent())
        sender.sendMessage("${ChatColor.AQUA}/p warp ${ChatColor.GRAY}- ${ChatColor.GREEN}${Locale.getLocale().partyHelpWarp}".toComponent())
        sender.sendMessage(FAP.blueSeparator.toComponent())
    }

    private fun doHijack(sender: ProxiedPlayer) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            if (!player.admin) {
                return@then sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
            }
            val party = FAP.db.party.getParty(player.uuid).complete()
                ?: return@then sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
            party.leader = player
            party.update().queue()
            party.getOnlineMembers().then {
                it.broadcastMessage(FAP.goldSeparator.toComponent())
                it.broadcastMessage(Locale.getLocale().partyHijacked.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                it.broadcastMessage(FAP.goldSeparator.toComponent())
            }.queue()
        }.queue()
    }

    private fun doPromote(sender: ProxiedPlayer, targetName: String) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            val party = FAP.db.party.getParty(player.uuid).complete()
            if (party == null) {
                sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                return@then
            }
            val target = ProxyServer.getInstance().getPlayer(targetName)
            if (target == null) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().offlinePlayer.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            val targetPlayer = FAP.db.players.getPlayer(target.uniqueId).complete()
            if (targetPlayer.uuid == sender.uniqueId) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().invalidArgs.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            if (party.leader.uuid != sender.uniqueId) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            party.leader = targetPlayer
            party.update().queue()
            party.getOnlineMembers().then {
                it.broadcastMessage(FAP.goldSeparator.toComponent())
                it.broadcastMessage(Locale.getLocale().partyPromoted.format("${player.getFullName()}${ChatColor.YELLOW}", "${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                it.broadcastMessage(FAP.goldSeparator.toComponent())
            }.queue()
        }.queue()
    }

    private fun doLeave(sender: ProxiedPlayer) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            val party = FAP.db.party.getParty(sender.uniqueId).complete()
            if (party == null) {
                sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                return@then
            }
            if (party.leader.uuid == sender.uniqueId) {
                doDisband(player, party)
            } else {
                player.party = null
                player.update().queue()
                party.getOnlineMembers().then { players ->
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                    sender.sendMessage(Locale.getLocale().leftParty.toComponent(ChatColor.YELLOW))
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                    players.remove(sender)
                    players.broadcastMessage(FAP.blueSeparator.toComponent())
                    players.broadcastMessage(Locale.getLocale().someoneLeftParty.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    players.broadcastMessage(FAP.blueSeparator.toComponent())
                }.queue()
            }
        }.queue()
    }

    private fun doKick(sender: ProxiedPlayer, targetName: String, reason: String) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            val party = FAP.db.party.getParty(sender.uniqueId).complete()
            if (party == null) {
                sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                return@then
            }
            if (party.leader.uuid != sender.uniqueId) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            @Suppress("DEPRECATION") val target = FAP.db.players.getPlayer(targetName).complete()
            if (target == null) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            if (party.id != target.party) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().notInThisParty.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            target.party = null
            target.update().queue()
            ProxyServer.getInstance().getPlayer(target.uuid)?.let {
                it.sendMessage(FAP.goldSeparator.toComponent())
                it.sendMessage(Locale.getLocale().kickedFromParty.format("${ChatColor.WHITE}$reason").toComponent(ChatColor.YELLOW))
                it.sendMessage(FAP.goldSeparator.toComponent())
            }
            party.getOnlineMembers().then { players ->
                sender.sendMessage(FAP.goldSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().kickedParty.format("${target.getFullName()}${ChatColor.YELLOW}", "${ChatColor.WHITE}$reason").toComponent(ChatColor.YELLOW))
                sender.sendMessage(FAP.goldSeparator.toComponent())
                players.remove(sender)
                players.broadcastMessage(FAP.goldSeparator.toComponent())
                players.broadcastMessage(Locale.getLocale().someoneLeftParty.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                players.broadcastMessage(FAP.goldSeparator.toComponent())
            }.queue()
        }.queue()
    }

    private fun doDisband(sender: ProxiedPlayer) {
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            val party = FAP.db.party.getParty(sender.uniqueId).complete()
            if (party == null) {
                sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                return@then
            }
            if (party.leader.uuid != sender.uniqueId) {
                sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
                return@then
            }
            doDisband(player, party)
        }.queue()
    }

    private fun doDisband(player: Player, party: Party) {
        party.getOnlineMembers().then { players ->
            players.broadcastMessage(FAP.blueSeparator.toComponent())
            players.broadcastMessage(Locale.getLocale().partyDisbanded.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
            players.broadcastMessage(FAP.blueSeparator.toComponent())
            FAP.db.players.getPendingPartyPlayers(party.id).then { players1 ->
                players1.forEach { p ->
                    tasks.remove(p.uuid)?.cancel()
                }
            }.queue()
            FAP.db.party.disbandParty(party.id).queue()
        }.queue()
    }

    private fun doAccept(sender: ProxiedPlayer, partyIdString: String) {
        val partyId = partyIdString.toIntOrNull()
        if (partyId == null) {
            sender.sendMessage("${ChatColor.RED}${Locale.getLocale().invalidArgs}".toComponent())
            return
        }
        FAP.db.party.getParty(partyId).then { party ->
            if (party == null) {
                sender.sendMessage("${ChatColor.RED}${Locale.getLocale().invalidArgs}".toComponent())
                return@then
            }
            val player = FAP.db.players.getPlayer(sender.uniqueId).complete()
            if (player.invitedParty == null || party.id != player.invitedParty) {
                sender.sendMessage("${ChatColor.RED}${Locale.getLocale().invalidArgs}".toComponent())
                return@then
            }
            player.party = player.invitedParty // accept
            player.invitedParty = null
            player.update().queue()
            tasks.remove(player.uuid)?.cancel()
            party.getOnlineMembers().then { players ->
                players.remove(sender)
                players.broadcastMessage(FAP.blueSeparator.toComponent())
                players.broadcastMessage(Locale.getLocale().someoneJoinedParty.format("${player.getFullName()}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
                players.broadcastMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().joinedParty.format("${party.leader.getFullName()}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
                sender.sendMessage(FAP.blueSeparator.toComponent())
            }.queue()
        }.queue()
    }

    private fun doWarp(sender: ProxiedPlayer) {
        FAP.db.party.getParty(sender.uniqueId).then { party ->
            if (party == null) {
                sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                return@then
            }
            if (party.leader.uuid != sender.uniqueId) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            val info = sender.server.info
            val p = party.leader
            if (FAP.noWarpServers.contains(info.name.toLowerCase()) && !p.admin) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().cantWarp.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            sender.sendMessage(Locale.getLocale().partyWarping.toComponent(ChatColor.GREEN))
            val sameServer = CollectionList<String>()
            val warped = CollectionList<String>()
            val failed = CollectionList<String>()
            party.getOnlineMembers().then { list ->
                list.forEach { player ->
                    if (info.name == player.server.info.name) {
                        sameServer.add(FAP.db.players.getPlayer(player.uniqueId).complete().getFullName())
                    }
                    player.connect(info)
                    player.sendMessage(FAP.blueSeparator.toComponent())
                    player.sendMessage(Locale.getLocale().partySummoned.format("${p.getFullName()}${ChatColor.GOLD}").toComponent(ChatColor.GOLD))
                    player.sendMessage(FAP.blueSeparator.toComponent())
                }
                Promise.sleepAsync(3000).then {
                    list.forEach { player ->
                        if (!player.isConnected) return@forEach
                        val pl = FAP.db.players.getPlayer(player.uniqueId).complete()
                        if (sameServer.contains(pl.getFullName())) return@forEach
                        if (info.name == player.server.info.name) {
                            warped.add(pl.getFullName())
                        } else {
                            failed.add(pl.getFullName())
                        }
                    }
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                    sender.sendMessage(Locale.getLocale().warpResult.toComponent(ChatColor.GREEN))
                    sender.sendMessage(Locale.getLocale().partyAlreadyInServer.format("${ChatColor.YELLOW}${sameServer.size}${ChatColor.GRAY}", sameServer.join("${ChatColor.GRAY}, ")).toComponent())
                    sender.sendMessage(Locale.getLocale().partyWarped.format("${ChatColor.YELLOW}${warped.size}${ChatColor.GRAY}", warped.join("${ChatColor.GRAY}, ")).toComponent())
                    sender.sendMessage(Locale.getLocale().partyWarpFailed.format("${ChatColor.YELLOW}${failed.size}${ChatColor.GRAY}", failed.join("${ChatColor.GRAY}, ")).toComponent())
                    sender.sendMessage(FAP.blueSeparator.toComponent())
                }.queue()
            }.queue()
        }.queue()
    }

    private fun doJoin(sender: ProxiedPlayer, targetName: String) {
        val target = ProxyServer.getInstance().getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(Locale.getLocale().noPlayer.toComponent(ChatColor.RED))
            return
        }
        FAP.db.players.getPlayer(sender.uniqueId).then { player ->
            if (!player.admin) return@then sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
            val party = FAP.db.party.getParty(target.uniqueId).complete()
            if (party == null) {
                sender.sendMessage("${ChatColor.RED}This player is not in the party.".toComponent())
                return@then
            }
            player.party = party.id
            player.update().queue()
            party.getOnlineMembers().then { players ->
                players.remove(sender)
                players.broadcastMessage(FAP.blueSeparator.toComponent())
                players.broadcastMessage(Locale.getLocale().someoneJoinedParty.format("${player.getFullName()}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
                players.broadcastMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().joinedParty.format("${party.leader.getFullName()}${ChatColor.GREEN}").toComponent(ChatColor.GREEN))
                sender.sendMessage(FAP.blueSeparator.toComponent())
            }.queue()
        }.queue()
    }

    private fun doInvite(sender: ProxiedPlayer, targetName: String) {
        val target = ProxyServer.getInstance().getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(TextComponent("${ChatColor.RED}${Locale.getLocale().noPlayer}"))
            return
        }
        FAP.db.party.getParty(sender.uniqueId).then { party2 ->
            val player = FAP.db.players.getPlayer(sender.uniqueId).complete()
            var party: Party? = party2
            if (party2 == null) {
                party = FAP.db.party.createParty(player).complete()
                player.party = party.id
                player.update().complete()
            }
            if (party == null) throw AssertionError("Should not be null")
            if (party.leader.uuid != sender.uniqueId) {
                sender.sendMessage(Locale.getLocale().noPermission.toComponent(ChatColor.RED))
                return@then
            }
            val targetPlayer = FAP.db.players.getPlayer(target.uniqueId).complete()
            if (targetPlayer.party != null || targetPlayer.invitedParty != null) {
                sender.sendMessage(Locale.getLocale().inOtherParty.toComponent(ChatColor.RED))
                return@then
            }
            if (party.id == targetPlayer.party || party.id == targetPlayer.invitedParty) {
                sender.sendMessage(FAP.blueSeparator.toComponent())
                sender.sendMessage(Locale.getLocale().alreadyInParty.toComponent(ChatColor.RED))
                sender.sendMessage(FAP.blueSeparator.toComponent())
                return@then
            }
            targetPlayer.invitedParty = party.id
            targetPlayer.update().queue()
            party.getOnlineMembers().then { list ->
                list.broadcastMessage(FAP.blueSeparator.toComponent())
                list.broadcastMessage(Locale.getLocale().invited.format("${player.getFullName()}${ChatColor.YELLOW}", "${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                list.broadcastMessage(FAP.blueSeparator.toComponent())
            }.queue()
            target.sendMessage(FAP.blueSeparator.toComponent())
            target.sendMessage(Locale.getLocale().invitedParty.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
            val text = TextComponent(ChatColor.AQUA.toString() + Locale.getLocale().invitedParty2)
            text.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/p accept ${party.id}")
            text.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("${ChatColor.YELLOW}${Locale.getLocale().clickToJoinParty}"))
            target.sendMessage(text)
            target.sendMessage(FAP.blueSeparator.toComponent())
            val task = ProxyServer.getInstance().scheduler.schedule(FAP.instance, {
                tasks.remove(targetPlayer.uuid)
                targetPlayer.invitedParty = null
                targetPlayer.update()
                if (target.isConnected) {
                    target.sendMessage(FAP.blueSeparator.toComponent())
                    target.sendMessage(Locale.getLocale().partyInviteExpiredReceiver.format("${player.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    target.sendMessage(FAP.blueSeparator.toComponent())
                }
                party.getOnlineMembers().then { list ->
                    list.broadcastMessage(FAP.blueSeparator.toComponent())
                    list.broadcastMessage(Locale.getLocale().partyInviteExpired.format("${targetPlayer.getFullName()}${ChatColor.YELLOW}").toComponent(ChatColor.YELLOW))
                    list.broadcastMessage(FAP.blueSeparator.toComponent())
                }.queue()
            }, 1L, TimeUnit.MINUTES)
            tasks.add(targetPlayer.uuid, task)
        }.queue()
    }

    companion object {
        val tasks = Collection<UUID, ScheduledTask>()

        fun doList(sender: ProxiedPlayer) {
            FAP.db.party.getParty(sender.uniqueId).then { party ->
                if (party == null) {
                    sender.sendMessage(Locale.getLocale().notInParty.toComponent(ChatColor.RED))
                    return@then
                }
                sender.sendMessage("----- ${Locale.getLocale().partyLeader} -----".toComponent(ChatColor.GREEN))
                sender.sendMessage(name(party.leader).toComponent())
                sender.sendMessage("----- ${Locale.getLocale().partyMember} -----".toComponent(ChatColor.GREEN))
                var members = ""
                party.getMembers().complete().forEach { player ->
                    if (player.uuid != party.leader.uuid) members += name(player)
                }
                sender.sendMessage(members.toComponent())
            }.queue()
        }

        private fun name(player: Player) = "  ${if (player.isOnline()) ChatColor.GREEN else ChatColor.RED}${FAP.CIRCLE} ${player.getFullName()}"
    }

    private val commands = CollectionList.of("invite", "disband", "leave", "kick")

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (sender !is ProxiedPlayer) return emptyList()
        if (args.isEmpty()) return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.concat(commands)
        if (args.size == 1) return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.concat(commands).doFilter(args[0])
        if (args.size == 2) {
            if (args[0] == "invite"
                || args[0] == "kick") return CollectionList(ProxyServer.getInstance().players).map { p -> p.name }.doFilter(args[1])
        }
        return emptyList()
    }
}
