package com.tymwitko.bogitrip.model

import com.tymwitko.bogitrip.MAXIMUM_LATITUDE
import com.tymwitko.bogitrip.METERS_TO_RADIANS
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.*

class DistanceCalculator {
    fun moveByDistAngle(distanceMeters: Double, angle: Float, origin: MyLocationNewOverlay): GeoPoint {
        val distRadians = distanceMeters / (METERS_TO_RADIANS)
        val oriLoc = origin.myLocation
        val lat1 = oriLoc.latitude * PI / 180
        val lon1 = oriLoc.longitude * PI / 180
        val lat2 = asin(sin(lat1) * cos(distRadians) + cos(lat1) * sin(distRadians) * cos(angle))
        val lon2 = lon1 + atan2(sin(angle) * sin(distRadians) * cos(lat1), cos(distRadians) - sin(lat1) * sin(lat2))
        return GeoPoint(lat2 * 180 / PI, lon2 * 180 / PI)
    }

    fun getDistanceBetweenPoints(start: GeoPoint, end: GeoPoint): Double {
        val startFixed = assurePointOnMap(start)
        val endFixed = assurePointOnMap(end)
        return sqrt(abs((startFixed.latitude - endFixed.latitude).pow(2) +
                (startFixed.longitude - endFixed.longitude).pow(2)))
    }

    fun assurePointOnMap(point: GeoPoint): GeoPoint {
        if (point.longitude < 0) point.longitude = point.longitude + 360
        if (point.longitude > 180) point.longitude = point.longitude - 360
        //latitude is a bit harder. see https://en.wikipedia.org/wiki/Mercator_projection
        if (point.latitude > MAXIMUM_LATITUDE) point.latitude = MAXIMUM_LATITUDE
        if (point.latitude < -MAXIMUM_LATITUDE) point.latitude = -MAXIMUM_LATITUDE
        return point
    }
}