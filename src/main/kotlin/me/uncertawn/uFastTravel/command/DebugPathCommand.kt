package me.uncertawn.uFastTravel.command

import me.uncertawn.uFastTravel.data.DataManager
import me.uncertawn.uFastTravel.data.TravelData
import me.uncertawn.uFastTravel.travelpath.DebugPath
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class DebugPathCommand(val plugin: Plugin) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return false

        var travelData: TravelData? = null
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            travelData = DataManager(plugin).loadTravelPath(args[0])
            if (travelData != null) {
                sender.sendMessage(travelData!!.world)
                travelData!!.points.forEachIndexed { index, vector ->
                    sender.sendMessage("$index:\nx: ${vector.x} y: ${vector.y} z: ${vector.z}")
                }
                if (sender.world.name.equals(travelData!!.world))
                    DebugPath(plugin, sender, 60, travelData!!).start()
            }
        })

        return true
    }
}