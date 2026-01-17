package com.example.fitlifesmarthealthlifestyleapp.ui.activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LatLngPoint
import com.example.fitlifesmarthealthlifestyleapp.service.ActivityTrackingService
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class ActivityFragment : Fragment(), ActivityTrackingService.TrackingListener {

    private lateinit var viewModel: ActivityViewModel
    private lateinit var mapView: MapView
    private var currentMarker: Marker? = null

    private var routePolyline: Polyline? = null

    private lateinit var tvTime: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvPace: TextView
    private lateinit var tvAvgSpeed: TextView
    private lateinit var tvCalories: TextView
    private lateinit var btnStartActivity: Button
    private lateinit var btnComplete: Button

    private var trackingService: ActivityTrackingService? = null
    private var isServiceBound = false

    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0

    // realtime location (for marker follow even when not tracking)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isLocationUpdatesActive = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ActivityTrackingService.LocalBinder
            trackingService = binder.getService()
            trackingService?.setListener(this@ActivityFragment)
            isServiceBound = true
            Log.d(TAG, "Service connected")

            // đảm bảo startTracking gọi sau khi bound
            trackingService?.startTracking()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                startRealtimeLocationUpdates()
                Toast.makeText(requireContext(), "Location tracking enabled", Toast.LENGTH_SHORT).show()
            }
            else -> Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationCallback()

        viewModel = ViewModelProvider(this)[ActivityViewModel::class.java]
        initViews(view)
        setupMap()
        setupObservers()
        setupListeners()
        checkLocationPermission()
    }

    private fun initViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        tvTime = view.findViewById(R.id.tvTime)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvPace = view.findViewById(R.id.tvPace)
        tvAvgSpeed = view.findViewById(R.id.tvAvgSpeed)
        tvCalories = view.findViewById(R.id.tvCalories)
        btnStartActivity = view.findViewById(R.id.btnStartActivity)
        btnComplete = view.findViewById(R.id.btnComplete)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)

        val defaultLocation = GeoPoint(21.0285, 105.8542)
        mapView.controller.setCenter(defaultLocation)

        routePolyline = Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#FF6B35")
            outlinePaint.strokeWidth = 10f
            outlinePaint.isAntiAlias = true
        }
        mapView.overlays.add(routePolyline)
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { updateMapLocation(it) }
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> startRealtimeLocationUpdates()
            else -> locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun startRealtimeLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        if (isLocationUpdatesActive) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdateDelayMillis(2000L)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        isLocationUpdatesActive = true

        fusedLocationClient.lastLocation.addOnSuccessListener { it?.let(::updateMapLocation) }
    }

    private fun stopRealtimeLocationUpdates() {
        if (!isLocationUpdatesActive) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
    }

    private fun updateMapLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        if (currentMarker == null) {
            currentMarker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "You are here"
            }
            mapView.overlays.add(currentMarker)
            mapView.controller.animateTo(geoPoint)
        } else {
            currentMarker?.position = geoPoint
            mapView.controller.animateTo(geoPoint)
        }
        mapView.invalidate()
    }

    private fun isGPSEnabled(): Boolean {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showEnableGPSDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("GPS Required")
            .setMessage("Please enable GPS/Location services to track your activity")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupObservers() {
        viewModel.distance.observe(viewLifecycleOwner) { tvDistance.text = String.format("%.2f km", it) }
        viewModel.speed.observe(viewLifecycleOwner) { tvAvgSpeed.text = String.format("%.1f km/h", it) }
        viewModel.calories.observe(viewLifecycleOwner) { tvCalories.text = "$it kcal" }

        viewModel.toastMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        viewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
            btnStartActivity.visibility = if (isTracking) View.GONE else View.VISIBLE
            btnComplete.visibility = if (isTracking) View.VISIBLE else View.GONE
        }

        // NEW: vẽ route
        viewModel.routePoints.observe(viewLifecycleOwner) { drawRoute(it) }
    }

    private fun setupListeners() {
        btnStartActivity.setOnClickListener {
            if (!isGPSEnabled()) {
                showEnableGPSDialog()
                return@setOnClickListener
            }
            startTracking()
        }

        btnComplete.setOnClickListener { completeTracking() }
    }

    private fun startTracking() {
        viewModel.startTracking()
        drawRoute(emptyList())

        val intent = Intent(requireContext(), ActivityTrackingService::class.java)
        requireContext().startService(intent)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        seconds = 0
        handler.post(timeRunnable)
        Toast.makeText(requireContext(), "Activity tracking started!", Toast.LENGTH_SHORT).show()
    }

    private fun completeTracking() {
        handler.removeCallbacks(timeRunnable)

        trackingService?.stopTracking()
        viewModel.stopTracking()
        viewModel.completeActivity()

        if (isServiceBound) {
            requireContext().unbindService(serviceConnection)
            isServiceBound = false
        }

        seconds = 0
        updateTimeUI()
    }

    private fun drawRoute(points: List<LatLngPoint>) {
        val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
        routePolyline?.setPoints(geoPoints)
        mapView.invalidate()

        if (geoPoints.size >= 2) {
            val bb = BoundingBox.fromGeoPointsSafe(geoPoints)
            if (bb != null) mapView.zoomToBoundingBox(bb, true, 120)
        }
    }

    private val timeRunnable = object : Runnable {
        override fun run() {
            seconds++
            updateTimeUI()
            viewModel.updateDuration(seconds)
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateTimeUI() {
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        tvTime.text = String.format("%02d:%02d", minutes, secs)

        val distance = viewModel.distance.value ?: 0.0
        tvPace.text = if (distance > 0 && seconds > 0) {
            val paceInMinutes = (seconds / 60.0) / distance
            val paceMin = paceInMinutes.toInt()
            val paceSec = ((paceInMinutes - paceMin) * 60).toInt()
            String.format("%d:%02d", paceMin, paceSec)
        } else "0:00"
    }

    // Service callback
    override fun onLocationUpdate(location: Location, distanceKm: Double, speedKmh: Double, routePoints: List<LatLngPoint>) {
        viewModel.updateFromService(location, distanceKm, speedKmh, routePoints)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) startRealtimeLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (viewModel.isTracking.value != true) stopRealtimeLocationUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable)
        stopRealtimeLocationUpdates()
        if (isServiceBound) requireContext().unbindService(serviceConnection)
        mapView.onDetach()
    }

    companion object { private const val TAG = "ActivityFragment" }
}