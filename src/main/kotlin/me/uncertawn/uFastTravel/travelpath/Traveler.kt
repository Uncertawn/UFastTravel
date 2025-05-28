package me.uncertawn.uFastTravel.travelpath

import io.papermc.paper.entity.TeleportFlag
import me.uncertawn.uFastTravel.UFastTravel
import me.uncertawn.uFastTravel.data.TravelData
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Traveler(private val plugin: UFastTravel, private val player: Player, private val travelData: TravelData) {
    private val movePoints = mutableListOf<Vector>()
    private var currentTargetIndex = 0
    private var taskId: Int = -1
    private val baseSpeed: Double = 1.0
    private val closeEnough: Double = 0.05
    val entity: ArmorStand
    private var currentSpeed: Double = baseSpeed
    private val originalSegmentDistances = mutableListOf<Double>()
    private var currentSegmentIndex = 0
    private var segmentProgress = 0.0
    private val segmentStartIndices = mutableListOf<Int>()
    private var currentYaw: Float = 0f

    init {
        val firstPoint = travelData.points.firstOrNull() ?: Vector()
        val spawnLocation = player.location.set(firstPoint.x, firstPoint.y, firstPoint.z)

        entity = player.world.spawn(spawnLocation, ArmorStand::class.java).apply {
            customName(Component.text("Path Follower"))
            isCustomNameVisible = true
            isInvulnerable = true
            isVisible = false
            isMarker = true
            setGravity(false)
            addPassenger(player)
        }
        currentYaw = entity.location.yaw

        calculateOriginalDistances()
        generateSmoothedPath()
    }

    private fun calculateYaw(direction: Vector): Float {
        val x = direction.x
        val z = direction.z
        var newYaw = Math.toDegrees(-Math.atan2(x, z)).toFloat()

        // Smooth transition between current and new yaw
        val angleDiff = ((((newYaw - currentYaw) % 360) + 540) % 360 - 180)
        currentYaw = (currentYaw + angleDiff * 0.2f) % 360

        return currentYaw
    }

    private fun calculateOriginalDistances() {
        originalSegmentDistances.clear()
        val path = travelData.points
        if (path.size < 2) return

        for (i in 0 until path.size - 1) {
            val distance = path[i].distance(path[i + 1])
            originalSegmentDistances.add(distance)
        }
    }

    private fun getSpeedForDistance(distance: Double): Double {
        // Adjust these values to your liking
        return when {
            distance < 10 -> baseSpeed
            distance < 30 -> baseSpeed * 2.0
            distance < 50 -> baseSpeed * 3.0
            else -> baseSpeed * 4.0
        }
    }

    private fun generateSmoothedPath() {
        val path = travelData.points
        if (path.size < 2) return

        movePoints.clear()
        segmentStartIndices.clear()
        movePoints.add(path[0])
        segmentStartIndices.add(0)

        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]
            val segmentDistance = originalSegmentDistances[i]

            val heightOffset = when {
                segmentDistance < 10 -> 1.5
                segmentDistance < 30 -> 2.5
                else -> 3.5
            }

            val control1 = calculateControlPoint(start, end, 0.33, heightOffset)
            val control2 = calculateControlPoint(start, end, 0.66, heightOffset)

            val segments = max(10, min(30, (segmentDistance / 2).toInt()))
            generateCubicBezierPoints(start, control1, control2, end, segments)

            segmentStartIndices.add(movePoints.size)
        }

        movePoints.add(path.last())
    }

    private fun calculateControlPoint(start: Vector, end: Vector, t: Double, heightOffset: Double): Vector {
        val direction = end.clone().subtract(start)
        val control = start.clone().add(direction.multiply(t))
        control.y = max(start.y, end.y) + heightOffset
        return control
    }

    private fun generateCubicBezierPoints(p0: Vector, p1: Vector, p2: Vector, p3: Vector, segments: Int) {
        for (i in 1 until segments) {
            val t = i.toDouble() / segments.toDouble()
            val point = cubicBezier(p0, p1, p2, p3, t)
            movePoints.add(point)
        }
    }

    private fun cubicBezier(p0: Vector, p1: Vector, p2: Vector, p3: Vector, t: Double): Vector {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        val point = p0.clone().multiply(uuu)
        point.add(p1.clone().multiply(3 * uu * t))
        point.add(p2.clone().multiply(3 * u * tt))
        point.add(p3.clone().multiply(ttt))

        return point
    }

    private fun updateSegmentTracking() {
        if (segmentStartIndices.size < 2) return

        for (i in segmentStartIndices.indices.reversed()) {
            if (currentTargetIndex >= segmentStartIndices[i]) {
                currentSegmentIndex = min(i, segmentStartIndices.size - 2)
                val segmentStart = segmentStartIndices[currentSegmentIndex]
                val segmentEnd = if (currentSegmentIndex + 1 < segmentStartIndices.size) {
                    segmentStartIndices[currentSegmentIndex + 1]
                } else {
                    movePoints.size - 1
                }
                segmentProgress = (currentTargetIndex - segmentStart).toDouble() / (segmentEnd - segmentStart).toDouble()
                break
            }
        }
    }

    private fun getDynamicSpeed(segmentDistance: Double, progress: Double): Double {
        val baseSpeed = getSpeedForDistance(segmentDistance)

        // Smooth acceleration/deceleration curve
        val progressFactor = sin(progress * Math.PI).toFloat()
        return baseSpeed * (0.5 + progressFactor * 0.5)
    }

    fun start(reversed: Boolean) {
        if (taskId != -1 || movePoints.isEmpty()) return

        val title = if (reversed) {
            Title.title(
                Component.text("Traveling to ${travelData.from}"),
                Component.text(travelData.message)
            )
        } else {
            Title.title(
                Component.text("Traveling to ${travelData.to}"),
                Component.text(travelData.message)
            )
        }

        player.showTitle(title)

        if (!plugin.playersMounted.contains(player.uniqueId)) {
            plugin.playersMounted.set(player.uniqueId, entity)
        }

        if (!plugin.entities.contains(entity)) {
            plugin.entities.add(entity)
        }

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            if (entity.isDead || !entity.isValid) {
                stop()
                return@scheduleSyncRepeatingTask
            }

            val target = movePoints[currentTargetIndex]
            val currentLoc = entity.location.toVector()
            val direction = target.clone().subtract(currentLoc)
            val distance = direction.length()

            if (distance > closeEnough) {
                updateSegmentTracking()

                val segmentDistance = if (currentSegmentIndex < originalSegmentDistances.size) {
                    originalSegmentDistances[currentSegmentIndex]
                } else {
                    originalSegmentDistances.lastOrNull() ?: 0.0
                }

                currentSpeed = getDynamicSpeed(segmentDistance, segmentProgress)
                val moveDistance = minOf(distance, currentSpeed)
                val velocity = direction.normalize().multiply(moveDistance)

                val newLocation = entity.location.add(velocity)
                newLocation.yaw = calculateYaw(direction)

                entity.teleport(
                    newLocation,
                    TeleportFlag.EntityState.RETAIN_PASSENGERS
                )
            } else {
                currentTargetIndex++
                if (currentTargetIndex >= movePoints.size) {
                    remove()
                    return@scheduleSyncRepeatingTask
                }
            }

            if (entity.passengers.isEmpty()) {
                entity.addPassenger(player)
            }
        }, 0L, 1L)
    }

    fun stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
            if (plugin.playersMounted.contains(player.uniqueId)) {
                plugin.playersMounted.remove(player.uniqueId)
            }
        }
    }

    fun remove() {
        stop()
        entity.remove()
        if (plugin.entities.contains(entity)) {
            plugin.entities.remove(entity)
        }
    }
}

// i am unsure which i wanna use but i think i am done with this class and have the bottom comment just in case
/*
class Traveler(private val plugin: Plugin, private val player: Player, private val travelData: TravelData) {
    private val movePoints = mutableListOf<Vector>()
    private var currentTargetIndex = 0
    private var taskId: Int = -1
    private val baseSpeed: Double = 1.0
    private val closeEnough: Double = 0.05
    val entity: ArmorStand
    private var currentSpeed: Double = baseSpeed

    init {
        val firstPoint = travelData.points.firstOrNull() ?: Vector()
        val spawnLocation = player.world.getHighestBlockAt(firstPoint.x.toInt(), firstPoint.z.toInt())
            .location.add(0.0, 1.0, 0.0)

        entity = player.world.spawn(spawnLocation, ArmorStand::class.java).apply {
            customName(Component.text("Path Follower"))
            isCustomNameVisible = true
            isInvulnerable = true
            isVisible = false
            isMarker = true
            setGravity(false)
        }

        generateSmoothedPath()
    }

    private fun generateSmoothedPath() {
        val path = travelData.points
        if (path.size < 2) return

        movePoints.clear()

        if (path.size == 2) {
            generateCubicBezierPoints(
                path[0],
                path[0].clone().add(Vector(0.0, 5.0, 0.0)),
                path[1].clone().add(Vector(0.0, 5.0, 0.0)),
                path[1],
                segments = 15
            )
            return
        }

        movePoints.add(path[0])

        val controlPoints = mutableListOf<Pair<Vector, Vector>>()
        for (i in 1 until path.size - 1) {
            val prev = path[i - 1]
            val current = path[i]
            val next = path[i + 1]

            val tangent = next.clone().subtract(prev).normalize()
            val control1 = current.clone().subtract(tangent.clone().multiply(0.5))
            val control2 = current.clone().add(tangent.clone().multiply(0.5))

            control1.y = max(current.y, prev.y) + 3.0
            control2.y = max(current.y, next.y) + 3.0

            controlPoints.add(Pair(control1, control2))
        }

        for (i in 0 until path.size - 1) {
            val p0 = path[i]
            val p3 = path[i + 1]

            val p1 = if (i == 0) {
                p0.clone().add(Vector(0.0, 5.0, 0.0))
            } else {
                controlPoints[i - 1].second
            }

            val p2 = if (i == path.size - 2) {
                p3.clone().add(Vector(0.0, 5.0, 0.0))
            } else {
                controlPoints[i].first
            }

            generateCubicBezierPoints(p0, p1, p2, p3, segments = 15)
        }

        movePoints.add(path.last())
    }

    private fun generateCubicBezierPoints(p0: Vector, p1: Vector, p2: Vector, p3: Vector, segments: Int) {
        for (i in 1 until segments) {
            val t = i.toDouble() / segments.toDouble()
            val point = cubicBezier(p0, p1, p2, p3, t)
            movePoints.add(point)
        }
    }

    private fun cubicBezier(p0: Vector, p1: Vector, p2: Vector, p3: Vector, t: Double): Vector {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        val point = p0.clone().multiply(uuu)
        point.add(p1.clone().multiply(3 * uu * t))
        point.add(p2.clone().multiply(3 * u * tt))
        point.add(p3.clone().multiply(ttt))

        return point
    }

    fun start(reversed: Boolean) {
        if (taskId != -1 || movePoints.isEmpty()) return
        var title: Title?
        if (reversed)
            title = Title.title(Component.text("Traveling to ${travelData.from}"), Component.text(travelData.message))
        else
            title = Title.title(Component.text("Traveling to ${travelData.to}"), Component.text(travelData.message))
        player.showTitle(title)

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            if (entity.isDead || !entity.isValid) {
                stop()
                return@scheduleSyncRepeatingTask
            }

            val target = movePoints[currentTargetIndex]
            val currentLoc = entity.location.toVector()
            val direction = target.clone().subtract(currentLoc)
            val distance = direction.length()

            if (distance > closeEnough) {
                // Dynamic speed adjustment - slows down as approaching point
                currentSpeed = baseSpeed

                // Calculate exact movement needed to reach target (no overshooting)
                val moveDistance = minOf(distance, currentSpeed)
                val velocity = direction.normalize().multiply(moveDistance)

                entity.teleport(entity.location.add(velocity), TeleportFlag.EntityState.RETAIN_PASSENGERS)
            } else {
                currentTargetIndex++
                if (currentTargetIndex >= movePoints.size) {
                    remove()
                    return@scheduleSyncRepeatingTask
                }
            }
            if (entity.passengers.isEmpty()) {
                entity.addPassenger(player)
            }
        }, 0L, 1L)
    }

    fun stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
    }

    fun remove() {
        stop()
        entity.remove()
    }
}
*/
