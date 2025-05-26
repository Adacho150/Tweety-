package com.example.tweety2
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import java.io.IOException

fun fetchBusStops(context: Context, mapView: MapView) {
    val url = "http://www.poznan.pl/mim/plan/map_service.html?mtype=pub_transport&co=cluster"

    val request = Request.Builder().url(url).build()
    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("Map", "Failed to fetch bus stops", e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { json ->
                val gson = Gson()
                val featureCollection = gson.fromJson(json, FeatureCollection::class.java)
                val overlay = FolderOverlay()

                featureCollection.features.forEach { feature ->
                    val coordinates = feature.geometry.coordinates
                    val point = GeoPoint(coordinates[1], coordinates[0])
                    val marker = Marker(mapView).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = feature.properties["name"] ?: "Przystanek"
                    }
                    overlay.add(marker)
                }

                mapView.overlays.add(overlay)
                mapView.postInvalidate()
            }
        }
    })
}