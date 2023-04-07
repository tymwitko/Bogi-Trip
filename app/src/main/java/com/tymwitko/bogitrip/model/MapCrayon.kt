package com.tymwitko.bogitrip.model

import com.tymwitko.bogitrip.MAXIMUM_LATITUDE
import io.reactivex.Observable
import io.reactivex.Single
import org.osmdroid.bonuspack.routing.MapQuestRoadManager
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

class MapCrayon {
    fun drawRoute(startLat: Double, startLong: Double, targetLat: Double?, targetLong: Double?, roadManager: OSRMRoadManager) =
        Single.fromCallable {
            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(GeoPoint(startLat, startLong))
            val endPoint = targetLat?.let { targetLong?.let { it1 -> GeoPoint(it, it1) } }
            endPoint?.let { waypoints.add(it) }
            return@fromCallable roadManager.getRoad(waypoints)
        }

    fun drawCircle(colorCode: Int, location: GeoPoint, radius: Double, map: MapView): Polygon {
        /*
         * <b>Note</b></b: when plotting a point off the map, the conversion from
         * screen coordinates to map coordinates will return values that are invalid from a latitude,longitude
         * perspective. Sometimes this is a wanted behavior and sometimes it isn't. We are leaving it up to you,
         * the developer using osmdroid to decide on what is right for your application. See
         * <a href="https://github.com/osmdroid/osmdroid/pull/722">https://github.com/osmdroid/osmdroid/pull/722</a>
         * for more information and the discussion associated with this.
         */

        val circle: List<GeoPoint> =
            Polygon.pointsAsCircle(
                DistanceCalculator().assurePointOnMap(location),
                radius
            )
        //TODO: implement moveToDesiredPosition()
//        for (point in circle){
//            if (point.latitude > maxLat || point.latitude < -maxLat){
//                point.moveToDesiredPosition()
//            }
//        }
        return Polygon(map)
            .apply {
                infoWindow = null
                outlinePaint.color = colorCode
                points = circle
            }
    }
}