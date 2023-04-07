package com.tymwitko.bogitrip.model

import android.graphics.Point
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan

class MapPotato {
    fun naviMapOrient(step: Int = 0, road: Road, map: MapView): Double {
        if (road.mNodes.size > step+1) {
            val projection: Projection = map.projection
            val zero = Point()
            projection.toPixels(road.mNodes[step].mLocation, zero)
            val one = Point()
            projection.toPixels(road.mNodes[step+1].mLocation, one)
            val dlong = one.x - zero.x
            val dlat = -one.y + zero.y
            val ori = abs(atan(dlat.toDouble() / dlong.toDouble()) * 360f / (2f * PI.toFloat())) % 90f
            return if (dlong > 0) {
                if (dlat > 0) {
                    -90 + abs(ori)
                } else {
                    -90 - abs(ori)
                }
            } else {
                if (dlat > 0) {
                    90 - abs(ori)
                } else {
                    90 + abs(ori)
                }
            }
        }
        return 0.0
    }
}