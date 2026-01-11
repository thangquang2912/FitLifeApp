package com.example.fitlifesmarthealthlifestyleapp.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.service.ActivityTrackingService
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ActivityFragment : Fragment(), ActivityTrackingService.TrackingListener {

    private lateinit var viewModel:  ActivityViewModel
    private lateinit var mapView: MapView
    private var currentMarker: Marker? = null

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

    // ✨ Real-time location tracking
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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                Log.d(TAG, "Location permission granted")
                startRealtimeLocationUpdates()
                Toast.makeText(requireContext(), "Location tracking enabled ✅", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission required ❌", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize OSM configuration
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
        mapView.controller.setZoom(17.0) // Zoom closer for better tracking

        // Set default location (Vietnam - Hanoi)
        val defaultLocation = GeoPoint(21.0285, 105.8542)
        mapView.controller.setCenter(defaultLocation)

        Log.d(TAG, "Map initialized")
    }

    // ✨ Setup location callback for real-time updates
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "📍 Real-time location update: ${location.latitude}, ${location.longitude}")
                    updateMapLocation(location)
                }
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Location permission already granted")
                startRealtimeLocationUpdates() // ✨ Start tracking immediately
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // ✨ Start real-time location updates
    private fun startRealtimeLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        if (isLocationUpdatesActive) {
            Log.d(TAG, "Location updates already active")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L // Update every 2 seconds
        ).apply {
            setMinUpdateIntervalMillis(1000L) // Min 1 second
            setMaxUpdateDelayMillis(2000L)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        isLocationUpdatesActive = true
        Log.d(TAG, " Real-time location updates started")

        // Also get last known location immediately
        getLastKnownLocation()
    }

    // ✨ Stop real-time location updates
    private fun stopRealtimeLocationUpdates() {
        if (!isLocationUpdatesActive) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
        Log.d(TAG, "️Real-time location updates stopped")
    }

    // ✨ Get last known location for immediate display
    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location:  Location? ->
            if (location != null) {
                Log.d(TAG, "📍 Last known location: ${location.latitude}, ${location.longitude}")
                updateMapLocation(location)
                Toast.makeText(requireContext(), "Location found! 📍", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "Last known location is null, waiting for real-time updates...")
            }
        }
    }

    // ✨ Update map with new location
    private fun updateMapLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        // Update or create marker
        if (currentMarker == null) {
            currentMarker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)


                title = "You are here"
            }
            mapView.overlays.add(currentMarker)

            // First time: zoom and center
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(geoPoint)

            Log.d(TAG, "✅ Marker created and camera moved")
        } else {
            // Update existing marker
            currentMarker?. position = geoPoint

            // Auto-follow:  always center camera on user location
            mapView. controller.animateTo(geoPoint)
        }

        mapView.invalidate()
    }

    // ✨ Check if GPS is enabled
    private fun isGPSEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // ✨ Show dialog to enable GPS
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
        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            // This is called during active tracking (service)
            updateMarker(location)
        }

        viewModel.distance.observe(viewLifecycleOwner) { distance ->
            tvDistance.text = String.format("%.2f km", distance)
        }

        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            tvAvgSpeed.text = String.format("%.1f km/h", speed)
        }

        viewModel.calories.observe(viewLifecycleOwner) { calories ->
            tvCalories.text = "$calories kcal"
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
            if (isTracking) {
                btnStartActivity.visibility = View.GONE
                btnComplete.visibility = View.VISIBLE
            } else {
                btnStartActivity.visibility = View.VISIBLE
                btnComplete.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        btnStartActivity.setOnClickListener {
            if (! isGPSEnabled()) {
                showEnableGPSDialog()
                return@setOnClickListener
            }

            startTracking()
        }

        btnComplete.setOnClickListener {
            completeTracking()
        }
    }

    private fun startTracking() {
        viewModel.startTracking()

        val intent = Intent(requireContext(), ActivityTrackingService::class.java)
        requireContext().startService(intent)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        trackingService?.startTracking()

        seconds = 0
        handler.post(timeRunnable)

        Log.d(TAG, "🏃 Activity tracking started")
        Toast.makeText(requireContext(), "Activity tracking started!  🏃", Toast.LENGTH_SHORT).show()
    }

    private fun completeTracking() {
        handler.removeCallbacks(timeRunnable)

        val finalDistance = trackingService?.stopTracking() ?: 0.0
        viewModel.stopTracking()
        viewModel.completeActivity()

        if (isServiceBound) {
            requireContext().unbindService(serviceConnection)
            isServiceBound = false
        }

        // Reset UI
        seconds = 0
        updateTimeUI()

        Log.d(TAG, "✅ Activity tracking completed.  Distance: $finalDistance km")
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

        // Calculate pace
        val distance = viewModel.distance.value ?: 0.0
        val pace = if (distance > 0 && seconds > 0) {
            val paceInMinutes = (seconds / 60.0) / distance
            val paceMin = paceInMinutes.toInt()
            val paceSec = ((paceInMinutes - paceMin) * 60).toInt()
            String.format("%d:%02d", paceMin, paceSec)
        } else {
            "0:00"
        }
        tvPace.text = pace
    }

    private fun updateMarker(location: Location) {
        // This is called during active tracking (from service)
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        currentMarker?.position = geoPoint
        mapView.controller.animateTo(geoPoint)
        mapView.invalidate()

        Log.d(TAG, "📍 Marker updated during tracking")
    }

    // TrackingListener implementation
    override fun onLocationUpdate(location: Location, distanceKm: Double, speedKmh: Double) {
        viewModel.updateLocation(location, distanceKm, speedKmh)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        // ✨ Resume location updates when returning to fragment
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRealtimeLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()

        if (!viewModel.isTracking.value!!) {
            stopRealtimeLocationUpdates()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable)

        stopRealtimeLocationUpdates()

        if (isServiceBound) {
            requireContext().unbindService(serviceConnection)
        }
        mapView.onDetach()
    }

    companion object {
        private const val TAG = "ActivityFragment"
    }
}