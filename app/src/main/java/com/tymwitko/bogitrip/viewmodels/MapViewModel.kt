package com.tymwitko.bogitrip.viewmodels

import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.tymwitko.bogitrip.MaxMin
import com.tymwitko.bogitrip.model.*
import com.tymwitko.bogitrip.model.TtsManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapViewModel: ViewModel(), KoinComponent {

    private val turnNavigator: TurnByTurnNavigator by inject()
    private val ttsManager: TtsManager by inject()

    private lateinit var road: Road
    private lateinit var roadOverlay: Polyline

    private var lastLocLat: Double? = null
    private var lastLocLong: Double? = null

    private var previousLocLat: Double? = null
    private var previousLocLong: Double? = null

    private var circleMin: Polygon? = null
    private var circleMax: Polygon? = null

    private var randomOverlay: Marker? = null

    var myLocationOverlay: MyLocationNewOverlay? = null

    private var naviStep = 1

    private var isNaviMode = false

    fun setNaviStep(new: Int) {
        naviStep = new
    }

    fun getNaviStep() = naviStep

    fun setNewRandomPoint(marker: Marker) {
        randomOverlay = marker
    }

    fun getRandomOverlay() = randomOverlay

    fun navigateAtCurrentStep() =
        navigate(road, naviStep)

    private fun navigate(road: Road, step: Int) =
        turnNavigator.getCurrentInstruction(road, step) // todo

    fun drawCircle(size: MaxMin, colorCode: Int, location: GeoPoint, radius: Double, map: MapView) =
        when (size) {
            MaxMin.MAX -> MapCrayon().drawCircle(colorCode, location, radius, map)
                .also {
                    circleMax = it
                }
            MaxMin.MIN -> MapCrayon().drawCircle(colorCode, location, radius, map)
                .also {
                    circleMin = it
                }
        }

    fun getCircle(size: MaxMin) =
        when (size) {
            MaxMin.MAX -> circleMax
            MaxMin.MIN -> circleMin
        }

    fun drawRouteTo(targetLat: Double?, targetLong: Double?, roadManager: OSRMRoadManager) =
        lastLocLat?.let {
            lastLocLong?.let { it1 ->
                MapCrayon().drawRoute(it, it1, targetLat, targetLong, roadManager)
                    .doOnSuccess { road = it } }
        }

    fun naviMapOrient(step: Int = 0, map: MapView) =
        MapPotato().naviMapOrient(step, road, map)

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    fun isRoadPresentAndOk() = this::road.isInitialized && road.mStatus == Road.STATUS_OK

    fun getRoadStatus() = if(this::road.isInitialized) {
        road.mStatus
    } else {
        null
    }

    fun getRouteOverlay() = if (this::roadOverlay.isInitialized) {
        roadOverlay
    } else if (this::road.isInitialized) {
        RoadManager.buildRoadOverlay(road)
            .also { roadOverlay = it }
    } else {
        null
    }

    fun getRouteOverlayFromRoad(road: Road?): Polyline =
        RoadManager.buildRoadOverlay(road)

    fun updateLocation(location: GeoPoint) {
        previousLocLong = lastLocLong
        previousLocLat = lastLocLat
        lastLocLat = location.latitude
        lastLocLong = location.longitude
    }

    fun updateLocation(location: Location) {
        previousLocLong = lastLocLong
        previousLocLat = lastLocLat
        lastLocLat = location.latitude
        lastLocLong = location.longitude
    }

    fun getLastLocation() =
        lastLocLat?.let {
            lastLocLong?.let {
                it1 -> GeoPoint(it, it1)
            }
        }

    fun getLocationFromOverlay() = myLocationOverlay?.myLocation

    fun setupMyLocationOverlay(locationOverlay: MyLocationNewOverlay? = null) {
        locationOverlay?.let { myLocationOverlay = it }
        myLocationOverlay
            ?.apply {
                enableMyLocation()
                isDrawAccuracyEnabled = true
                runOnFirstFix { // never reaches this point
                    Log.d("TAG", "runOnFirstFix")
                    if (myLocationOverlay?.myLocation == null) {
                        Log.d("TAG", "Location nott retrieved")
                    }
                }
            }
    }

    fun setLocationIcon(icon: Bitmap) {
        myLocationOverlay?.setPersonIcon(icon)
    }

    fun moveByDistAngle(randDist: Double, randAng: Float): GeoPoint? =
        myLocationOverlay?.let { DistanceCalculator().moveByDistAngle(randDist, randAng, it) }

    fun switchLocationOverlayToNavi() {
        myLocationOverlay
            ?.apply {
                enableFollowLocation()
                enableAutoStop = false
            }
    }

    fun isInNaviMode() = isNaviMode

    fun setNaviMode(mode: Boolean) {
        isNaviMode = mode
    }

    fun checkIfPointPassed(point: GeoPoint = road.mNodes[naviStep].mLocation): Boolean {
        val last = lastLocLat?.let { lastLocLong?.let { it1 -> GeoPoint(it, it1) } }?.let {
            DistanceCalculator().getDistanceBetweenPoints(
                point,
                it
            )
        }
        val prev = previousLocLat?.let { previousLocLong?.let { it1 -> GeoPoint(it, it1) } }?.let {
            DistanceCalculator().getDistanceBetweenPoints(
                point,
                it
            )
        }
        return last?.let { l ->
            prev?.let { p ->
                l < p
            }
        } ?: false
    }
}