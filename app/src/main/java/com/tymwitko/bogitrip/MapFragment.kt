package com.tymwitko.bogitrip

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.*
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.tymwitko.bogitrip.databinding.FragmentStarterMapBinding
import com.tymwitko.bogitrip.viewmodels.MapViewModel
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.api.IMapView
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.TileStates
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
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
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

//enum class STATE{
//    INIT, POINT, ROUTE, NAVI
//}

class MapFragment : Fragment() {
    private lateinit var map : MapView
    private var minRange: Double? = null
    private var maxRange: Double? = null
//    private var state = STATE.INIT
    private lateinit var binding: FragmentStarterMapBinding

    private val viewModel by viewModel<MapViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = context?.packageName
        //important! set your user agent to prevent getting banned from the osm servers
    }

    private fun onBugReport(){
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(resources.getString(R.string.bug_url)))
        startActivity(browserIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentStarterMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenuBar()
        setupListeners()
        setupInitialButtonVisibilityAndColors()
        setupMap()

        if (savedInstanceState != null){
            map.controller.setZoom(savedInstanceState.getFloat("ZOOM").toDouble())
            map.controller.setCenter(GeoPoint(savedInstanceState.getFloat("CENTER_LAT").toDouble(), savedInstanceState.getFloat("CENTER_LONG").toDouble()))
            map.mapOrientation = savedInstanceState.getFloat("ORIENTATION")

            if (viewModel.isRoadPresentAndOk()) {
                map.overlays.add(viewModel.getRouteOverlay())
                binding.btnRoute.isVisible = false
                if (viewModel.isRoadPresentAndOk()) {
                    binding.btnNavi.isVisible = true
                } else{
                    enterNaviMode()
                }
            }
        }

        if (viewModel.isInNaviMode()) {
            enterNaviMode()
        } else {
            Log.d("TAG", "Restoring elements: ${viewModel.getCircle(MaxMin.MAX)}," +
                    "${viewModel.getCircle(MaxMin.MIN)}, ${viewModel.getRandomOverlay()}")
            viewModel.getCircle(MaxMin.MAX)?.let {
                map.overlays.add(it)
            }
            viewModel.getCircle(MaxMin.MIN)?.let {
                map.overlays.add(it)
            }
            map.invalidate()
        }
        viewModel.getRouteOverlay()?.let {
            map.overlays.add(it)
        }
        viewModel.getRandomOverlay()?.let {
            map.overlays.add(it)
        }

        val tileStates: TileStates = map.overlayManager.tilesOverlay.tileStates
        if (tileStates.total == tileStates.upToDate) {
            //for tests
            map.contentDescription = "MAP LOADED"
        }
        map.invalidate()
        map.setDestroyMode(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("TAG", "onSaveInstanceState called")
        outState.putFloat("CENTER_LAT", map.mapCenter.latitude.toFloat())
        outState.putFloat("CENTER_LONG", map.mapCenter.longitude.toFloat())
        outState.putFloat("ZOOM", map.zoomLevelDouble.toFloat())
        outState.putFloat("ORIENTATION", map.mapOrientation)
    }

    private fun setupMenuBar() {
        val menuHost: MenuHost = activity as MenuHost

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.map_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.shareMenuButton -> {
                        onBugReport()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupListeners() {
        binding.apply {
            btnRandom.setOnClickListener {
                if (viewModel.getLocationFromOverlay() != null) {
                    binding.btnRoute.isVisible = true
                    binding.btnNavi.isVisible = false
                    minRange?.let { it1 -> maxRange?.let { it2 -> drawRandomMarker(it1, it2) } }
                } else {
                    Toast.makeText(
                        context,
                        R.string.TOAST_LOC,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            btnRoute.setOnClickListener {
                drawRoute()
            }

            btnLocation.setOnClickListener {
                map.controller.animateTo(viewModel.getLocationFromOverlay())
            }

            btnOrient.setOnClickListener {
                map.mapOrientation = 0.0f
            }

            btnNavi.setOnClickListener {
                enterNaviMode()
            }

            btnQuitNavi.setOnClickListener {
                map.mapOrientation = 0.0f
                map.controller.setZoom(16.0)
                viewModel.myLocationOverlay?.disableFollowLocation()
                binding.apply {
                    btnQuitNavi.isVisible = false
                    btnRefresh.isVisible = false
                    btnLocation.isVisible = true
                    btnNavi.isVisible = true
                    btnOrient.isVisible = true
                    btnRandom.isVisible = true
                    insTextView.isVisible = false
                    editRangeMin.isVisible = true
                    editRangeMax.isVisible = true
                }
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            btnRefresh.setOnClickListener {
                viewModel.setNaviStep(0)
                drawRoute()
                //TODO: cached instructions from previous route appear after refreshing
                enterNaviMode()
            }

            editRangeMin.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    minRange = if (s.isNotEmpty()) {
                        try {
                            s.toString().toDouble()
                        } catch (e: java.lang.NumberFormatException) {
                            0.0
                        }
                    } else {
                        0.0
                    }
                    try {
                        drawCircles()
                    } catch (e: java.lang.IllegalArgumentException) {
                        Toast.makeText(context, R.string.TOAST_EDGE, Toast.LENGTH_SHORT).show()
                    }

                }

                override fun afterTextChanged(s: Editable) {
                }
            })

            editRangeMax.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    maxRange = if (s.isNotEmpty()) {
                        try {
                            s.toString().toDouble()
                        } catch (e: java.lang.NumberFormatException) {
                            0.0
                        }
                    } else {
                        0.0
                    }
                    try {
                        drawCircles()
                    } catch (e: java.lang.IllegalArgumentException) {
                        Toast.makeText(context, R.string.TOAST_EDGE, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun afterTextChanged(s: Editable) {
                }
            })
        }


        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Called when a new location is found by the network location provider.
                Log.d("TAG", "Location: ${location.latitude}")
                viewModel.updateLocation(location)
                if(viewModel.isInNaviMode()){
                    if (viewModel.checkIfPointPassed()) {
                        viewModel.navigateAtCurrentStep()
                            ?.let { showInstructionsAndSpeak(it) }
                        drawRoute()
                    }
                }
            }

            override fun onProviderEnabled(provider: String) {
                Log.d("TAG", "Provider enabled $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.d("TAG", "Provider disabled $provider")
            }
        }

        // Register the listener with the Location Manager to receive location updates
        if (context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("TAG", "Location access granted")
//            locationManager.requestLocationUpdates(
//                LocationManager.NETWORK_PROVIDER,
//                1,
//                1f,
//                locationListener
//            )
            context?.let {
                (it.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                    .requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1,
                        1f,
                        locationListener
                    )
            }
        }

    }

    private fun setupInitialButtonVisibilityAndColors() {
        binding.apply {
            btnNavi.isVisible = false
            btnQuitNavi.isVisible = false
            btnRefresh.isVisible = false
            insTextView.isVisible = false

            editRangeMin.backgroundTintList = ColorStateList.valueOf(Color.MAGENTA)
            editRangeMax.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
            editRangeMin.setTextColor(ColorStateList.valueOf(Color.MAGENTA))
            editRangeMax.setTextColor(ColorStateList.valueOf(Color.BLUE))
            editRangeMin.setHintTextColor(ColorStateList.valueOf(Color.MAGENTA))
            editRangeMax.setHintTextColor(ColorStateList.valueOf(Color.BLUE))
        }
    }

    private fun setupMap() {
        map = binding.mapview
        map.apply {
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent): Boolean {
                        Log.i(
                            IMapView.LOGTAG,
                            System.currentTimeMillis()
                                .toString() + " onScroll " + event.x + "," + event.y
                        )
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

                setTileSource(TileSourceFactory.MAPNIK)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                setUseDataConnection(true)
                controller.setZoom(10.0)
                controller.setCenter(GeoPoint(0.0, 0.0))

                setScrollableAreaLimitLatitude(MAXIMUM_LATITUDE, -MAXIMUM_LATITUDE, 0)
                minZoomLevel = 2.0

                setMultiTouchControls(true)
                overlays.add(RotationGestureOverlay(map)
                    .apply {
                        isEnabled
                    })

                overlays.add(CopyrightOverlay(context)
                    .apply {
                        setAlignBottom(true)
                        setAlignRight(true)
                    })

//        compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), map)
//        compassOverlay.enableCompass()
//        map.overlays.add(compassOverlay)

                val dm: DisplayMetrics? = context?.resources?.displayMetrics
                overlays.add(ScaleBarOverlay(map)
                    .apply {
                        setCentred(true)
                        //play around with these values to get the location on screen in the right place for your application
                        setScaleBarOffset(dm!!.widthPixels / 2, 10)
                    })

                val provider = GpsMyLocationProvider(context)
                    .apply {
                        addLocationSource(LocationManager.GPS_PROVIDER)
                        addLocationSource(LocationManager.NETWORK_PROVIDER)
                        locationUpdateMinDistance = 1f
                        locationUpdateMinTime = 10
                    }

                viewModel.setupMyLocationOverlay(MyLocationNewOverlay(provider, map))

                val icon: Bitmap = BitmapFactory.decodeResource(
                    resources,
                    org.osmdroid.library.R.drawable.person
                )
                viewModel.setLocationIcon(icon)
                overlays.add(viewModel.myLocationOverlay)
                invalidate()

                if (viewModel.getLocationFromOverlay() == null) {
                    // could be too early
                    Log.d("TAG", "Location not retrieved")
                }
                controller.setZoom(6.0)
            }
    }

    private fun forceLocationUpdate() {
        viewModel.getLocationFromOverlay()?.let { viewModel.updateLocation(it) }
    }

    @SuppressLint("CheckResult")
    private fun drawRoute() {
        if (!viewModel.isInNaviMode()) {
            Toast.makeText(context, R.string.calculating, Toast.LENGTH_SHORT).show()
        }

        viewModel.getRouteOverlay()?.let{
            map.overlays.remove(it)
        }
        map.overlays
            .filterIsInstance<Polyline>()
            .forEach { map.overlays.remove(it) }

        forceLocationUpdate()

        viewModel.drawRouteTo(
            viewModel.getRandomOverlay()?.position?.latitude,
            viewModel.getRandomOverlay()?.position?.longitude,
            OSRMRoadManager(context, Configuration.getInstance().userAgentValue)
        )
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(Schedulers.computation())
            ?.doOnError { Log.d("TAG", it.toString())
                Toast.makeText(context, getString(R.string.ROUTING_FAILED), Toast.LENGTH_SHORT).show() }
            ?.doAfterSuccess {
                activity?.runOnUiThread {
                    map.invalidate()
                    viewModel.getRouteOverlay()?.let {
//                        if (state == STATE.POINT) {
//                            viewModel.getRouteOverlay()?.let {
//                                map.overlays.remove(it)
//                            }
//                        }
                        if (!viewModel.isInNaviMode()) {
                            binding.btnRoute.isVisible = false
                            binding.btnNavi.isVisible = true
                        }
                    } ?: run {
                        Toast.makeText(context, R.string.no_destination, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            ?.subscribe { road ->
                map.overlays.add(viewModel.getRouteOverlayFromRoad(road))
                if (viewModel.isRoadPresentAndOk()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        map.overlays.add(viewModel.getRouteOverlay())
                        binding.btnRoute.isVisible = false
                        binding.btnNavi.isVisible = true
                    }
                } else {
                    Log.d(
                        "TAG",
                        "${viewModel.getRoadStatus()}, ${Road.STATUS_INVALID}, ${Road.STATUS_TECHNICAL_ISSUE}"
                    )
                }
            }
        map.invalidate()
        viewModel.setNaviStep(1)
        if (viewModel.isInNaviMode() && viewModel.isRoadPresentAndOk()){
            viewModel.naviMapOrient(viewModel.getNaviStep(), map)
        }
    }

    private fun drawCircles() {
        var loc = viewModel.getLocationFromOverlay() ?: viewModel.getLastLocation()
        loc?.let {
            viewModel.updateLocation(it)
        } ?: run {
            loc = viewModel.getLastLocation()
        }

        loc?.let {
            map.overlays
                .filterIsInstance<Polygon>()
                .forEach { circle -> map.overlays.remove(circle) }
            map.overlays.add(
                viewModel.drawCircle(
                    MaxMin.MIN, Color.MAGENTA,
                    it, minRange?.times(1000) ?: 0.0, map
                )
            )
            map.overlays.add(
                viewModel.drawCircle(
                    MaxMin.MAX, Color.BLUE,
                    it, maxRange?.times(1000) ?: 0.0, map
                )
            )
            map.invalidate()
        } ?: run { Toast.makeText(context,
            R.string.TOAST_LOC,
            Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRandomMarker(minRangeArg: Double, maxRangeArg: Double) {
        viewModel.getRouteOverlay()?.let {
            map.overlays.remove(it)
        }
        val minR = minRangeArg * 1000
        val maxR = maxRangeArg * 1000
        if (maxR >= minR && maxR > 0 && minR >= 0) {
            val randAng = Random.nextFloat() * 360f
            val randDist = Random.nextFloat() * (maxR - minR) + minR
            if (viewModel.getRandomOverlay() in map.overlays) {
                map.overlays.remove(viewModel.getRandomOverlay())
            }
            viewModel.getRouteOverlay()?.let {
                map.overlays.remove(it)
            }

            val randomOverlay = Marker(map)
                .apply {
                position =
                    viewModel.moveByDistAngle(randDist, randAng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            viewModel.setNewRandomPoint(randomOverlay)
            map.overlays.add(randomOverlay)
            map.invalidate()
        }
        else{
            Toast.makeText(context, R.string.TOAST_INV, Toast.LENGTH_SHORT).show()
        }
    }
    private fun enterNaviMode() {
        binding.apply {
            btnNavi.isVisible = false
            btnOrient.isVisible = false
            btnQuitNavi.isVisible = true
            btnRefresh.isVisible = true
            btnLocation.isVisible = false
            btnRandom.isVisible = false
            editRangeMax.isVisible = false
            editRangeMin.isVisible = false
            insTextView.isVisible = true
        }
        if (viewModel.isRoadPresentAndOk()) {
            viewModel.setNaviMode(true)
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            map.controller.setZoom(18.0)
            viewModel.switchLocationOverlayToNavi()
            viewModel.navigateAtCurrentStep()
                ?.let { it1 ->
                    showInstructionsAndSpeak(it1) }
        }
    }

    private fun showInstructionsAndSpeak(instructions: String) {
        binding.insTextView.text = instructions
        viewModel.speak(instructions)
    }
}