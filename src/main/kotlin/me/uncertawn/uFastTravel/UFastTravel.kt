/*
TODO:
maybe Make existing commands make sense maybe add tab completion


/removepath [pathname]

sometimes the Path Follower does not start following path
maybe its bc the chunks are unloaded should check that out

save player that started traveling in the config file so that when they rejoin they get tp'd to the start without loosing money
 */
package me.uncertawn.uFastTravel

import com.ticxo.modelengine.api.ModelEngineAPI
import me.uncertawn.uFastTravel.command.CreatePathCommand
import me.uncertawn.uFastTravel.command.DebugPathCommand
import me.uncertawn.uFastTravel.command.EditPathCommand
import me.uncertawn.uFastTravel.command.ListPathsCommand
import me.uncertawn.uFastTravel.command.StartTravelCommand
import me.uncertawn.uFastTravel.command.SummonDisplayBlimp
import me.uncertawn.uFastTravel.command.TpToPointInPath
import me.uncertawn.uFastTravel.listener.PlayerGotDamagedListener
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class UFastTravel : JavaPlugin() {
    var playersMounted: HashMap<UUID, Entity> = HashMap()
    var entities: MutableList<Entity> = mutableListOf()
    var playerTravellingPath: HashMap<UUID, String> = HashMap()

    lateinit var modelEngineAPI: ModelEngineAPI

    var taskId = -1
    override fun onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            logger.info("Model Engine found!")
            modelEngineAPI = Bukkit.getPluginManager().getPlugin("ModelEngine") as ModelEngineAPI
        } else {
            logger.severe("Model Engine not found!")
        }

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            logger.info("Found models in ModelEngine: ${modelEngineAPI.modelRegistry.keys}")
        }, 40)

        // Plugin startup logic
        getCommand("createpath")?.setExecutor(CreatePathCommand(this))
        getCommand("editpath")?.setExecutor(EditPathCommand(this))
        getCommand("debugpath")?.setExecutor(DebugPathCommand(this))
        getCommand("starttravel")?.setExecutor(StartTravelCommand(this))
        getCommand("pathlist")?.setExecutor(ListPathsCommand(this))
        getCommand("tptopointinpath")?.setExecutor(TpToPointInPath(this))
        getCommand("displayblimp")?.setExecutor(SummonDisplayBlimp(this))

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
