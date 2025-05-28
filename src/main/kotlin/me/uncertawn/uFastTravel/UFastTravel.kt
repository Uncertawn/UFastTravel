/*
TODO:
Make existing commands make sense maybe add tab completion

rotate armor stand in the path direction for better third person view
/editpoint [path name] [index] [remove]
/removepath [pathname]
 */
package me.uncertawn.uFastTravel

import me.uncertawn.uFastTravel.command.CreatePathCommand
import me.uncertawn.uFastTravel.command.DebugPathCommand
import me.uncertawn.uFastTravel.command.EditPathCommand
import me.uncertawn.uFastTravel.command.ListPathsCommand
import me.uncertawn.uFastTravel.command.StartTravelCommand
import me.uncertawn.uFastTravel.command.TpToPointInPath
import me.uncertawn.uFastTravel.listener.PlayerGotDamagedListener
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class UFastTravel : JavaPlugin() {
    var playersMounted: HashMap<UUID, Entity> = HashMap()
    var entities: MutableList<Entity> = mutableListOf()

    var taskId = -1
    override fun onEnable() {
        // Plugin startup logic
        getCommand("createpath")?.setExecutor(CreatePathCommand(this))
        getCommand("editpath")?.setExecutor(EditPathCommand(this))
        getCommand("debugpath")?.setExecutor(DebugPathCommand(this))
        getCommand("starttravel")?.setExecutor(StartTravelCommand(this))
        getCommand("pathlist")?.setExecutor(ListPathsCommand(this))
        getCommand("tptopointinpath")?.setExecutor(TpToPointInPath(this))

        server.pluginManager.registerEvents(PlayerGotDamagedListener(this), this)

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            entities.forEachIndexed { index, entity -> {
                logger.info("Found non-mounted entity ${entity.name} at ${entity.location}")
                if (entity.passengers.isEmpty()) {
                    entity.remove()
                    entities.removeAt(index)
                    logger.info("Removed non-mounted entity ${entity.name} at ${entity.location}")
                }
            } }
        }, 0L, 20L*30).taskId
    }

    override fun onDisable() {
        // Plugin shutdown logic
        Bukkit.getScheduler().cancelTask(taskId)
    }
}
