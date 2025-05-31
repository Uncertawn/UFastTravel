package me.uncertawn.uFastTravel.npc

import com.ticxo.modelengine.api.ModelEngineAPI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand

class BlimpDisplay {
    fun summon(location: Location) {
        var entity: ArmorStand = location.world.spawn(location, ArmorStand::class.java).apply {
            customName(Component.text("BlimpDisplay"))
            isCustomNameVisible = false
            isInvulnerable = true
            isVisible = false
            isMarker = false
            isCollidable = false
            setGravity(false)
        }

        var modeledEntity = ModelEngineAPI.createModeledEntity(entity)
        var activeModel = ModelEngineAPI.createActiveModel("blimpppppp")
        modeledEntity.addModel(activeModel, true)
    }
}