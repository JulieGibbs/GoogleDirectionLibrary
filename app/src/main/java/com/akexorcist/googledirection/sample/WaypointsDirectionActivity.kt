package com.akexorcist.googledirection.sample

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.config.GoogleDirectionConfiguration
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.akexorcist.googledirection.model.Route
import com.akexorcist.googledirection.util.DirectionConverter
import com.akexorcist.googledirection.util.execute
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_waypoints_direction.*

class WaypointsDirectionActivity : AppCompatActivity() {
    companion object {
        private const val serverKey = "YOUR_SERVER_KEY"
        private val park = LatLng(41.8838111, -87.6657851)
        private val shopping = LatLng(41.8766061, -87.6556908)
        private val dinner = LatLng(41.8909056, -87.6467561)
        private val gallery = LatLng(41.9007082, -87.6488802)
    }

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waypoints_direction)

        buttonRequestDirection.setOnClickListener { requestDirection() }

        (supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment).getMapAsync { googleMap ->
            this.googleMap = googleMap
        }
    }

    private fun requestDirection() {
        showSnackbar(getString(R.string.direction_requesting))
        GoogleDirectionConfiguration.getInstance().isLogEnabled = true
        GoogleDirection.withServerKey(serverKey)
            .from(park)
            .and(shopping)
            .and(dinner)
            .to(gallery)
            .transportMode(TransportMode.DRIVING)
            .execute(
                onDirectionSuccess = { direction, _ -> onDirectionSuccess(direction) },
                onDirectionFailure = { t -> onDirectionFailure(t) }
            )
    }

    private fun onDirectionSuccess(direction: Direction) {
        showSnackbar(getString(R.string.success_with_status, direction.status))
        if (direction.isOK) {
            val route = direction.routeList[0]
            val legCount = route.legList.size
            for (index in 0 until legCount) {
                val leg = route.legList[index]
                googleMap?.addMarker(MarkerOptions().position(leg.startLocation.coordination))
                if (index == legCount - 1) {
                    googleMap?.addMarker(MarkerOptions().position(leg.endLocation.coordination))
                }
                val stepList = leg.stepList
                val polylineOptionList = DirectionConverter.createTransitPolyline(this, stepList, 5, Color.RED, 3, Color.BLUE)
                for (polylineOption in polylineOptionList) {
                    googleMap?.addPolyline(polylineOption)
                }
            }
            setCameraWithCoordinationBounds(route)
            buttonRequestDirection.visibility = View.GONE
        } else {
            showSnackbar(direction.status)
        }
    }

    private fun onDirectionFailure(t: Throwable) {
        showSnackbar(t.message)
    }

    private fun setCameraWithCoordinationBounds(route: Route) {
        val southwest = route.bound.southwestCoordination.coordination
        val northeast = route.bound.northeastCoordination.coordination
        val bounds = LatLngBounds(southwest, northeast)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun showSnackbar(message: String?) {
        message?.let {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
        }
    }
}
