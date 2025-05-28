package me.uncertawn.uFastTravel.command

import me.uncertawn.uFastTravel.data.DataManager
import me.uncertawn.uFastTravel.data.TravelData
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class TpToPointInPath(val plugin: Plugin) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return false

        var travelData: TravelData? = null
        travelData = DataManager(plugin).loadTravelPath(args[0])
        if (travelData != null)
            sender.teleport(travelData.points[args[1].toInt()].toLocation(sender.world))

        return true
    }
}