package com.tymwitko.bogitrip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.mapsforge.map.layer.overlay.Circle
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import kotlin.math.*
import kotlin.random.Random


/**
 * A simple [Fragment] subclass.
 * Use the [StarterMapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StarterMapFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {
//    private var _binding: FragmentStarterMapBinding? = null
    private lateinit var map : MapView
    private lateinit var v : View
    private var minRange: Double = 0.0
    private var maxRange: Double = 0.0
    private val items = ArrayList<OverlayItem>()
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var randomOverlay: ItemizedOverlayWithFocus<OverlayItem>
    private lateinit var pmin: Polygon
    private lateinit var pmax: Polygon
    private var maxLat: Double = 85.05112877980658
    private var lastLocLat: Double = 0.0
    private var lastLocLong: Double = 0.0
    private var isLocation = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = context?.packageName
        //important! set your user agent to prevent getting banned from the osm servers
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        v = inflater.inflate(R.layout.fragment_starter_map, null)

        val btnRandom = v.findViewById<View>(R.id.btnRandom)
        val btnRoute = v.findViewById<View>(R.id.btnRoute)
        val btnLocation = v.findViewById<View>(R.id.btnLocation)
        val btnOrient = v.findViewById<View>(R.id.btnOrient)

        btnRandom.setOnClickListener(this)
        btnRoute.setOnClickListener(this)
        btnLocation.setOnClickListener(this)
        btnOrient.setOnClickListener(this)

        val editMinRange = v.findViewById<View>(R.id.editRangeMin) as EditText
        val editMaxRange = v.findViewById<View>(R.id.editRangeMax) as EditText

        editMinRange.backgroundTintList = ColorStateList.valueOf(Color.MAGENTA)
        editMaxRange.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
        editMinRange.setTextColor(ColorStateList.valueOf(Color.MAGENTA))
        editMaxRange.setTextColor(ColorStateList.valueOf(Color.BLUE))
        editMinRange.setHintTextColor(ColorStateList.valueOf(Color.MAGENTA))
        editMaxRange.setHintTextColor(ColorStateList.valueOf(Color.BLUE))
//        editMinRange.doAfter

        map = v.findViewById<View>(R.id.mapview) as MapView
        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                Log.i(
                    IMapView.LOGTAG,
                    System.currentTimeMillis().toString() + " onScroll " + event.x + "," + event.y
                )
                //Toast.makeText(getActivity(), "onScroll", Toast.LENGTH_SHORT).show();
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Log.i(
                    IMapView.LOGTAG,
                    System.currentTimeMillis().toString() + " onZoom " + event.zoomLevel
                )
                return true
            }
        })

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        map.setUseDataConnection(true)
        map.controller.setZoom(10.0)
        map.controller.setCenter(GeoPoint(0.0, 0.0))

        map.setScrollableAreaLimitLatitude(TileSystem.MaxLatitude,-TileSystem.MaxLatitude, 0)
        map.minZoomLevel = 2.0

        val rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay.isEnabled
        map.setMultiTouchControls(true)
        map.overlays.add(rotationGestureOverlay)

        val copyrightOverlay = CopyrightOverlay(requireContext())
        copyrightOverlay.setAlignBottom(true)
        copyrightOverlay.setAlignRight(true)
        map.overlays.add(copyrightOverlay)




        // Note, "context" refers to your activity/application context.
        // You can simply do resources.displayMetrics when inside an activity.
        // When you aren't in an activity class, you will need to have passed the context
        // to the non-activity class.
        val dm : DisplayMetrics? = context?.resources?.displayMetrics
        val scaleBarOverlay = ScaleBarOverlay(map)
        scaleBarOverlay.setCentred(true)
        //play around with these values to get the location on screen in the right place for your application
        scaleBarOverlay.setScaleBarOffset(dm!!.widthPixels / 2, 10)
        map.overlays.add(scaleBarOverlay);

        // Acquire a reference to the system Location Manager
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val provider = GpsMyLocationProvider(requireContext())
        provider.addLocationSource(LocationManager.GPS_PROVIDER)
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER)
        provider.locationUpdateMinDistance = 100f
        provider.locationUpdateMinTime = 10000


        // Define a listener that responds to location updates

        // Define a listener that responds to location updates
        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Called when a new location is found by the network location provider.
                Log.d("TAG", "Location: ${location.latitude}")
                lastLocLat = location.latitude
                lastLocLong = location.longitude
                isLocation = true
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
                Log.d("TAG", "Location status: $provider $status")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d("TAG", "Provider enabled $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.d("TAG", "Provider disabled $provider")
            }
        }

        // Register the listener with the Location Manager to receive location updates

        // Register the listener with the Location Manager to receive location updates
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("TAG", "Location access granted")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100,
                100f,
                locationListener
            )
        }
        myLocationOverlay = MyLocationNewOverlay(provider, map)
        if (!myLocationOverlay.enableMyLocation()) {
            Log.d("TAG", "Location not enabled")
        }

        myLocationOverlay.isDrawAccuracyEnabled = true
//        val icon: Bitmap = BitmapFactory.decodeResource(resources,
//            org.osmdroid.library.R.drawable.person
//        )
//        myLocationOverlay.setPersonIcon(icon)
        myLocationOverlay.runOnFirstFix(Runnable { // never reaches this point
            Log.d("TAG", "runOnFirstFix")
            if (myLocationOverlay.myLocation == null) {
                Log.d("TAG", "Location nott retrieved")
            }
        })
        map.overlays.add(myLocationOverlay)
        map.invalidate()

        if (myLocationOverlay.myLocation == null) {
            // could be too early
            Log.d("TAG", "Location not retrieved")
        }

        editMinRange.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                minRange = if (s.isNotEmpty()) {
                    s.toString().toDouble()
                }else{
                    0.0
                }
                try {
                    if (isLocation) {
                        drawCircleMin()
                    }
                } catch (e: java.lang.IllegalArgumentException) {
                    Toast.makeText(context, R.string.TOAST_EDGE, Toast.LENGTH_SHORT).show()
                }

            }
            override fun afterTextChanged(s: Editable) {
            }
        })

        editMaxRange.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                maxRange = if (s.isNotEmpty()) {
                    s.toString().toDouble()
                }else{
                    0.0
                }
                try {
                    if (isLocation) {
                        drawCircleMax()
                    }
                } catch (e: java.lang.IllegalArgumentException) {
                    Toast.makeText(context, R.string.TOAST_EDGE, Toast.LENGTH_SHORT).show()
                }
            }
            override fun afterTextChanged(s: Editable) {
            }
        })

        map.controller.setCenter(myLocationOverlay.myLocation)

        map.controller.animateTo(myLocationOverlay.myLocation)
        map.controller.setZoom(6.0)

        if (savedInstanceState != null){
            map.controller.setZoom(savedInstanceState.getFloat("ZOOM").toDouble())
            map.controller.setCenter(GeoPoint(savedInstanceState.getFloat("CENTER_LAT").toDouble(), savedInstanceState.getFloat("CENTER_LONG").toDouble()))
            map.mapOrientation = savedInstanceState.getFloat("ORIENTATION")
            isLocation = savedInstanceState.getBoolean("IS_LOC")
            lastLocLat = savedInstanceState.getFloat("LOC_LAT").toDouble()
            lastLocLong = savedInstanceState.getFloat("LOC_LONG").toDouble()
//            map.controller.setCenter(GeoPoint(lastLocLat, lastLocLong))
//            dessertsSold = savedInstanceState.getInt(KEY_DESSERT_SOLD, 0)
//            showCurrentDessert()
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TAG", "onviewcreated")
        if(isLocation) {
            drawCircleMin()
            drawCircleMax()
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        map.controller.setCenter(myLocationOverlay.myLocation)
        myLocationOverlay.onResume()
        if (this::randomOverlay.isInitialized) {
            randomOverlay.onResume()
        }
        if (this::pmin.isInitialized) {
            pmin.onResume()
        }
        if (this::pmax.isInitialized) {
            pmax.onResume()
        }
//        myLocationOverlay.lastFix
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("TAG", "onSaveInstanceState called")
        try {
            outState.putFloat("LOC_LAT", lastLocLat.toFloat())
            outState.putFloat("LOC_LONG", lastLocLong.toFloat())
        } catch (e: java.lang.NullPointerException){}
        outState.putFloat("CENTER_LAT", map.mapCenter.latitude.toFloat())
        outState.putFloat("CENTER_LONG", map.mapCenter.longitude.toFloat())
        outState.putFloat("ZOOM", map.zoomLevelDouble.toFloat())
        outState.putFloat("ORIENTATION", map.mapOrientation)
        outState.putBoolean("IS_LOC", isLocation)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment StarterMapFragment.
         */
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            StarterMapFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
    }

    override fun onClick(p0: View?) {
        if (p0 == v.findViewById(R.id.btnRandom)){
            if (myLocationOverlay.myLocation != null) {
                generateRandom(minRange, maxRange)
            } else {
                Toast.makeText(context, R.string.TOAST_LOC, Toast.LENGTH_SHORT).show()
            }
        }
        if (p0 == v.findViewById(R.id.btnRoute)){
            drawRoute()
        }
        if (p0 == v.findViewById(R.id.btnLocation)){
            map.controller.animateTo(myLocationOverlay.myLocation)
//            map.controller.setZoom(16.0)
        }
        if (p0 == v.findViewById(R.id.btnOrient)){
            map.mapOrientation = 0.0f;
        }
    }

    private fun drawRoute() {
        Toast.makeText(context, R.string.TOAST_SOON, Toast.LENGTH_SHORT).show()
        //TODO: Routing
    }

    private fun drawCircleMin() {
        var loc = myLocationOverlay.myLocation
        if(loc == null){
            loc = GeoPoint(lastLocLat, lastLocLong)
        }
        if (this::pmin.isInitialized && pmin in map.overlays) {
            map.overlays.remove(pmin)
        }
        /*
         * <b>Note</b></b: when plotting a point off the map, the conversion from
         * screen coordinates to map coordinates will return values that are invalid from a latitude,longitude
         * perspective. Sometimes this is a wanted behavior and sometimes it isn't. We are leaving it up to you,
         * the developer using osmdroid to decide on what is right for your application. See
         * <a href="https://github.com/osmdroid/osmdroid/pull/722">https://github.com/osmdroid/osmdroid/pull/722</a>
         * for more information and the discussion associated with this.
         */

        //just in case the point is off the map, let's fix the coordinates
        if (loc.longitude < -180) loc.longitude = loc.longitude + 360
        if (loc.longitude > 180) loc.longitude = loc.longitude - 360
        //latitude is a bit harder. see https://en.wikipedia.org/wiki/Mercator_projection
        if (loc.latitude > maxLat) loc.latitude = maxLat
        if (loc.latitude < -maxLat) loc.latitude = -maxLat
        val circle: List<GeoPoint> = Polygon.pointsAsCircle(loc, minRange*1000)
        //TODO: for (point in circle) { if (point.latitude > maxLat || point.latitude < -maxLat) { point.moveToDesiredPosition() } }
        pmin = Polygon(map)
        pmin.setStrokeColor(Color.MAGENTA)
        pmin.points = circle
        pmin.title = getString(R.string.MIN_RANGE)
        map.overlays.add(pmin)
        map.invalidate()
    }

    private fun drawCircleMax() {
        var loc = myLocationOverlay.myLocation
        if(loc == null){
            loc = GeoPoint(lastLocLat, lastLocLong)
        }
        if (this::pmax.isInitialized && pmax in map.overlays) {
            map.overlays.remove(pmax)
        }
        /*
         * <b>Note</b></b: when plotting a point off the map, the conversion from
         * screen coordinates to map coordinates will return values that are invalid from a latitude,longitude
         * perspective. Sometimes this is a wanted behavior and sometimes it isn't. We are leaving it up to you,
         * the developer using osmdroid to decide on what is right for your application. See
         * <a href="https://github.com/osmdroid/osmdroid/pull/722">https://github.com/osmdroid/osmdroid/pull/722</a>
         * for more information and the discussion associated with this.
         */

        //just in case the point is off the map, let's fix the coordinates
        if (loc.longitude < -180) loc.longitude = loc.longitude + 360
        if (loc.longitude > 180) loc.longitude = loc.longitude - 360
        //latitude is a bit harder. see https://en.wikipedia.org/wiki/Mercator_projection
        if (loc.latitude > maxLat) loc.latitude = maxLat
        if (loc.latitude < -maxLat) loc.latitude = -maxLat
        val circle: List<GeoPoint> = Polygon.pointsAsCircle(loc, maxRange*1000)
        //TODO: implement moveToDesiredPosition()
//        for (point in circle){
//            if (point.latitude > maxLat || point.latitude < -maxLat){
//                point.moveToDesiredPosition()
//            }
//        }
        pmax = Polygon(map)
        pmax.setStrokeColor(Color.BLUE)
        pmax.points = circle
        pmax.title = getString(R.string.MAX_RANGE)
        map.overlays.add(pmax)
        if (this::pmin.isInitialized){
            Log.d("TAG", "drawMin")
            drawCircleMin()
        }

        map.invalidate()
    }

    private fun generateRandom(minRangeArg: Double, maxRangeArg: Double) {
        val minR = minRangeArg * 1000
        val maxR = maxRangeArg * 1000
        if (maxR >= minR && maxR > 0 && minR >= 0) {
            val randAng = Random.nextFloat() * 360f
            val randDist = Random.nextFloat() * (maxR - minR) + minR
            if (this::randomOverlay.isInitialized && randomOverlay in map.overlays) {
                map.overlays.remove(randomOverlay)
                items.clear()
            }
            items.add(OverlayItem(getString(R.string.RAND_TITLE), getString(R.string.RAND_SNIP), moveByDistAngle(randDist, randAng, myLocationOverlay)))
            //TODO: coords link for different apps or nav
            @Suppress("DEPRECATION")
            randomOverlay = ItemizedOverlayWithFocus(items, object: ItemizedIconOverlay.OnItemGestureListener<OverlayItem>{
                override fun onItemSingleTapUp(index:Int, item:OverlayItem):Boolean {
                    //do something
                    return true
                }
                override fun onItemLongPress(index:Int, item:OverlayItem):Boolean {
                    return false
                }
            }, context)
            randomOverlay.setFocusItemsOnTap(true);
            map.overlays.add(randomOverlay)
            map.invalidate()
        }
        else{
            Toast.makeText(requireContext(), R.string.TOAST_INV, Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveByDistAngle(distanceMeters: Double, angle: Float, origin: MyLocationNewOverlay): GeoPoint {
        val distRadians = distanceMeters / (6372797.6)
        val oriLoc = origin.myLocation
        val lat1 = oriLoc.latitude * PI / 180
        val lon1 = oriLoc.longitude * PI / 180
        val lat2 = asin(sin(lat1) * cos(distRadians) + cos(lat1) * sin(distRadians) * cos(angle))
        val lon2 = lon1 + atan2(sin(angle) * sin(distRadians) * cos(lat1), cos(distRadians) - sin(lat1) * sin(lat2))
        return GeoPoint(lat2 * 180 / PI, lon2 * 180 / PI)
    }

    override fun onLongClick(p0: View?): Boolean {
        return false
    }
}