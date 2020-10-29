package xyz.acrylicstyle.fap

import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import net.md_5.bungee.event.EventHandler
import java.util.logging.Logger

class FAP: Plugin(), Listener {
    init {
        instance = this
    }

    override fun onEnable() {
        log = this.logger
        proxy.pluginManager.registerListener(this, this)
        config = ConfigurationProvider.getProvider(YamlConfiguration::class.java).load("./plugins/FAP/config.yml")
        val host = config.getString("database.host") ?: throw IllegalArgumentException("database.host, name, username, password must be defined")
        val name = config.getString("database.name") ?: throw IllegalArgumentException("database.name, username, password must be defined")
        val username = config.getString("database.username") ?: throw IllegalArgumentException("database.username, password must be defined")
        val password = config.getString("database.password") ?: throw IllegalArgumentException("database.password must be defined")
        db = ConnectionHolder(host, name, username, password)
    }

    @EventHandler
    fun onPlayerDisconnect(e: PlayerDisconnectEvent) {
    }

    companion object {
        lateinit var instance: FAP
        lateinit var log: Logger
        lateinit var config: Configuration
        lateinit var db: ConnectionHolder
    }
}
