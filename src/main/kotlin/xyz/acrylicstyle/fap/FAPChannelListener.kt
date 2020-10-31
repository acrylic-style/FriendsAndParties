package xyz.acrylicstyle.fap

import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.UUID

object FAPChannelListener: Listener {
    @EventHandler
    fun onPluginMessage(e: PluginMessageEvent) {
        val tag = e.tag
        if (tag == "fap:prefix") {
            val input = DataInputStream(ByteArrayInputStream(e.data))
            val subchannel = input.readUTF()
            val msg = input.readUTF()
            val uuid = try {
                UUID.fromString(subchannel)
            } catch (e: IllegalArgumentException) {
                return
            }
            FAP.db.players.getPlayer(uuid).then { player ->
                player.prefix = if (msg == "null") null else msg
                player.update().complete()
            }.queue()
        }
    }
}
