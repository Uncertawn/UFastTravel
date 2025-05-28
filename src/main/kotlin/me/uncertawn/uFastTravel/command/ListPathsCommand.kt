package me.uncertawn.uFastTravel.command

import me.uncertawn.uFastTravel.data.DataManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class ListPathsCommand(var plugin: Plugin) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        var paths = ""
        var num: Int = 0
        DataManager(plugin).getTravelPaths().forEach {
            it -> paths += "$it, "
            num++
        }
        paths += "total: $num paths"
        sender.sendMessage(paths)

        return true
    }
}