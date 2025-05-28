package me.uncertawn.uFastTravel.travelpath

import me.uncertawn.uFastTravel.data.TravelData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

class DebugPath(
    private val plugin: Plugin,
    private val player: Player,
    private val seconds: Int,
    private val travelData: TravelData
) {
    private var taskId: Int = -1
    private var timeLeft = seconds
    private val textDisplays = mutableListOf<TextDisplay>()

    fun start() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            when (timeLeft) {
                0 -> {
                    stop()
                    clearDisplays()
                }
                else -> {
                    if (textDisplays.isEmpty()) {
                        loadAndSpawnDisplays()
                    }
                    timeLeft--
                }
            }
        }, 0L, 20L)
    }

    fun stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
    }

    private fun clearDisplays() {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            textDisplays.forEach { it.remove() }
            textDisplays.clear()
        })
    }

    private fun loadAndSpawnDisplays() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            Bukkit.getScheduler().runTask(plugin, Runnable {
                travelData.points.forEachIndexed { index, vector ->
                    val display = spawnTextDisplay(player.location, vector, index.toString(), index)
                    display.text().hoverEvent(HoverEvent.showText(Component.text("Point $index at ${vector.x}, ${vector.y}, ${vector.z}")))
                    textDisplays.add(display)
                }
            })
        })
    }

    private fun spawnTextDisplay(baseLocation: Location, position: Vector, text: String, index: Int): TextDisplay {
        val world = baseLocation.world ?: throw IllegalArgumentException("World cannot be null")
        val location = baseLocation.clone().apply {
            x = position.x
            y = position.y
            z = position.z
        }

        return world.spawn(location, TextDisplay::class.java).apply {
            this.text(Component.text(text))
            transformation.scale.mul(2.0f)
            isPersistent = false
            lineWidth = 800*2
            backgroundColor = Color.RED
            textOpacity = 2
            setDefaultBackground(false)
            billboard = Display.Billboard.CENTER
        }
    }
}