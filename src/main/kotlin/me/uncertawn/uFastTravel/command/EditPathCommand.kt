package me.uncertawn.uFastTravel.command

import me.uncertawn.uFastTravel.data.DataManager
import me.uncertawn.uFastTravel.data.TravelData
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

class EditPathCommand(val plugin: Plugin) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return false


        sender.sendMessage(args[2])
        var travelData: TravelData? = null
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            travelData = DataManager(plugin).loadTravelPath(args[0])
            if (travelData != null) {
                travelData?.let { sender.sendMessage("Found travel data ${it.message}") }

                if (args[1].equals("addpoint")) {
                    var resultVector = extractVector(args[2], args[3], args[4], sender)
                    sender.sendMessage("$resultVector")

                    var temp: MutableList<Vector> = travelData!!.points.toMutableList()
                    temp.add(resultVector)
                    travelData!!.points = temp.toTypedArray()
                }
                else if (args[1].equals("editpoint")) {
                    var resultVector = extractVector(args[3], args[4], args[5], sender)
                    sender.sendMessage("$resultVector")
                    travelData!!.points[args[2].toInt()] = resultVector
                }
                else if (args[1].equals("from") || args[1].equals("to")) {
                    var message = ""
                    args.forEachIndexed { index, it ->
                        if (index > 1) {
                            message += " $it"
                        }
                    }

                    if (args[1].equals("from"))
                        travelData!!.from = message.trim()
                    else
                        travelData!!.to = message.trim()
                }

                DataManager(plugin).saveTravelPath(travelData!!)
            }
        })

        return true
    }

    fun extractVector(x:String, y:String, z:String, sender: Player): Vector {
         var X = if (x.equals("~"))
            sender.location.x
        else
            x.toDouble()
        var Y = if (y.equals("~"))
            sender.location.y
        else
            y.toDouble()
        var Z = if (z.equals("~"))
            sender.location.z
        else
            z.toDouble()
        return Vector(X, Y, Z)
    }
}