package me.uncertawn.uFastTravel.command

import me.uncertawn.uFastTravel.UFastTravel
import me.uncertawn.uFastTravel.data.DataManager
import me.uncertawn.uFastTravel.data.TravelData
import me.uncertawn.uFastTravel.travelpath.Traveler
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StartTravelCommand(var plugin: UFastTravel) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        var player: Player? = Bukkit.getPlayer(args[0])
        if (args.size>3 || args.size < 2) {
            sender.sendMessage(command.usage)
            return false
        }
        if (player == null) {
            sender.sendMessage("Player ${args[0]} was not found!")
            return false
        }
        var data: TravelData? = DataManager(plugin).loadTravelPath(args[1])
        if(data != null) {
            var travelData: TravelData = data

            if (args.size==3) {
                if (args[2].toBoolean()==true)
                    travelData.points.reverse()
                    Traveler(plugin, player, travelData).start(true)
            }
            else
                Traveler(plugin, player, travelData).start(false)
        }
        else
            sender.sendMessage("No such path ${args[1]}")

        return true
    }
}