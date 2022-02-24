package com.tymwitko.bogitrip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.location.*
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.osmdroid.api.IMapView
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.TileStates
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
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

enum class STATE{
    INIT, POINT, ROUTE, NAVI
}

class StarterMapFragment : Fragment(), View.OnClickListener, View.OnLongClickListener, TextToSpeech.OnInitListener {
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
    private lateinit var roadManager: RoadManager
    private lateinit var roadOverlay: Polyline
    private lateinit var btnNavi: View
    private lateinit var btnQuitNavi: View
    private lateinit var btnRoute: View
    private lateinit var btnLocation: View
    private lateinit var btnOrient: View
    private lateinit var btnRandom: View
    private lateinit var insTextView: TextView
    private lateinit var compassOverlay: CompassOverlay
    private lateinit var road: Road
    private lateinit var editMinRange: EditText
    private lateinit var editMaxRange: EditText
    private var maxLat: Double = 85.05112877980658
    private var lastLocLat: Double = 200.0
    private var lastLocLong: Double = 200.0
    private var isLocation = false
    private var state = STATE.INIT
//    private var firstCreate = true
    private lateinit var tts: TextToSpeech
//    private lateinit var orientationListener: OrientationEventListener
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
        state = STATE.INIT
        v = inflater.inflate(R.layout.fragment_starter_map, null)

        roadManager = OSRMRoadManager(context, context?.packageName)
        //(roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE)
        //TODO: get a real link
//        (roadManager as OSRMRoadManager).setService("https://api.openrouteservice.org/v2/directions/driving-car/geojson?api_key=5b3ce3597851110001cf6248cd9a342bec284d8db5e8f4ecd2faa6a9")

        btnRandom = v.findViewById(R.id.btnRandom)
        btnRoute = v.findViewById(R.id.btnRoute)
        btnLocation = v.findViewById(R.id.btnLocation)
        btnOrient = v.findViewById(R.id.btnOrient)
        btnNavi = v.findViewById(R.id.btnNavi)
        btnQuitNavi = v.findViewById(R.id.btnQuitNavi)
        insTextView = v.findViewById(R.id.insTextView)

        btnRandom.setOnClickListener(this)
        btnRoute.setOnClickListener(this)
        btnLocation.setOnClickListener(this)
        btnOrient.setOnClickListener(this)
        btnNavi.setOnClickListener(this)
        btnQuitNavi.setOnClickListener(this)

        btnNavi.isVisible = false
        btnQuitNavi.isVisible = false
        insTextView.isVisible = false

        editMinRange = v.findViewById<View>(R.id.editRangeMin) as EditText
        editMaxRange = v.findViewById<View>(R.id.editRangeMax) as EditText

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

        compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), map)
        compassOverlay.enableCompass()
//        map.overlays.add(compassOverlay)


        // Note, "context" refers to your activity/application context.
        // You can simply do resources.displayMetrics when inside an activity.
        // When you aren't in an activity class, you will need to have passed the context
        // to the non-activity class.
        val dm : DisplayMetrics? = context?.resources?.displayMetrics
        val scaleBarOverlay = ScaleBarOverlay(map)
        scaleBarOverlay.setCentred(true)
        //play around with these values to get the location on screen in the right place for your application
        scaleBarOverlay.setScaleBarOffset(dm!!.widthPixels / 2, 10)
        map.overlays.add(scaleBarOverlay)

        // Acquire a reference to the system Location Manager
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val provider = GpsMyLocationProvider(requireContext())
        provider.addLocationSource(LocationManager.GPS_PROVIDER)
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER)
        provider.locationUpdateMinDistance = 1f
        provider.locationUpdateMinTime = 10


        // Define a listener that responds to location updates

        // Define a listener that responds to location updates
        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Called when a new location is found by the network location provider.
                Log.d("TAG", "Location: ${location.latitude}")
                lastLocLat = location.latitude
                lastLocLong = location.longitude
                isLocation = true
//                if(state == STATE.NAVI){
//                    drawRoute()
////                    map.mapOrientation = -compassOverlay.orientation
//                }
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
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("TAG", "Location access granted")
//            locationManager.requestLocationUpdates(
//                LocationManager.NETWORK_PROVIDER,
//                1,
//                1f,
//                locationListener
//            )
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1,
                1f,
                locationListener
            )
        }
        myLocationOverlay = MyLocationNewOverlay(provider, map)
        if (!myLocationOverlay.enableMyLocation()) {
            Log.d("TAG", "Location not enabled")
        }

        myLocationOverlay.isDrawAccuracyEnabled = true
        val icon: Bitmap = BitmapFactory.decodeResource(resources,
            org.osmdroid.library.R.drawable.person
        )
        myLocationOverlay.setPersonIcon(icon)
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
                    if (myLocationOverlay.myLocation != null){
                        isLocation = true
                    }
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
                    if (myLocationOverlay.myLocation != null){
                        isLocation = true
                    }
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
        map.controller.setZoom(6.0)



        if (savedInstanceState != null){
            map.controller.setZoom(savedInstanceState.getFloat("ZOOM").toDouble())
            map.controller.setCenter(GeoPoint(savedInstanceState.getFloat("CENTER_LAT").toDouble(), savedInstanceState.getFloat("CENTER_LONG").toDouble()))
            map.mapOrientation = savedInstanceState.getFloat("ORIENTATION")
            isLocation = savedInstanceState.getBoolean("IS_LOC")
            lastLocLat = savedInstanceState.getFloat("LOC_LAT").toDouble()
            lastLocLong = savedInstanceState.getFloat("LOC_LONG").toDouble()
            state = savedInstanceState.getSerializable("STATE") as STATE
//            firstCreate = savedInstanceState.getBoolean("FIRST")
            if (state == STATE.NAVI || state == STATE.ROUTE) {
                road = savedInstanceState.getParcelable<Road>("ROAD")!!
            }
            //recovering random waypoint
            if (state != STATE.INIT) {
                items.add(
                    OverlayItem(
                        getString(R.string.RAND_TITLE),
                        getString(R.string.RAND_SNIP),
                        GeoPoint(
                            savedInstanceState.getFloat("RAND_LAT").toDouble(),
                            savedInstanceState.getFloat("RAND_LONG").toDouble()
                        )
                    )
                )
                //@Suppress("DEPRECATION")
                randomOverlay = ItemizedOverlayWithFocus(
                    items,
                    object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                        override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                            //do something
                            return true
                        }

                        override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                            return false
                        }
                    },
                    context
                )
                randomOverlay.setFocusItemsOnTap(true)
                map.overlays.add(randomOverlay)
                map.invalidate()
            }

            if ((state == STATE.ROUTE || state == STATE.NAVI) && isLocation) {
                drawRoute()
                btnRoute.isVisible = false
                if (state == STATE.ROUTE){
                    btnNavi.isVisible = true
                }else{
                    btnNavi.isVisible = false
                    btnOrient.isVisible = false
                    btnQuitNavi.isVisible = true
                    btnLocation.isVisible = false
                    btnRandom.isVisible = false
                    editMaxRange.isVisible = false
                    editMinRange.isVisible = false
                    insTextView.isVisible = true
                    navigate()
                }
            }
        }
        tts = TextToSpeech(context, this)
        return v
    }

    private fun navigate() {
        state = STATE.NAVI
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        map.controller.setZoom(18.0)
        myLocationOverlay.enableFollowLocation()
        myLocationOverlay.enableAutoStop = false

        var i = 1
        val insThread = Thread{
            try {
                drawRoute()
                naviMapOrient()
                insTextView.text = road.mNodes[1].mInstructions
                Log.d("TAG", "mInstructions: ${road.mNodes[0].mInstructions}, ${road.mNodes[1].mInstructions}, ${road.mNodes[2].mInstructions}")
                if (!this::tts.isInitialized){
                    tts = TextToSpeech(context, this)
                }
                tts.speak(road.mNodes[1].mInstructions, TextToSpeech.QUEUE_FLUSH, null,"")
//                while (i < road.mNodes.size - 1 && state == STATE.NAVI){
                while (state == STATE.NAVI){
                    if (abs(lastLocLat - road.mNodes[i].mLocation.latitude) < 0.0001 && abs(lastLocLong - road.mNodes[i].mLocation.longitude) < 0.0001) {
                        insTextView.text = road.mNodes[i + 1].mInstructions
                        tts.speak(road.mNodes[i+1].mInstructions, TextToSpeech.QUEUE_FLUSH, null,"")
//                        i += 1
                        drawRoute()
                        naviMapOrient()
                    }
                }
            } catch (e: Exception) {
                Log.d("TAG", "ERROR while instructing $e")
            }
        }
        insThread.start()
    }

    private fun naviMapOrient(){
        if (this::road.isInitialized) {
            if (road.mNodes.size > 1) {
                val projection: Projection = map.getProjection()
                val zero = Point()
                projection.toPixels(road.mNodes[0].mLocation, zero)
                val one = Point()
                projection.toPixels(road.mNodes[1].mLocation, one)
                val dlong = one.x - zero.x
                val dlat = -one.y + zero.y
//            val dlat =
//                (road.mNodes[1].mLocation.latitude - road.mNodes[0].mLocation.latitude).toFloat()
//
//            val dlong =
//                (road.mNodes[1].mLocation.longitude - road.mNodes[0].mLocation.longitude).toFloat()

                var ori =
                    abs(atan(dlat.toDouble() / dlong.toDouble()) * 360f / (2f * PI.toFloat())) % 90f
                ori = if (dlong > 0) {
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
                activity?.runOnUiThread {
                    map.mapOrientation = ori.toFloat()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tileStates: TileStates = map.overlayManager.tilesOverlay.tileStates
        if (tileStates.total == tileStates.upToDate)
        {
            //for tests
            map.contentDescription = "MAP LOADED"
        }

        if (myLocationOverlay.myLocation != null){
            isLocation = true
        }
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("TAG", "onSaveInstanceState called")
        if (myLocationOverlay.myLocation != null){
            outState.putFloat("LOC_LAT", myLocationOverlay.myLocation.latitude.toFloat())
            outState.putFloat("LOC_LONG", myLocationOverlay.myLocation.longitude.toFloat())
        }else {
            outState.putFloat("LOC_LAT", lastLocLat.toFloat())
            outState.putFloat("LOC_LONG", lastLocLong.toFloat())
        }
        outState.putFloat("CENTER_LAT", map.mapCenter.latitude.toFloat())
        outState.putFloat("CENTER_LONG", map.mapCenter.longitude.toFloat())
        outState.putFloat("ZOOM", map.zoomLevelDouble.toFloat())
        outState.putFloat("ORIENTATION", map.mapOrientation)
        outState.putBoolean("IS_LOC", isLocation)
        outState.putSerializable("STATE", state)
//        outState.putBoolean("FIRST", firstCreate)
        if (state == STATE.NAVI || state == STATE.ROUTE) {
            outState.putParcelable("ROAD", road)
        }
        if(state != STATE.INIT) {
            outState.putFloat("RAND_LAT", randomOverlay.getItem(0).point.latitude.toFloat())
            outState.putFloat("RAND_LONG", randomOverlay.getItem(0).point.longitude.toFloat())
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment StarterMapFragment.
         */
    }

    override fun onClick(p0: View?) {
        if (p0 == v.findViewById(R.id.btnRandom)){
            if (myLocationOverlay.myLocation != null) {
                state = STATE.POINT
                btnRoute.isVisible = true
                btnNavi.isVisible = false
                generateRandom(minRange, maxRange)
            } else {
                Toast.makeText(context, R.string.TOAST_LOC, Toast.LENGTH_SHORT).show()
            }
        }
        if (p0 == v.findViewById(R.id.btnRoute)){
            if (this::randomOverlay.isInitialized && isLocation) {
                drawRoute()
                btnRoute.isVisible = false
                btnNavi.isVisible = true
            }
            else if(!this::randomOverlay.isInitialized){
                Toast.makeText(context, "No destination selected!", Toast.LENGTH_SHORT).show()
            }else{
                //note: relies on isLocation being refreshed on drawing the circles
                Toast.makeText(context, R.string.TOAST_LOC, Toast.LENGTH_SHORT).show()
            }
        }
        if (p0 == v.findViewById(R.id.btnLocation)){
            map.controller.animateTo(myLocationOverlay.myLocation)
//            map.controller.setZoom(16.0)
        }
        if (p0 == v.findViewById(R.id.btnOrient)){
            map.mapOrientation = 0.0f
        }
        if (p0 == v.findViewById(R.id.btnNavi)){
            btnQuitNavi.isVisible = true
            btnLocation.isVisible = false
            btnNavi.isVisible = false
            btnOrient.isVisible = false
            btnRandom.isVisible = false
            insTextView.isVisible = true
            editMinRange.isVisible = false
            editMaxRange.isVisible = false
            navigate()
        }
        if (p0 == v.findViewById(R.id.btnQuitNavi)){
            state = STATE.ROUTE
            map.mapOrientation = 0.0f
            map.controller.setZoom(16.0)
            myLocationOverlay.disableFollowLocation()
            btnQuitNavi.isVisible = false
            btnLocation.isVisible = true
            btnNavi.isVisible = true
            btnOrient.isVisible = true
            btnRandom.isVisible = true
            insTextView.isVisible = false
            editMinRange.isVisible = true
            editMaxRange.isVisible = true
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            orientationListener.disable()
        }
        //TODO: btnBug
//        if (p0 == v.findViewById(R.id.btnBug)){
//            val emailIntent = Intent(Intent.ACTION_SEND)
//            emailIntent.type = "text/plain"
//            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("osmdroidbugs@gmail.com"))
//            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Open Map crash log")
//            emailIntent.putExtra(Intent.EXTRA_TEXT, "Log data")
//
//            val uri: Uri = Uri.fromFile(file)
//            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
//            startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"))
//        }
    }

    private fun drawRoute(){
        if (state != STATE.NAVI) {
            state = STATE.ROUTE
            Toast.makeText(context, "Calculating route...", Toast.LENGTH_SHORT).show()
        }

        if(this::roadOverlay.isInitialized){
            map.overlays.remove(roadOverlay)
        }

        if(myLocationOverlay.myLocation != null){
            lastLocLat = myLocationOverlay.myLocation.latitude
            lastLocLong = myLocationOverlay.myLocation.longitude
        }

        val thread = Thread {
            try {
                val waypoints = ArrayList<GeoPoint>()
                waypoints.add(GeoPoint(lastLocLat, lastLocLong))
                val endPoint = GeoPoint(randomOverlay.getItem(0).point.latitude, randomOverlay.getItem(0).point.longitude)
                waypoints.add(endPoint)
                road = roadManager.getRoad(waypoints)
                if(this::roadOverlay.isInitialized){
                    map.overlays.remove(roadOverlay)
                }
                roadOverlay = RoadManager.buildRoadOverlay(road)
                if (road.mStatus == Road.STATUS_OK){
                    map.overlays.add(roadOverlay)
                }else{
                    Log.d("TAG", "${road.mStatus}, ${Road.STATUS_INVALID}, ${Road.STATUS_TECHNICAL_ISSUE}")
                    activity?.runOnUiThread {
                        Toast.makeText(context, getString(R.string.ROUTING_FAILED), Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.d("TAG", e.toString())
                activity?.runOnUiThread {
                    Toast.makeText(context, getString(R.string.ROUTING_FAILED), Toast.LENGTH_SHORT).show()
                }
            }
        }
        thread.start()
        map.invalidate()
        if (state == STATE.NAVI && this::road.isInitialized){
            naviMapOrient()
        }
    }

    private fun drawCircleMin() {
        var loc = myLocationOverlay.myLocation
        if(loc == null){
            loc = GeoPoint(lastLocLat, lastLocLong)
        }else{
            lastLocLat = loc.latitude
            lastLocLong = loc.longitude
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
        //TODO: implement moveToDesiredPosition()
//        for (point in circle){
//            if (point.latitude > maxLat || point.latitude < -maxLat){
//                point.moveToDesiredPosition()
//            }
//        }
        pmin = Polygon(map)
        pmin.infoWindow = null
        pmin.setStrokeColor(Color.MAGENTA)
        pmin.points = circle
//        pmin.title = getString(R.string.MIN_RANGE)
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
        pmax.infoWindow = null
        pmax.setStrokeColor(Color.BLUE)
        pmax.points = circle
//        pmax.title = getString(R.string.MAX_RANGE)
        map.overlays.add(pmax)
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
            if(this::roadOverlay.isInitialized){
                map.overlays.remove(roadOverlay)
            }
            items.add(OverlayItem(getString(R.string.RAND_TITLE), getString(R.string.RAND_SNIP), moveByDistAngle(randDist, randAng, myLocationOverlay)))
            randomOverlay = ItemizedOverlayWithFocus(items, object: ItemizedIconOverlay.OnItemGestureListener<OverlayItem>{
                override fun onItemSingleTapUp(index:Int, item:OverlayItem):Boolean {
                    //do something
                    return true
                }
                override fun onItemLongPress(index:Int, item:OverlayItem):Boolean {
                    return false
                }
            }, context)
            randomOverlay.setFocusItemsOnTap(true)
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

    override fun onInit(p0: Int) {
        // TTS
        if (p0 == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            var lang = Locale.GERMAN
            //supported langs: ENG, FR, DE, CHN, JAP, KOR, IT
            if (Locale.getDefault() in Locale.getAvailableLocales()){
                lang = Locale.getDefault()
            }
            val result = tts.setLanguage(lang)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
//                if (firstCreate) {
//                    firstCreate = false
//                }
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }
}