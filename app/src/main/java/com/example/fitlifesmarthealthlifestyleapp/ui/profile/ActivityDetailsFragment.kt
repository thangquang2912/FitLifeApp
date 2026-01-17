package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.ActivityLog
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class ActivityDetailsFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var tvStats: TextView
    private lateinit var viewModel: ActivityDetailsViewModel

    private var polyline: Polyline? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        return inflater.inflate(R.layout.fragment_activity_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapViewDetails)
        tvStats = view.findViewById(R.id.tvStats)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        polyline = Polyline(mapView).apply {
            outlinePaint.color = Color.parseColor("#FF6B35")
            outlinePaint.strokeWidth = 10f
            outlinePaint.isAntiAlias = true
        }
        mapView.overlays.add(polyline)

        viewModel = ViewModelProvider(this)[ActivityDetailsViewModel::class.java]

        val activityId = requireArguments().getString("activityId") ?: run {
            Toast.makeText(requireContext(), "Missing activityId", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.load(activityId)

        viewModel.activity.observe(viewLifecycleOwner) { log ->
            if (log != null) render(log)
        }

        viewModel.toast.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    private fun render(log: ActivityLog) {
        tvStats.text =
            "Type: ${log.activityType}\n" +
                    "Distance: ${String.format("%.2f", log.distanceKm)} km\n" +
                    "Duration: ${formatDuration(log.durationSeconds)}\n" +
                    "Avg speed: ${String.format("%.1f", log.avgSpeedKmh)} km/h\n" +
                    "Pace: ${log.paceMinPerKm} /km\n" +
                    "Calories: ${log.caloriesBurned} kcal"

        val geoPoints = log.routePoints.map { GeoPoint(it.lat, it.lng) }
        polyline?.setPoints(geoPoints)
        mapView.invalidate()

        if (geoPoints.size >= 2) {
            val bb = BoundingBox.fromGeoPointsSafe(geoPoints)
            if (bb != null) mapView.zoomToBoundingBox(bb, true, 120)
        } else if (geoPoints.size == 1) {
            mapView.controller.setZoom(17.0)
            mapView.controller.setCenter(geoPoints.first())
        }
    }

    private fun formatDuration(sec: Int): String {
        val m = (sec % 3600) / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }
}