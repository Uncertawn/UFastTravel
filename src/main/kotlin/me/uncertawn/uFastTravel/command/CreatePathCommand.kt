package me.uncertawn.uFastTravel.command

import me.uncertawn.uFastTravel.data.DataManager
import me.uncertawn.uFastTravel.data.TravelData
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class CreatePathCommand(val plugin: Plugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command!")
            return false
        }
        try {
            sender.sendMessage(label)
            sender.sendMessage("Saving ${args[0]}")
            var travelData = TravelData(args[0], sender.world.name)
            DataManager(plugin).saveTravelPath(travelData)
            sender.sendMessage("Should be done")
            return true
        } catch (_: Exception) {
            sender.sendMessage(command.usage)
            return true
        }
    }
}