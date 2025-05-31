package me.uncertawn.uFastTravel.listener

import me.uncertawn.uFastTravel.UFastTravel
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerGotDamagedListener(var plugin: UFastTravel ) : Listener {
    @EventHandler
    fun playerDamagedEvent(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (!plugin.playersMounted.contains(event.entity.uniqueId)) return
        if (event.damageSource.damageType == DamageType.IN_WALL)
            event.isCancelled = true
        else {
            if (plugin.playersMounted.contains(event.entity.uniqueId)) {
                plugin.playersMounted.get(event.entity.uniqueId)?.remove()
            }
        }
    }

    @EventHandler
    fun playerDisconnect(event: PlayerQuitEvent) {
        var player: Player = event.player
        plugin.entities.forEach { entity ->
            if (entity.passengers.contains(player)) {
                entity.remove()
                plugin.entities.remove(entity)
            }
        }
    }
}