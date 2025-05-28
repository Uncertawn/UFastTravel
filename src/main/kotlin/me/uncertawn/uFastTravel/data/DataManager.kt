package me.uncertawn.uFastTravel.data

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.io.File

class DataManager(var plugin: Plugin) {
    fun saveTravelPath(travelData: TravelData) {
        Bukkit.getScheduler().runTaskAsynchronously( plugin, Runnable {
            try {

                val dataFile = File(plugin.dataFolder, "travelpaths/${travelData.name}.json")
                dataFile.parentFile.mkdirs()

                dataFile.writeText(Gson().toJson(travelData))
                plugin.logger.info(Gson().toJson(travelData).toString())
            } catch (e: Exception) {
                plugin.logger.severe("Failed to save data: ${e.message}")
            }
        })
    }

    fun loadTravelPath(name: String): TravelData? {
        val dataFolder by lazy { File(plugin.dataFolder, "travelpaths/") }
        if (!dataFolder.exists()) {
            plugin.logger.severe("travelpaths folder does not exist! Making travelpaths folder in ${plugin.dataFolder}")
            dataFolder.mkdirs()
            return null
        }
        val file = File(dataFolder, "${name}.json")
        if (!file.exists()) {
            plugin.logger.severe("No such path ${name}!")
            return null
        }

        return try {
            val jsonString = file.readText()
            Gson().fromJson(jsonString, TravelData::class.java)
        } catch (e: Exception) {
            plugin.logger.severe("Something went wrong when trying to load path ${name}! ${e.message}")
            return null
        }
    }

    fun getTravelPaths(): Array<String> {
        val dataFolder by lazy { File(plugin.dataFolder, "travelpaths/") }
        if (!dataFolder.exists()) {
            plugin.logger.severe("travelpaths folder does not exist! Making travelpaths folder in ${plugin.dataFolder}")
            dataFolder.mkdirs()
            return arrayOf("")
        }
        var arra = mutableListOf<String>()
        dataFolder.list().forEach { it ->
            arra.add(it.replace(".json", ""))
        }
        return arra.toTypedArray()
    }
}