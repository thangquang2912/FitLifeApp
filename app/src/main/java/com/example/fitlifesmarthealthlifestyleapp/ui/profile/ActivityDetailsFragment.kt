package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LatLngPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max

class ActivityDetailsFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView

    private lateinit var tvDistanceValue: TextView
    private lateinit var tvPaceValue: TextView
    private lateinit var tvCaloriesValue: TextView
    private lateinit var tvDurationValue: TextView

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

        btnBack = view.findViewById(R.id.btnBack)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)

        tvDistanceValue = view.findViewById(R.id.tvDistanceValue)
        tvPaceValue = view.findViewById(R.id.tvPaceValue)
        tvCaloriesValue = view.findViewById(R.id.tvCaloriesValue)
        tvDurationValue = view.findViewById(R.id.tvDurationValue)

        mapView = view.findViewById(R.id.mapViewDetails)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        polyline = Polyline(mapView).apply {
            outlinePaint.color = Color.parseColor("#FF6B35")
            outlinePaint.strokeWidth = 10f
            outlinePaint.isAntiAlias = true
        }
        mapView.overlays.add(polyline)

        btnBack.setOnClickListener {
            findNavController().popBackStack(R.id.workoutHistoryFragment, false)
        }

        viewModel = ViewModelProvider(this)[ActivityDetailsViewModel::class.java]

        val activityId = requireArguments().getString("activityId") ?: run {
            Toast.makeText(requireContext(), "Missing activityId", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ActivityDetailsState.Loading -> {
                    tvTitle.text = "Activity Details"
                    tvSubtitle.text = "Loading..."
                }
                is ActivityDetailsState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                is ActivityDetailsState.Success -> render(state.ui)
            }
        }

        viewModel.load(activityId)
    }

    private fun render(ui: ActivityDetailsUi) {
        tvTitle.text = ui.title
        tvSubtitle.text = ui.subtitle

        tvDistanceValue.text = ui.distanceText
        tvPaceValue.text = ui.paceText
        tvCaloriesValue.text = ui.caloriesText
        tvDurationValue.text = ui.durationText

        val clean = sanitizeRoute(ui.raw.routePoints)
        val geoPoints = clean.map { GeoPoint(it.lat, it.lng) }

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

    private fun sanitizeRoute(points: List<LatLngPoint>): List<LatLngPoint> {
        if (points.size <= 2) return points

        val sorted = points.sortedBy { it.timeMs }
        val out = ArrayList<LatLngPoint>(sorted.size)
        var last: LatLngPoint? = null

        for (p in sorted) {
            if (p.accuracyMeters > 25f) continue

            val l = last
            if (l == null) {
                out.add(p); last = p; continue
            }

            val d = distanceMeters(l, p)
            val dtSec = max(1.0, (p.timeMs - l.timeMs) / 1000.0)
            val v = d / dtSec

            if (d > 120.0 && v > 12.0) continue
            if (d < 2.0) continue

            out.add(p)
            last = p
        }
        return out
    }

    private fun distanceMeters(a: LatLngPoint, b: LatLngPoint): Double {
        val res = FloatArray(1)
        android.location.Location.distanceBetween(a.lat, a.lng, b.lat, b.lng, res)
        return res[0].toDouble()
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