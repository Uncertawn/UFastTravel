package me.uncertawn.uFastTravel.data

import org.bukkit.util.Vector

class TravelData(
    var name: String,
    var world: String,
    var from: String = "",
    var to: String = "",
    var message: String = "",
    var points: Array<Vector> = emptyArray()
) {
    fun getStartingPoint(): Vector? {
        if (points.isNotEmpty())
           return points[0]
        return null
    }

    fun getEndingPoint(): Vector? {
        if (points.isNotEmpty())
            return points[0]
        return null
    }
}